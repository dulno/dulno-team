package com.dulno.team.access.member;

import com.dulno.core.mail.MailFactory;
import com.dulno.core.mail.OutgoingMailDatabaseTable;
import com.dulno.core.member.MemberDatabaseTable;
import com.dulno.core.member.session.MemberSessionDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class MemberContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final MemberDatabaseTable memberDatabaseTable;
  private final MemberSessionDatabaseTable memberSessionDatabaseTable;
  private final OutgoingMailDatabaseTable outgoingMailDatabaseTable;
  private final MailFactory mailFactory;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("memberDatabaseTable", memberDatabaseTable);
    beanFactory.registerSingleton("memberSessionDatabaseTable",
      memberSessionDatabaseTable);
    beanFactory.registerSingleton("outgoingMailDatabaseTable",
      outgoingMailDatabaseTable);
    beanFactory.registerSingleton("mailFactory", mailFactory);
  }
}
