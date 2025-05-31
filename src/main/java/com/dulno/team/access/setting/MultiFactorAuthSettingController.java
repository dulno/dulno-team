package com.dulno.team.access.setting;

import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.hashing.Hashing;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.Staff;
import com.dulno.team.staff.StaffDatabaseTable;
import com.dulno.team.staff.mfa.StaffMultiFactorAuthDatabaseTable;
import com.dulno.team.staff.mfa.StaffMultiFactorAuthEntry;
import com.dulno.team.staff.mfa.StaffMultiFactorAuthFactory;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class MultiFactorAuthSettingController extends TeamController {
  private final StaffMultiFactorAuthDatabaseTable multiFactorAuthDatabaseTable;
  private final StaffMultiFactorAuthFactory multiFactorAuthFactory;
  private final Hashing hashing;

  private MultiFactorAuthSettingController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    StaffMultiFactorAuthDatabaseTable multiFactorAuthDatabaseTable,
    StaffMultiFactorAuthFactory multiFactorAuthFactory, Hashing hashing
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.multiFactorAuthDatabaseTable = multiFactorAuthDatabaseTable;
    this.multiFactorAuthFactory = multiFactorAuthFactory;
    this.hashing = hashing;
  }

  @RequestMapping(path = "/settings/2fa/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> multiFactorAuth(
    HttpServletRequest request
  ) {
    return multiFactorAuthDatabaseTable.authExists(findStaffId(request))
      .thenApply(exists -> Map.of("enabled", exists));
  }

  @RequestMapping(path = "/settings/2fa/switch/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> switchMultiFactorAuth(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    return findStaff(request).thenCompose(staff -> switchMultiFactorAuth(staff,
      body.getString("password")));
  }

  private CompletableFuture<Map<String, Object>> switchMultiFactorAuth(
    Staff staff, String password
  ) {
    if (!hashing.matches(password, staff.passwordHash())) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    return multiFactorAuthDatabaseTable.authExists(staff.id())
      .thenCompose(exists -> switchMultiFactorAuth(staff.id(), exists));
  }

  private CompletableFuture<Map<String, Object>> switchMultiFactorAuth(
    UUID staffId, boolean multiFactorAuthEnabled
  ) {
    if (multiFactorAuthEnabled) {
      multiFactorAuthDatabaseTable.deleteAuth(staffId);
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var auth = multiFactorAuthFactory.createAuth(staffId);
    return auth.setup().thenCompose(value -> auth.generateQRCode()
      .thenApply(Base64.getEncoder()::encodeToString)
      .thenCompose(qrCodeEncoded -> multiFactorAuthDatabaseTable.findAuth(staffId)
        .thenApply(authUser -> Map.of("qrCode", qrCodeEncoded, "secret",
          authUser.secret(), "recoveryCodes", authUser.recoveryCodes()))));
  }

  @RequestMapping(path = "/settings/2fa/confirm/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> confirmMultiFactorAuth(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var staffId = findStaffId(request);
    return multiFactorAuthDatabaseTable.authExists(staffId)
      .thenCompose(exists -> confirmMultiFactorAuth(staffId,
        body.getString("code"), exists));
  }

  private CompletableFuture<Map<String, Object>> confirmMultiFactorAuth(
    UUID staffId, String code, boolean multiFactorEnabled
  ) {
    if (!multiFactorEnabled) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    return multiFactorAuthDatabaseTable.findAuth(staffId)
      .thenCompose(auth -> confirmMultiFactorAuth(staffId, code, auth));
  }

  private CompletableFuture<Map<String, Object>> confirmMultiFactorAuth(
    UUID staffId, String code, StaffMultiFactorAuthEntry auth
  ) {
    if (auth.confirmed()) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    return multiFactorAuthFactory.createAuth(staffId).verifyCode(code)
      .thenCompose(verified -> !verified ?
        CompletableFuture.completedFuture(Map.of("success", false)) :
        multiFactorAuthDatabaseTable.confirmAuth(staffId)
          .thenApply(value -> Map.of("success", true)));
  }

  @RequestMapping(path = "/settings/2fa/isConfirmed/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> isMultiFactorAuthConfirmed(
    HttpServletRequest request
  ) {
    var staffId = findStaffId(request);
    return multiFactorAuthDatabaseTable.authExists(staffId)
      .thenCompose(exists -> isMultiFactorAuthConfirmed(staffId, exists));
  }

  private CompletableFuture<Map<String, Object>> isMultiFactorAuthConfirmed(
    UUID staffId, boolean multiFactorEnabled
  ) {
    if (!multiFactorEnabled) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    return multiFactorAuthDatabaseTable.findAuth(staffId)
      .thenApply(auth -> Map.of("success", true, "confirmed", auth.confirmed()));
  }
}