package com.dulno.team.access.staff;

import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.environment.DulnoEnvironment;
import com.dulno.core.mail.Mail;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.*;
import com.dulno.team.staff.mfa.StaffMultiFactorAuthDatabaseTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class StaffController extends TeamController {
  private final StaffVerificationDatabaseTable staffVerificationDatabaseTable;
  private final StaffPasswordResetDatabaseTable staffPasswordResetDatabaseTable;
  private final StaffEmailChangeDatabaseTable staffEmailChangeDatabaseTable;
  private final StaffMultiFactorAuthDatabaseTable staffMultiFactorAuthDatabaseTable;
  private final Mail verificationMail;
  private final DulnoEnvironment environment;

  private StaffController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    StaffVerificationDatabaseTable staffVerificationDatabaseTable,
    StaffPasswordResetDatabaseTable staffPasswordResetDatabaseTable,
    StaffEmailChangeDatabaseTable staffEmailChangeDatabaseTable,
    StaffMultiFactorAuthDatabaseTable staffMultiFactorAuthDatabaseTable,
    @Qualifier("verificationMail") Mail verificationMail,
    DulnoEnvironment environment
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.staffVerificationDatabaseTable = staffVerificationDatabaseTable;
    this.staffPasswordResetDatabaseTable = staffPasswordResetDatabaseTable;
    this.staffEmailChangeDatabaseTable = staffEmailChangeDatabaseTable;
    this.staffMultiFactorAuthDatabaseTable = staffMultiFactorAuthDatabaseTable;
    this.verificationMail = verificationMail;
    this.environment = environment;
  }

  @RequestMapping(path = "/staff/add/", method = RequestMethod.POST)
  public void addStaff(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    staffHasPermission(findStaffId(request), "staff.add").thenAccept(
      hasPermission -> addStaff(body.getString("name"), body.getString("email"),
        body.getString("group"), hasPermission));
  }

  private void addStaff(
    String name, String email, String group, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    staffDatabaseTable().generateAvailableStaffId()
      .thenAccept(id -> addStaff(id, name, email, group));
  }

  private static final String ONBOARDING_EMAIL_TITLE = "Onboarding";
  private static final String ONBOARDING_URL = "https://panel.%s/confirm/%s/%s/";
  private static final String ONBOARDING_EMAIL_BODY = "Hey %s,\n" +
    "\n" +
    "welcome to the Dulno team. Here's to good cooperation! " +
    "In the following you will get access to the Dulno-Panel. " +
    "Please follow the steps below.\n" +
    "\n" +
    "Click the link below and set your panel password:\n" +
    "%s\n" +
    "\n" +
    "That's it already.\n" +
    "\n" +
    "Welcome aboard!";

  private void addStaff(
    UUID id, String name, String email, String group
  ) {
    staffDatabaseTable().insertStaff(id, name, email, "", "en",
      group, Lists.newArrayList());
    var token = UUID.randomUUID().toString();
    staffVerificationDatabaseTable.insertVerification(id, token);
    var body = String.format(ONBOARDING_EMAIL_BODY, name,
      String.format(ONBOARDING_URL, environment.domain(), id.toString(), token));
    verificationMail.send(email, "en", ONBOARDING_EMAIL_TITLE, body);
  }

  @RequestMapping(path = "/staff/remove/", method = RequestMethod.POST)
  public void removeStaff(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var staffId = body.getUUID("staff");
    staffHasPermission(findStaffId(request), "staff.remove").thenAccept(
      hasPermission -> staffDatabaseTable().staffExists(staffId).thenAccept(
        staffExists -> removeStaff(staffId, hasPermission, staffExists)));
  }

  private void removeStaff(
    UUID staffId, boolean hasPermission, boolean staffExists
  ) {
    if (!hasPermission || !staffExists) {
      return;
    }
    staffDatabaseTable().deleteStaff(staffId);
    staffPasswordResetDatabaseTable.deleteResetToken(staffId);
    staffEmailChangeDatabaseTable.deleteChange(staffId);
    staffMultiFactorAuthDatabaseTable.deleteAuth(staffId);
  }

  @RequestMapping(path = "/staff/group/change/", method = RequestMethod.POST)
  public void changeStaffGroup(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var staffId = body.getUUID("staff");
    staffHasPermission(findStaffId(request), "staff.group.change")
      .thenAccept(hasPermission -> staffDatabaseTable().staffExists(staffId)
        .thenAccept(staffExists -> changeStaffGroup(staffId,
          body.getString("group"), hasPermission, staffExists)));
  }

  private void changeStaffGroup(
    UUID staffId, String group, boolean hasPermission, boolean staffExists
  ) {
    if (!hasPermission || !staffExists) {
      return;
    }
    staffDatabaseTable().changeStaffGroup(staffId, group);
  }

  @RequestMapping(path = "/staff/permission/add/", method = RequestMethod.POST)
  public void addStaffPermission(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var staffId = body.getUUID("staff");
    staffHasPermission(findStaffId(request), "staff.permission.add")
      .thenAccept(hasPermission -> staffDatabaseTable().staffExists(staffId)
        .thenAccept(staffExists -> addStaffPermission(staffId,
          body.getString("permission"), hasPermission, staffExists)));
  }

  private void addStaffPermission(
    UUID staffId, String permission, boolean hasPermission, boolean staffExists
  ) {
    if (!hasPermission || !staffExists) {
      return;
    }
    staffDatabaseTable().addStaffPermission(staffId, permission);
  }

  @RequestMapping(path = "/staff/permission/remove/", method = RequestMethod.POST)
  public void removeStaffPermission(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var staffId = body.getUUID("staff");
    staffHasPermission(findStaffId(request), "staff.permission.remove")
      .thenAccept(hasPermission -> staffDatabaseTable().staffExists(staffId)
        .thenAccept(staffExists -> removeStaffPermission(staffId,
          body.getString("permission"), hasPermission, staffExists)));
  }

  private void removeStaffPermission(
    UUID staffId, String permission, boolean hasPermission, boolean staffExists
  ) {
    if (!hasPermission || !staffExists) {
      return;
    }
    staffDatabaseTable().removeStaffPermission(staffId, permission);
  }

  @RequestMapping(path = "/staff/has/permission/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> hasStaffPermission(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    staffHasPermission(findStaffId(request), body.getString("permission"))
      .thenAccept(hasPermission -> futureResponse.complete(
        Map.of("hasPermission", hasPermission)));
    return futureResponse;
  }

  @RequestMapping(path = "/staff/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findAllStaff(
    HttpServletRequest request
  ) {
    return staffHasPermission(findStaffId(request), "staff.find")
      .thenCompose(this::findAllStaff);
  }

  private CompletableFuture<Map<String, Object>> findAllStaff(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return staffDatabaseTable().findAllStaff()
      .thenApply(staff -> Map.of("staff", findStaffInformation(staff)));
  }

  private List<Map<String, Object>> findStaffInformation(List<Staff> staffList) {
    var result = Lists.<Map<String, Object>>newArrayList();
    for (var staff : staffList) {
      result.add(assemblyStaffInformation(staff));
    }
    return result;
  }

  private Map<String, Object> assemblyStaffInformation(Staff staff) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", staff.id());
    information.put("name", staff.name());
    information.put("group", staff.group());
    information.put("permissions", staff.permissions());
    return information;
  }
}
