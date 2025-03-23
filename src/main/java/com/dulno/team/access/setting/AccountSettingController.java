package com.dulno.team.access.setting;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.environment.DulnoEnvironment;
import com.dulno.core.hashing.Hashing;
import com.dulno.core.locale.Translation;
import com.dulno.core.mail.Mail;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.Staff;
import com.dulno.team.staff.StaffDatabaseTable;
import com.dulno.team.staff.StaffEmailChangeDatabaseTable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class AccountSettingController extends TeamController {
  private final StaffEmailChangeDatabaseTable staffEmailChangeDatabaseTable;
  private final Translation translation;
  private final Mail changeMail;
  private final Hashing hashing;
  private final DulnoEnvironment environment;

  private AccountSettingController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    StaffEmailChangeDatabaseTable staffEmailChangeDatabaseTable,
    Translation translation, @Qualifier("changeMail") Mail changeMail,
    Hashing hashing, DulnoEnvironment environment
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.staffEmailChangeDatabaseTable = staffEmailChangeDatabaseTable;
    this.translation = translation;
    this.changeMail = changeMail;
    this.hashing = hashing;
    this.environment = environment;
  }

  @RequestMapping(path = "/settings/account/password/change/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> changePassword(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    findStaff(request).thenAccept(staff -> futureResponse.complete(
      changePassword(staff, body.getString("currentPassword"),
        body.getString("newPassword"))));
    return futureResponse;
  }

  private Map<String, Object> changePassword(
    Staff staff, String currentPassword, String newPassword
  ) {
    if (!hashing.matches(currentPassword, staff.passwordHash())) {
      return Map.of("success", false);
    }
    staffDatabaseTable().changeStaffPassword(staff.id(),
      hashing.hash(newPassword));
    return Map.of("success", true);
  }

  @RequestMapping(path = "/settings/account/email/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findEmail(
    HttpServletRequest request
  ) {
    return findStaff(request).thenApply(staff -> Map.of("email", staff.email()));
  }

  @RequestMapping(path = "/settings/account/email/change/request/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> requestEmailChange(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    var newEmail = body.getString("newEmail");
    findStaff(request).thenAccept(staff -> staffDatabaseTable().staffExists(newEmail)
      .thenAccept(exists -> requestEmailChange(staff, body.getString("password"),
        newEmail, exists).thenAccept(futureResponse::complete)));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> requestEmailChange(
    Staff staff, String password, String newEmail, boolean accountExists
  ) {
    if (!hashing.matches(password, staff.passwordHash())) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1000));
    }
    if (accountExists) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1001));
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    staffEmailChangeDatabaseTable.changeExists(staff.id()).thenAccept(requestExists ->
      futureResponse.complete(requestEmailChange(staff, newEmail, requestExists)));
    return futureResponse;
  }

  private static final String EMAIL_CHANGE_URL = "https://panel.%s/email/change/complete/%s/%s/";

  private Map<String, Object> requestEmailChange(
    Staff staff, String newEmail, boolean requestExists
  ) {
    var token = UUID.randomUUID().toString();
    if (requestExists) {
      staffEmailChangeDatabaseTable.updateChange(staff.id(), newEmail, token);
    } else {
      staffEmailChangeDatabaseTable.insertChange(staff.id(), newEmail, token);
    }
    var title = translation.translate(staff.language(), "email.change.email.title");
    var body = String.format(
      translation.translate(staff.language(), "email.change.email.body"),
      String.format(EMAIL_CHANGE_URL, environment.domain(),
        staff.id().toString(), token));
    changeMail.send(newEmail, staff.language(), title, body);
    return Map.of("success", true);
  }

  @RequestMapping(path = "/settings/account/email/change/complete/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> completeEmailChange(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    var staffId = body.getUUID("staff");
    staffEmailChangeDatabaseTable.changeExists(staffId).thenAccept(exists ->
      completeEmailChange(staffId, body.getString("token"), exists)
        .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> completeEmailChange(
    UUID staffId, String token, boolean exists
  ) {
    if (!exists) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1000));
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    staffEmailChangeDatabaseTable.findChange(staffId).thenAccept(changeParameters ->
      completeEmailChange(staffId, token, changeParameters)
        .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> completeEmailChange(
    UUID staffId, String token, Map.Entry<String, String> changeParameters
  ) {
    if (!token.equals(changeParameters.getValue())) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1001));
    }
    staffEmailChangeDatabaseTable.deleteChange(staffId);
    staffDatabaseTable().changeStaffEmail(staffId, changeParameters.getKey());
    return CompletableFuture.completedFuture(Map.of("success", true));
  }
}
