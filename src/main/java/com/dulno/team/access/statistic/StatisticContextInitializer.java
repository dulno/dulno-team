package com.dulno.team.access.statistic;

import com.dulno.core.partner.analysis.card.PartnerNewCardDatabaseTable;
import com.dulno.core.scan.ScanDatabaseTable;
import com.dulno.core.user.statistic.UserAppOpenDatabaseTable;
import com.dulno.core.user.statistic.UserJoinDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class StatisticContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final UserJoinDatabaseTable userJoinDatabaseTable;
  private final UserAppOpenDatabaseTable userAppOpenDatabaseTable;
  private final ScanDatabaseTable scanDatabaseTable;
  private final PartnerNewCardDatabaseTable partnerNewCardDatabaseTable;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("userJoinDatabaseTable",
      userJoinDatabaseTable);
    beanFactory.registerSingleton("userAppOpenDatabaseTable",
      userAppOpenDatabaseTable);
    beanFactory.registerSingleton("scanDatabaseTable", scanDatabaseTable);
    beanFactory.registerSingleton("partnerNewCardDatabaseTable",
      partnerNewCardDatabaseTable);
  }
}
