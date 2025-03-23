package com.dulno.team.staff;

import com.dulno.core.database.*;
import com.dulno.core.database.condition.DatabaseCondition;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class StaffDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "staff";

  public static StaffDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("id", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("name", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("email", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("passwordHash", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("language", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("group", DatabaseDataType.TEXT));
    columns.add(DatabaseListColumn.create("permissions", DatabaseDataType.TEXT));
    return new StaffDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private StaffDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertStaff(Staff staff) {
    insertStaff(staff.id(), staff.name(), staff.email(),
      staff.passwordHash(), staff.language(), staff.group(),
      staff.permissions());
  }

  public void insertStaff(
    UUID id, String name, String email, String passwordHash, String language,
    String group, List<String> permissions
  ) {
    insert(DatabaseRow.of(id, name, email, passwordHash, language, group,
      permissions));
  }

  public void changeStaffName(UUID staffId, String name) {
    findStaff(staffId).thenAccept(staff ->
      changeStaffName(staff, name));
  }

  public void changeStaffName(Staff staff, String name) {
    staff.changeName(name);
    updateStaff(staff);
  }

  public void changeStaffEmail(UUID staffId, String email) {
    findStaff(staffId).thenAccept(staff ->
      changeStaffEmail(staff, email));
  }

  public void changeStaffEmail(Staff staff, String email) {
    staff.changeEmail(email);
    updateStaff(staff);
  }

  public void changeStaffPassword(UUID staffId, String passwordHash) {
    findStaff(staffId).thenAccept(staff ->
      changeStaffPassword(staff, passwordHash));
  }

  public void changeStaffPassword(Staff staff, String passwordHash) {
    staff.changePassword(passwordHash);
    updateStaff(staff);
  }

  public void changeStaffLanguage(UUID staffId, String language) {
    findStaff(staffId).thenAccept(staff ->
      changeStaffLanguage(staff, language));
  }

  public void changeStaffLanguage(Staff staff, String language) {
    staff.changeLanguage(language);
    updateStaff(staff);
  }

  public void addStaffPermission(UUID staffId, String permission) {
    findStaff(staffId).thenAccept(staff ->
      addStaffPermission(staff, permission));
  }

  public void addStaffPermission(Staff staff, String permission) {
    staff.addPermission(permission);
    updateStaff(staff);
  }

  public void removeStaffPermission(UUID staffId, String permission) {
    findStaff(staffId).thenAccept(staff ->
      removeStaffPermission(staff, permission));
  }

  public void removeStaffPermission(Staff staff, String permission) {
    staff.removePermission(permission);
    updateStaff(staff);
  }

  public void changeStaffGroup(UUID staffId, String group) {
    findStaff(staffId).thenAccept(staff ->
      changeStaffGroup(staff, group));
  }

  public void changeStaffGroup(Staff staff, String group) {
    staff.changeGroup(group);
    updateStaff(staff);
  }

  private void updateStaff(Staff staff) {
    update(staff.id(), DatabaseRow.of(staff.id(),
      staff.name(), staff.email(), staff.passwordHash(),
      staff.language(), staff.group(), staff.permissions()));
  }

  public void deleteStaff(UUID id) {
    delete(id);
  }

  public CompletableFuture<UUID> generateAvailableStaffId() {
    var futureResponse = new CompletableFuture<UUID>();
    var id = UUID.randomUUID();
    staffExists(id).thenApply(exists -> exists ?
      generateAvailableStaffId().thenApply(futureResponse::complete) :
      CompletableFuture.completedFuture(futureResponse.complete(id)));
    return futureResponse;
  }

  public CompletableFuture<Boolean> staffExists(UUID id) {
    return exists(id);
  }

  public CompletableFuture<Boolean> staffExists(String email) {
    return exists(DatabaseCondition.of("email", email.toLowerCase()));
  }

  public CompletableFuture<Staff> findStaff(UUID id) {
    return selectRow(id).thenApply(Staff::of);
  }

  public CompletableFuture<Staff> findStaff(String email) {
    return selectRow(DatabaseCondition.of("email", email.toLowerCase()))
      .thenApply(Staff::of);
  }

  public CompletableFuture<List<Staff>> findStaffOfGroup(String group) {
    return selectRows(DatabaseCondition.of("group", group)).thenApply(rows ->
      rows.stream().map(Staff::of).collect(Collectors.toList()));
  }

  public CompletableFuture<List<Staff>> findAllStaff() {
    return selectAllRows().thenApply(rows ->
      rows.stream().map(Staff::of).collect(Collectors.toList()));
  }
}

