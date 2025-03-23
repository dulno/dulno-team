package com.dulno.team.command;

import com.dulno.core.command.Command;
import com.dulno.core.environment.DulnoEnvironment;
import com.dulno.core.log.Log;
import com.dulno.core.mail.Mail;
import com.dulno.team.staff.Staff;
import com.dulno.team.staff.StaffDatabaseTable;
import com.dulno.team.staff.StaffVerificationDatabaseTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.List;
import java.util.UUID;

public final class StaffCommand extends Command {
  private final StaffDatabaseTable staffDatabaseTable;
  private final StaffVerificationDatabaseTable staffVerificationDatabaseTable;
  private final Mail verificationMail;
  private final DulnoEnvironment environment;

  @Inject
  private StaffCommand(
    Log log, StaffDatabaseTable staffDatabaseTable,
    StaffVerificationDatabaseTable staffVerificationDatabaseTable,
    @Named("verificationMail") Mail verificationMail,
    DulnoEnvironment environment
  ) {
    super(log, "staff", new String[] {"team"},
      new String[] {"add <name, email, group>", "remove <id>",
        "change <id, group>", "find <id>", "all"});
    this.staffDatabaseTable = staffDatabaseTable;
    this.staffVerificationDatabaseTable = staffVerificationDatabaseTable;
    this.verificationMail = verificationMail;
    this.environment = environment;
  }

  @Override
  public boolean execute(String[] arguments) {
    if (arguments.length == 0) {
      return false;
    }
    if (arguments[0].equalsIgnoreCase("add")) {
      return addStaff(arguments);
    }
    if (arguments[0].equalsIgnoreCase("remove")) {
      return removeStaff(arguments);
    }
    if (arguments[0].equalsIgnoreCase("change")) {
      return changeStaffGroup(arguments);
    }
    if (arguments[0].equalsIgnoreCase("find")) {
      return findStaff(arguments);
    }
    if (arguments[0].equalsIgnoreCase("all")) {
      return allStaffs(arguments);
    }
    return false;
  }

  private boolean addStaff(String[] arguments) {
    if (arguments.length != 4) {
      return false;
    }
    staffDatabaseTable.generateAvailableStaffId().thenAccept(id ->
      addStaff(id, arguments[1], arguments[2], arguments[3]));
    log().info("Staff has been added!");
    return true;
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
    name = name.replaceAll("_", " ");
    staffDatabaseTable.insertStaff(id, name, email, "", "en",
      group, Lists.newArrayList());
    var token = UUID.randomUUID().toString();
    staffVerificationDatabaseTable.insertVerification(id, token);
    var body = String.format(ONBOARDING_EMAIL_BODY, name,
      String.format(ONBOARDING_URL, environment.domain(), id.toString(), token));
    verificationMail.send(email, "en", ONBOARDING_EMAIL_TITLE, body);
  }

  private boolean removeStaff(String[] arguments) {
    if (arguments.length != 2) {
      return false;
    }
    staffDatabaseTable.deleteStaff(UUID.fromString(arguments[1]));
    log().info("Staff has been removed!");
    return true;
  }

  private boolean changeStaffGroup(String[] arguments) {
    if (arguments.length != 3) {
      return false;
    }
    staffDatabaseTable.changeStaffGroup(UUID.fromString(arguments[1]),
      arguments[2]);
    log().info("Staff group has been changed!");
    return true;
  }

  private boolean findStaff(String[] arguments) {
    if (arguments.length != 2) {
      return false;
    }
    staffDatabaseTable.findStaff(UUID.fromString(arguments[1]))
      .thenAccept(this::printStaff);
    return true;
  }

  private void printStaff(Staff member) {
    log().info("Staff:");
    log().info(" - Name: " + member.name());
    log().info(" - Email: " + member.email());
    log().info(" - Group: " + member.group());
    log().info(" - Permissions: ");
    for (var permission : member.permissions()) {
      log().info("   - " + permission);
    }
  }

  private boolean allStaffs(String[] arguments) {
    staffDatabaseTable.findAllStaff().thenAccept(this::printStaff);
    return true;
  }

  private void printStaff(List<Staff> staffList) {
    var groups = HashMultimap.<String, Staff>create();
    for (var staff : staffList) {
      groups.put(staff.group(), staff);
    }
    printStaffs(groups);
  }

  private void printStaffs(Multimap<String, Staff> staffMap) {
    for (var key : staffMap.keySet()) {
      var groupStaffList = staffMap.get(key);
      log().info(key + " (" + groupStaffList.size() + ")");
      for (var staff : groupStaffList) {
        log().info(" - " + (staff.name() + " (" + staff.email() + ") (" + staff.id() + ")"));
      }
    }
  }
}
