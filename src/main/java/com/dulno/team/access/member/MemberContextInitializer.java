package com.dulno.team.access.member;

import com.dulno.core.mail.MailFactory;
import com.dulno.core.mail.OutgoingMailDatabaseTable;
import com.dulno.core.member.session.SessionDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class MemberContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final SessionDatabaseTable sessionDatabaseTable;
  private final OutgoingMailDatabaseTable outgoingMailDatabaseTable;
  private final MailFactory mailFactory;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("sessionDatabaseTable", sessionDatabaseTable);
    beanFactory.registerSingleton("outgoingMailDatabaseTable",
      outgoingMailDatabaseTable);
    beanFactory.registerSingleton("mailFactory", mailFactory);
  }
}
