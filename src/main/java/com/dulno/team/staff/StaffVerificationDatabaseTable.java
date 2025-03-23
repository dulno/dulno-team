package com.dulno.team.staff;

import com.dulno.core.database.*;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StaffVerificationDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "staff_verification";

  public static StaffVerificationDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("staff", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("verificationToken", DatabaseDataType.TEXT));
    return new StaffVerificationDatabaseTable(connection, keyspace,
      TABLE_NAME, columns);
  }

  private StaffVerificationDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertVerification(UUID staffId, String token) {
    insert(DatabaseRow.of(staffId, token));
  }

  public CompletableFuture<Boolean> verificationExists(UUID staffId) {
    return exists(staffId);
  }

  public void deleteVerification(UUID staffId) {
    delete(staffId);
  }

  public CompletableFuture<String> findVerification(UUID staffId) {
    return selectRow(staffId).thenApply(row -> row.findCell(1).stringValue());
  }
}
