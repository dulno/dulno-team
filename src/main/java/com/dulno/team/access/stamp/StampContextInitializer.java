package com.dulno.team.access.stamp;

import com.dulno.core.stamp.StampDatabaseTable;
import com.dulno.core.stamp.StampSetupDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class StampContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final StampDatabaseTable stampDatabaseTable;
  private final StampSetupDatabaseTable stampSetupDatabaseTable;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("stampDatabaseTable", stampDatabaseTable);
    beanFactory.registerSingleton("stampSetupDatabaseTable",
      stampSetupDatabaseTable);
  }
}
