package com.dulno.team.access.password;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.environment.DulnoEnvironment;
import com.dulno.core.hashing.Hashing;
import com.dulno.core.locale.Translation;
import com.dulno.core.mail.Mail;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.Staff;
import com.dulno.team.staff.StaffDatabaseTable;
import com.dulno.team.staff.StaffPasswordResetDatabaseTable;
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
public final class PasswordController extends TeamController {
  private final Mail changeMail;
  private final StaffPasswordResetDatabaseTable staffPasswordResetDatabaseTable;
  private final Translation translation;
  private final Hashing hashing;
  private final DulnoEnvironment environment;

  private PasswordController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    @Qualifier("changeMail") Mail changeMail,
    StaffPasswordResetDatabaseTable staffPasswordResetDatabaseTable,
    Translation translation, Hashing hashing, DulnoEnvironment environment
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.changeMail = changeMail;
    this.staffPasswordResetDatabaseTable = staffPasswordResetDatabaseTable;
    this.translation = translation;
    this.hashing = hashing;
    this.environment = environment;
  }

  @RequestMapping(path = "/password/reset/request/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> requestPasswordReset(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    var email = body.getString("email");
    staffDatabaseTable().staffExists(email).thenAccept(exists ->
      requestPasswordReset(email, exists).thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> requestPasswordReset(
    String email, boolean exists
  ) {
    if (!exists) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "errorCode", 1000));
    }
    return staffDatabaseTable().findStaff(email)
      .thenCompose(staff -> requestPasswordReset(staff,
        "password.reset.email.title", "password.reset.email.body"));
  }

  public CompletableFuture<Map<String, Object>> requestPasswordReset(
    Staff staff, String emailTitle, String emailBody
  ) {
    return staffPasswordResetDatabaseTable.resetTokenExists(staff.id())
      .thenCompose(requestExists -> requestPasswordReset(staff, emailTitle,
        emailBody, requestExists));
  }

  private CompletableFuture<Map<String, Object>> requestPasswordReset(
    Staff staff, String emailTitle, String emailBody, boolean requestExists
  ) {
    if (requestExists) {
      return staffPasswordResetDatabaseTable.findResetToken(staff.id())
        .thenApply(token -> sendPasswordResetMail(staff, token, emailTitle,
          emailBody));
    }
    var token = UUID.randomUUID().toString();
    staffPasswordResetDatabaseTable.insertResetToken(staff.id(), token);
    return CompletableFuture.completedFuture(sendPasswordResetMail(staff, token,
      emailTitle, emailBody));
  }

  private static final String PASSWORD_RESET_URL = "https://panel.%s/password/reset/complete/%s/%s/";

  private Map<String, Object> sendPasswordResetMail(
    Staff staff, String token, String emailTitle, String emailBody
  ) {
    var title = translation.translate(staff.language(), emailTitle);
    var body = String.format(translation.translate(staff.language(), emailBody),
      staff.name(), String.format(PASSWORD_RESET_URL, environment.domain(),
        staff.id().toString(), token));
    changeMail.send(staff.email(), staff.language(), title, body);
    return Map.of("success", true);
  }

  @RequestMapping(path = "/password/reset/complete/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> completePasswordReset(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    var staffId = body.getUUID("staff");
    staffPasswordResetDatabaseTable.resetTokenExists(staffId).thenAccept(exists ->
      completePasswordReset(staffId, body.getString("token"), exists,
        body.getString("newPassword")).thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> completePasswordReset(
    UUID staffId, String token, boolean exists, String newPassword
  ) {
    if (!exists) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1000));
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    staffPasswordResetDatabaseTable.findResetToken(staffId).thenAccept(actualToken ->
      completePasswordReset(staffId, token, actualToken, newPassword)
        .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> completePasswordReset(
    UUID staffId, String token, String actualToken, String newPassword
  ) {
    if (!token.equals(actualToken)) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1001));
    }
    staffPasswordResetDatabaseTable.deleteResetToken(staffId);
    staffDatabaseTable().changeStaffPassword(staffId, hashing.hash(newPassword));
    return CompletableFuture.completedFuture(Map.of("success", true));
  }
}

