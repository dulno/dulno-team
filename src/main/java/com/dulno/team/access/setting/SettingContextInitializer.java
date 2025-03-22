package com.dulno.team.access.setting;

import com.dulno.team.staff.mfa.StaffMultiFactorAuthDatabaseTable;
import com.dulno.team.staff.mfa.StaffMultiFactorAuthFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class SettingContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final StaffMultiFactorAuthDatabaseTable staffMultiFactorAuthDatabaseTable;
  private final StaffMultiFactorAuthFactory staffMultiFactorAuthFactory;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("teamMultiFactorAuthDatabaseTable",
      staffMultiFactorAuthDatabaseTable);
    beanFactory.registerSingleton("teamMultiFactorAuthFactory",
      staffMultiFactorAuthFactory);
  }
}
