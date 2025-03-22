package com.dulno.team.access.verification;

import com.dulno.core.member.MemberDatabaseTable;
import com.dulno.team.staff.StaffEmailChangeDatabaseTable;
import com.dulno.team.staff.StaffPasswordResetDatabaseTable;
import com.dulno.team.staff.StaffVerificationDatabaseTable;
import com.dulno.team.staff.session.StaffSessionDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.maxmind.geoip2.DatabaseReader;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class VerificationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final MemberDatabaseTable memberDatabaseTable;
  private final StaffVerificationDatabaseTable staffVerificationDatabaseTable;
  private final StaffEmailChangeDatabaseTable staffEmailChangeDatabaseTable;
  private final StaffPasswordResetDatabaseTable staffPasswordResetDatabaseTable;
  private final StaffSessionDatabaseTable staffSessionDatabaseTable;
  private final DatabaseReader databaseReader;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("memberDatabaseTable", memberDatabaseTable);
    beanFactory.registerSingleton("teamVerificationDatabaseTable",
      staffVerificationDatabaseTable);
    beanFactory.registerSingleton("teamEmailChangeDatabaseTable",
      staffEmailChangeDatabaseTable);
    beanFactory.registerSingleton("teamPasswordResetDatabaseTable",
      staffPasswordResetDatabaseTable);
    beanFactory.registerSingleton("teamSessionDatabaseTable",
      staffSessionDatabaseTable);
    beanFactory.registerSingleton("databaseReader", databaseReader);
  }
}
