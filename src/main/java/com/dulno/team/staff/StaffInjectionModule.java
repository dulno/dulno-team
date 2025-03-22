package com.dulno.team.staff;

import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
import com.dulno.team.staff.mfa.StaffMultiFactorAuthDatabaseTable;
import com.dulno.team.staff.session.StaffSessionDatabaseTable;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public final class StaffInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  StaffDatabaseTable provideStaffDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var staffDatabaseTable = StaffDatabaseTable.create(databaseConnection,
      databaseKeyspace);
    staffDatabaseTable.createIfNotExists();
    staffDatabaseTable.createIndexIfNotExists("email");
    staffDatabaseTable.createIndexIfNotExists("group");
    return staffDatabaseTable;
  }

  @Provides
  @Singleton
  StaffVerificationDatabaseTable provideStaffVerificationDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var staffVerificationDatabaseTable = StaffVerificationDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    staffVerificationDatabaseTable.createIfNotExists();
    return staffVerificationDatabaseTable;
  }

  @Provides
  @Singleton
  StaffEmailChangeDatabaseTable provideStaffEmailChangeDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var staffEmailChangeDatabaseTable = StaffEmailChangeDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    staffEmailChangeDatabaseTable.createIfNotExists();
    return staffEmailChangeDatabaseTable;
  }

  @Provides
  @Singleton
  StaffPasswordResetDatabaseTable provideStaffPasswordResetDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var staffPasswordResetDatabaseTable = StaffPasswordResetDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    staffPasswordResetDatabaseTable.createIfNotExists();
    return staffPasswordResetDatabaseTable;
  }

  @Provides
  @Singleton
  StaffMultiFactorAuthDatabaseTable provideStaffMultiFactorAuthDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var staffMultiFactorAuthDatabaseTable = StaffMultiFactorAuthDatabaseTable.create(
      connection, keyspace);
    staffMultiFactorAuthDatabaseTable.createIfNotExists();
    return staffMultiFactorAuthDatabaseTable;
  }

  @Provides
  @Singleton
  StaffSessionDatabaseTable provideStaffSessionDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
   return StaffSessionDatabaseTable.create(connection, keyspace);
  }
}
