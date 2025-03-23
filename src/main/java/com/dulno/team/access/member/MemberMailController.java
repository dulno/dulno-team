package com.dulno.team.access.member;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.error.ErrorRepository;
import com.dulno.core.mail.*;
import com.dulno.core.member.MemberDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.StaffDatabaseTable;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class MemberMailController extends TeamController {
  private final MemberDatabaseTable memberDatabaseTable;
  private final OutgoingMailDatabaseTable mailDatabaseTable;
  private final MailFactory mailFactory;
  private final ErrorRepository errorRepository;

  private MemberMailController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, MemberDatabaseTable memberDatabaseTable,
    OutgoingMailDatabaseTable mailDatabaseTable, MailFactory mailFactory,
    ErrorRepository errorRepository
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.memberDatabaseTable = memberDatabaseTable;
    this.mailDatabaseTable = mailDatabaseTable;
    this.mailFactory = mailFactory;
    this.errorRepository = errorRepository;
  }

  @RequestMapping(path = "/mails/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findStaffMails(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var memberId = body.getUUID("member");
    return staffHasPermission(findStaffId(request), "member.mails.find")
      .thenCompose(hasPermission -> memberDatabaseTable.memberExists(memberId)
        .thenCompose(exists -> findStaffMails(memberId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findStaffMails(
    UUID memberId, boolean hasPermission, boolean memberExists
  ) {
    if (!hasPermission || !memberExists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return memberDatabaseTable.findMember(memberId)
      .thenCompose(member -> mailDatabaseTable.findOutgoingMailsByReceiver(member.email())
        .thenApply(mails -> Map.of("mails", mails.stream()
          .sorted(Comparator.comparingLong(OutgoingMail::time).reversed())
          .map(this::assemblyMailInformation).toList())));
  }

  private Map<String, Object> assemblyMailInformation(OutgoingMail mail) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", mail.id());
    information.put("sender", mail.sender());
    information.put("time", mail.time());
    information.put("title", mail.title());
    return information;
  }

  @RequestMapping(path = "/mail/resend/", method = RequestMethod.POST)
  public void resendMail(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var mailId = body.getUUID("mail");
    staffHasPermission(findStaffId(request), "member.mail.resend")
      .thenAccept(hasPermission -> mailDatabaseTable.outgoingMailExists(mailId)
        .thenAccept(exists -> resendMail(mailId, hasPermission, exists)));
  }

  private void resendMail(
    UUID mailId, boolean hasPermission, boolean mailExists
  ) {
    if (!hasPermission || !mailExists) {
      return;
    }
    mailDatabaseTable.findOutgoingMail(mailId).thenAccept(this::resendMail);
  }

  private void resendMail(OutgoingMail outgoingMail) {
    try {
      var mail = createMail(outgoingMail.sender());
      mail.resendEmail(outgoingMail);
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }

  private Mail createMail(String sender) throws Exception {
    var mailConfiguration = MailConfiguration.createAndLoad(sender.split("@")[0]);
    return mailFactory.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }
}
