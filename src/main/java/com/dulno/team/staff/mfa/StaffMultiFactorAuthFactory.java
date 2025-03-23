package com.dulno.team.staff.mfa;

import com.dulno.team.staff.StaffDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class StaffMultiFactorAuthFactory {
  private final StaffMultiFactorAuthDatabaseTable multiFactorAuthDatabaseTable;
  private final StaffDatabaseTable staffDatabaseTable;

  public StaffMultiFactorAuth createAuth(UUID staffId) {
    return StaffMultiFactorAuth.create(multiFactorAuthDatabaseTable,
      staffDatabaseTable, staffId);
  }
}
