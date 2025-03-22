package com.dulno.team.staff;

import com.dulno.core.database.*;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StaffPasswordResetDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "staff_password_reset";

  public static StaffPasswordResetDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("staff", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("resetToken", DatabaseDataType.TEXT));
    return new StaffPasswordResetDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private StaffPasswordResetDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertResetToken(UUID staffId, String token) {
    insert(DatabaseRow.of(staffId, token));
  }

  public CompletableFuture<Boolean> resetTokenExists(UUID staffId) {
    return exists(staffId);
  }

  public void deleteResetToken(UUID staffId) {
    delete(staffId);
  }

  public CompletableFuture<String> findResetToken(UUID staffId) {
    return selectRow(staffId).thenApply(row -> row.findCell(1).stringValue());
  }
}
