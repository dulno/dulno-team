package com.dulno.team.access.partner;

import com.dulno.core.bundle.BundleDatabaseTable;
import com.dulno.core.bundle.BundlePresetRepository;
import com.dulno.core.partner.PartnerDatabaseTable;
import com.dulno.core.partner.link.PartnerLinkDatabaseTable;
import com.dulno.core.partner.location.PartnerLocationDatabaseTable;
import com.dulno.core.partner.logo.PartnerLogoDatabaseTable;
import com.dulno.core.partner.member.PartnerMemberDatabaseTable;
import com.dulno.core.stripe.StripeDatabaseTable;
import com.dulno.core.stripe.TerminationDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.stripe.StripeClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class PartnerContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final PartnerDatabaseTable partnerDatabaseTable;
  private final PartnerMemberDatabaseTable partnerMemberDatabaseTable;
  private final PartnerLogoDatabaseTable partnerLogoDatabaseTable;
  private final PartnerLocationDatabaseTable partnerLocationDatabaseTable;
  private final PartnerLinkDatabaseTable partnerLinkDatabaseTable;
  private final BundleDatabaseTable bundleDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;
  private final StripeDatabaseTable stripeDatabaseTable;
  private final StripeClient stripeClient;
  private final TerminationDatabaseTable terminationDatabaseTable;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("partnerDatabaseTable", partnerDatabaseTable);
    beanFactory.registerSingleton("partnerMemberDatabaseTable",
      partnerMemberDatabaseTable);
    beanFactory.registerSingleton("partnerLogoDatabaseTable",
      partnerLogoDatabaseTable);
    beanFactory.registerSingleton("partnerLocationDatabaseTable",
      partnerLocationDatabaseTable);
    beanFactory.registerSingleton("partnerLinkDatabaseTable",
      partnerLinkDatabaseTable);
    beanFactory.registerSingleton("bundleDatabaseTable", bundleDatabaseTable);
    beanFactory.registerSingleton("bundlePresetRepository",
      bundlePresetRepository);
    beanFactory.registerSingleton("stripeDatabaseTable", stripeDatabaseTable);
    beanFactory.registerSingleton("stripeClient", stripeClient);
    beanFactory.registerSingleton("terminationDatabaseTable",
      terminationDatabaseTable);
  }
}
