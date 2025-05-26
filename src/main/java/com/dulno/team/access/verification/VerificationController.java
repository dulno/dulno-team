package com.dulno.team.access.verification;

import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.hashing.Hashing;
import com.dulno.core.session.SessionStatus;
import com.dulno.core.session.UserAgent;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.Staff;
import com.dulno.team.staff.StaffDatabaseTable;
import com.dulno.team.staff.StaffVerificationDatabaseTable;
import com.dulno.team.staff.mfa.StaffMultiFactorAuthFactory;
import com.dulno.team.staff.session.StaffSession;
import com.dulno.team.staff.session.StaffSessionDatabaseTable;
import com.maxmind.geoip2.DatabaseReader;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.security.Key;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class VerificationController extends TeamController {
  private final Key panelKey;
  private final Key refreshKey;
  private final StaffDatabaseTable staffDatabaseTable;
  private final StaffVerificationDatabaseTable staffVerificationDatabaseTable;
  private final StaffMultiFactorAuthFactory multiFactorAuthFactory;
  private final StaffSessionDatabaseTable sessionDatabaseTable;
  private final DatabaseReader geoDatabaseReader;
  private final Hashing hashing;

  private VerificationController(
    @Qualifier("panelKey") Key panelKey,
    @Qualifier("panelRefreshKey") Key refreshKey,
    StaffDatabaseTable staffDatabaseTable, GroupDatabaseTable groupDatabaseTable,
    StaffVerificationDatabaseTable staffVerificationDatabaseTable,
    StaffMultiFactorAuthFactory multiFactorAuthFactory,
    StaffSessionDatabaseTable sessionDatabaseTable, DatabaseReader geoDatabaseReader,
    Hashing hashing
  ) {
    super(panelKey, staffDatabaseTable, groupDatabaseTable);
    this.panelKey = panelKey;
    this.refreshKey = refreshKey;
    this.staffDatabaseTable = staffDatabaseTable;
    this.staffVerificationDatabaseTable = staffVerificationDatabaseTable;
    this.multiFactorAuthFactory = multiFactorAuthFactory;
    this.sessionDatabaseTable = sessionDatabaseTable;
    this.geoDatabaseReader = geoDatabaseReader;
    this.hashing = hashing;
  }

  @RequestMapping(path = "/verification/complete/", method = RequestMethod.POST)
  public void completeVerification(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var staffId = body.getUUID("staff");
    staffVerificationDatabaseTable.verificationExists(staffId)
      .thenAccept(exists -> completeVerification(staffId,
        body.getString("password"), body.getString("token"), exists));
  }

  private void completeVerification(
    UUID staffId, String password, String submittedToken, boolean tokenExists
  ) {
    if (!tokenExists) {
      return;
    }
    staffVerificationDatabaseTable.findVerification(staffId)
      .thenAccept(originalToken -> completeVerification(staffId, password,
        submittedToken, originalToken));
  }

  private void completeVerification(
    UUID staffId, String password, String submittedToken, String originalToken
  ) {
    if (!submittedToken.equals(originalToken)) {
      return;
    }
    staffDatabaseTable.findStaff(staffId).thenAccept(staff ->
      completeVerification(staff, password));
  }

  private void completeVerification(Staff staff, String password) {
    if (!staff.passwordHash().isEmpty()) {
      return;
    }
    staffVerificationDatabaseTable.deleteVerification(staff.id());
    staffDatabaseTable.changeStaffPassword(staff, hashing.hash(password));
  }

  @RequestMapping(path = "/verification/login/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> login(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var loginFuture = login(request, body.getString("email"),
      body.getString("password"), body.getString("multiFactorCode"));
    loginFuture.thenAccept(result -> applyLoginResponseStatus(response, result));
    return loginFuture;
  }

  private void applyLoginResponseStatus(
    HttpServletResponse response, Map<String, Object> result
  ) {
    if ((boolean) result.get("success")) {
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  private CompletableFuture<Map<String, Object>> login(
    HttpServletRequest request, String email, String password,
    String multiFactorCode
  ) {
    var verification = Verification.create(staffDatabaseTable, panelKey,
      refreshKey, hashing, email.trim(), password);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    verification.isAuthenticated().thenAccept(isAuthenticated ->
      checkAuthorization(request, verification, multiFactorCode, futureResponse,
        isAuthenticated));
    return futureResponse;
  }

  private void checkAuthorization(
    HttpServletRequest request, Verification verification, String multiFactorCode,
    CompletableFuture<Map<String, Object>> futureResponse, boolean isAuthenticated
  ) {
    if (!isAuthenticated) {
      futureResponse.complete(Map.of("success", false, "error", 1000));
      return;
    }
    staffDatabaseTable.findStaff(verification.email()).thenAccept(staff ->
      multiFactorAuthFactory.createAuth(staff.id()).verifyCode(multiFactorCode)
        .thenAccept(verified -> checkMultiFactorAuth(request, staff,
          verification, futureResponse, verified)));
  }

  private void checkMultiFactorAuth(
    HttpServletRequest request, Staff staff, Verification verification,
    CompletableFuture<Map<String, Object>> futureResponse,
    boolean multiFactorVerified
  ) {
    if (!multiFactorVerified) {
      futureResponse.complete(Map.of("success", false, "error", 1001));
      return;
    }
    sessionDatabaseTable.generateAvailableSessionId()
      .thenAccept(sessionId -> completeLogin(request, verification,
        futureResponse, staff, sessionId));
  }

  private void completeLogin(
    HttpServletRequest request, Verification verification,
    CompletableFuture<Map<String, Object>> futureResponse,
    Staff staff, UUID sessionId
  ) {
    var panelApiKey = verification.generatePanelApiKey(staff.id(), sessionId);
    var refreshToken = verification.generateRefreshToken(staff.id(), sessionId);
    storeSession(request, staff.id(), sessionId, refreshToken);
    futureResponse.complete(Map.of("success", true, "panelApiKey", panelApiKey,
      "refreshToken", refreshToken));
  }

  public void storeSession(
    HttpServletRequest request, UUID staffId, UUID sessionId, String refreshToken
  ) {
    var country = "";
    var city = "";
    var ipAddress = request.getHeader("X-Real-IP");
    try {
      var location = geoDatabaseReader.city(InetAddress.getByName(ipAddress));
      country = location.getCountry().getName();
      city = location.getCity().getName();
    } catch (Exception ignored) {
    }
    try {
      var platform = UserAgent.create(request.getHeader("User-Agent"))
        .findPlatform();
      sessionDatabaseTable.insertSession(sessionId, staffId, SessionStatus.ACTIVE,
        platform, ipAddress, country, city, System.currentTimeMillis(),
        refreshToken, System.currentTimeMillis());
    } catch (Exception ignored) {
    }
  }

  @RequestMapping(path = "/verification/refresh/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> refreshVerification(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var refreshToken = body.getString("refreshToken");
    var result = verifyToken(refreshKey, refreshToken);
    if (result.getKey() != HttpServletResponse.SC_ACCEPTED) {
      return CompletableFuture.completedFuture(Map.of("success", "false"));
    }
    var staffId = UUID.fromString(result.getValue().get("id", String.class));
    var sessionId = UUID.fromString(result.getValue().get("session", String.class));
    return staffDatabaseTable().staffExists(staffId)
      .thenCompose(staffExists -> sessionDatabaseTable.sessionExists(sessionId)
        .thenCompose(sessionExists -> refreshVerification(refreshToken, staffId,
          sessionId, staffExists, sessionExists)));
  }

  private CompletableFuture<Map<String, Object>> refreshVerification(
    String refreshToken, UUID staffId, UUID sessionId, boolean staffExists,
    boolean sessionExists
  ) {
    if (!staffExists || !sessionExists) {
      return CompletableFuture.completedFuture(Map.of("success", "false"));
    }
    return staffDatabaseTable().findStaff(staffId)
      .thenCompose(staff -> sessionDatabaseTable.findSession(sessionId)
        .thenApply(session -> refreshVerification(refreshToken, staff, session)));
  }

  private Map<String, Object> refreshVerification(
    String refreshToken, Staff staff, StaffSession session
  ) {
    if (session.status().isClosed() ||
      !session.lastRefreshToken().equals(refreshToken)
    ) {
      return Map.of("success", "false");
    }
    var verification = Verification.create(staffDatabaseTable(), panelKey,
      refreshKey, hashing);
    var newPanelApiKey = verification.generatePanelApiKey(staff.id(),
      session.id());
    var newRefreshToken = verification.generateRefreshToken(staff.id(),
      session.id());
    sessionDatabaseTable.updateSessionRefreshToken(session.id(), newRefreshToken);
    return Map.of("success", "true", "panelApiKey", newPanelApiKey,
      "refreshToken", newRefreshToken);
  }

  @RequestMapping(path = "/verification/logout/", method = RequestMethod.GET)
  public void logout(HttpServletRequest request) {
    sessionDatabaseTable.closeSession(findSessionId(request));
  }

  @RequestMapping(path = "/verification/isValid/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> isValid(
    @RequestBody String payload, HttpServletResponse response
  ) {
    return isValid(panelKey, payload, response)
      .thenApply(result -> finishValidation(result.getKey()));
  }

  private Map<String, Object> finishValidation(boolean isValid) {
    return Map.of("isValid", isValid ? "true" : "false");
  }

  private CompletableFuture<Map.Entry<Boolean, Claims>> isValid(
    Key key, String payload, HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var result = verifyToken(key, body.getString("token"));
    if (result.getKey() != HttpServletResponse.SC_ACCEPTED) {
      response.setStatus(result.getKey());
      return CompletableFuture.completedFuture(
        new AbstractMap.SimpleEntry<>(false, result.getValue()));
    }
    var userId = UUID.fromString(result.getValue().get("id", String.class));
    return staffDatabaseTable().staffExists(userId)
      .thenApply(exists -> new AbstractMap.SimpleEntry<>(exists, result.getValue()));
  }

  private Map.Entry<Integer, Claims> verifyToken(Key key, String token) {
    try {
      return new AbstractMap.SimpleEntry(HttpServletResponse.SC_ACCEPTED,
        Jwts.parser()
          .setSigningKey(key)
          .build()
          .parseClaimsJws(token)
          .getPayload());
    } catch (ExpiredJwtException exception) {
      return new AbstractMap.SimpleEntry(
        HttpServletResponse.SC_EXPECTATION_FAILED, null);
    } catch (Exception exception) {
      return new AbstractMap.SimpleEntry(
        HttpServletResponse.SC_FORBIDDEN, null);
    }
  }
}
