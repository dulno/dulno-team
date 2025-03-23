package com.dulno.team.staff;

import com.dulno.core.database.*;
import com.google.common.collect.Lists;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StaffEmailChangeDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "staff_email_change";

  public static StaffEmailChangeDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("staff", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("newEmail", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("changeToken", DatabaseDataType.TEXT));
    return new StaffEmailChangeDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private StaffEmailChangeDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertChange(UUID staffId, String newEmail, String token) {
    insert(DatabaseRow.of(staffId, newEmail, token));
  }

  public void updateChange(UUID staffId, String newEmail, String token) {
    update(staffId, DatabaseRow.of(staffId, newEmail, token));
  }

  public CompletableFuture<Boolean> changeExists(UUID staffId) {
    return exists(staffId);
  }

  public void deleteChange(UUID staffId) {
    delete(staffId);
  }

  public CompletableFuture<Map.Entry<String, String>> findChange(UUID staffId) {
    return selectRow(staffId).thenApply(row ->
      new AbstractMap.SimpleEntry<>(row.findCell(1).stringValue(),
        row.findCell(2).stringValue()));
  }
}
