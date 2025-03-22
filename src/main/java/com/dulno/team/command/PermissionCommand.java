package com.dulno.team.command;

import com.dulno.core.command.Command;
import com.dulno.core.log.Log;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.StaffDatabaseTable;
import com.google.inject.Inject;

import java.util.UUID;

public final class PermissionCommand extends Command {
  private final StaffDatabaseTable staffDatabaseTable;
  private final GroupDatabaseTable groupDatabaseTable;

  @Inject
  private PermissionCommand(
    Log log, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable
  ) {
    super(log, "permission", new String[] {"permissions"}, new String[] {
      "staff add <id, permission>", "staff remove <id, permission>",
      "group add <name, permission>", "group remove <name, permission>"});
    this.staffDatabaseTable = staffDatabaseTable;
    this.groupDatabaseTable = groupDatabaseTable;
  }

  @Override
  public boolean execute(String[] arguments) {
    if (arguments.length == 0) {
      return false;
    }
    if (arguments[0].equalsIgnoreCase("staff")) {
      return staffPermission(arguments);
    }
    if (arguments[0].equalsIgnoreCase("group")) {
      return groupPermission(arguments);
    }
    return false;
  }

  private boolean staffPermission(String[] arguments) {
    if (arguments.length < 2) {
      return false;
    }
    if (arguments[1].equalsIgnoreCase("add")) {
      return addStaffPermission(arguments);
    }
    if (arguments[1].equalsIgnoreCase("remove")) {
      return removeStaffPermission(arguments);
    }
    return true;
  }

  private boolean addStaffPermission(String[] arguments) {
    if (arguments.length != 4) {
      return false;
    }
    staffDatabaseTable.addStaffPermission(UUID.fromString(arguments[2]),
      arguments[3]);
    log().info("Staff permission added!");
    return true;
  }

  private boolean removeStaffPermission(String[] arguments) {
    if (arguments.length != 4) {
      return false;
    }
    staffDatabaseTable.removeStaffPermission(UUID.fromString(arguments[2]),
      arguments[3]);
    log().info("Staff permission removed!");
    return true;
  }

  private boolean groupPermission(String[] arguments) {
    if (arguments.length < 2) {
      return false;
    }
    if (arguments[1].equalsIgnoreCase("add")) {
      return addGroupPermission(arguments);
    }
    if (arguments[1].equalsIgnoreCase("remove")) {
      return removeGroupPermission(arguments);
    }
    return true;
  }

  private boolean addGroupPermission(String[] arguments) {
    if (arguments.length != 4) {
      return false;
    }
    groupDatabaseTable.addGroupPermission(arguments[2], arguments[3]);
    log().info("Group permission added!");
    return true;
  }

  private boolean removeGroupPermission(String[] arguments) {
    if (arguments.length != 4) {
      return false;
    }
    groupDatabaseTable.removeGroupPermission(arguments[2], arguments[3]);
    log().info("Group permission removed!");
    return true;
  }
}
