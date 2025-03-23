package com.dulno.team.staff.mfa;

import com.dulno.core.database.*;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StaffMultiFactorAuthDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "staff_multi_factor_auth";

  public static StaffMultiFactorAuthDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("staff", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("secret", DatabaseDataType.TEXT));
    columns.add(DatabaseListColumn.create("recoveryCodes", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("confirmed", DatabaseDataType.BOOLEAN));
    return new StaffMultiFactorAuthDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private StaffMultiFactorAuthDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public CompletableFuture<Void> insertAuth(
    UUID staffId, String secret, List<String> recoveryCodes
  ) {
    return insert(DatabaseRow.of(staffId, secret, recoveryCodes, false),
      "USING TTL " + (60 * 60));
  }

  public CompletableFuture<Void> confirmAuth(UUID staffId) {
    return findAuth(staffId).thenCompose(auth ->
      deleteAuth(staffId).thenCompose(value ->
        insert(DatabaseRow.of(staffId, auth.secret(), auth.recoveryCodes(), true))));
  }

  public CompletableFuture<Boolean> authExists(UUID staffId) {
    return exists(staffId);
  }

  public CompletableFuture<Void> deleteAuth(UUID staffId) {
    return delete(staffId);
  }

  public CompletableFuture<StaffMultiFactorAuthEntry> findAuth(UUID staffId) {
    return selectRow(staffId).thenApply(StaffMultiFactorAuthEntry::of);
  }
}
