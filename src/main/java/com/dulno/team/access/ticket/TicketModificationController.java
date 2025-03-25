package com.dulno.team.access.ticket;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.environment.DulnoEnvironment;
import com.dulno.core.locale.Translation;
import com.dulno.core.mail.Mail;
import com.dulno.core.member.Member;
import com.dulno.core.member.MemberDatabaseTable;
import com.dulno.core.ticket.*;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.Staff;
import com.dulno.team.staff.StaffDatabaseTable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.UUID;

@RestController
public final class TicketModificationController extends TeamController {
  private final TicketDatabaseTable ticketDatabaseTable;
  private final TicketMessageDatabaseTable ticketMessageDatabaseTable;
  private final MemberDatabaseTable memberDatabaseTable;
  private final Translation translation;
  private final Mail supportMail;
  private final DulnoEnvironment environment;

  private TicketModificationController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, TicketDatabaseTable ticketDatabaseTable,
    TicketMessageDatabaseTable ticketMessageDatabaseTable,
    MemberDatabaseTable memberDatabaseTable, Translation translation,
    @Qualifier("supportMail") Mail supportMail,
    DulnoEnvironment environment
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.ticketDatabaseTable = ticketDatabaseTable;
    this.ticketMessageDatabaseTable = ticketMessageDatabaseTable;
    this.memberDatabaseTable = memberDatabaseTable;
    this.translation = translation;
    this.supportMail = supportMail;
    this.environment = environment;
  }

  @RequestMapping(path = "/ticket/answer/", method = RequestMethod.POST)
  public void answerTicket(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var ticketId = body.getUUID("ticket");
    findStaff(request).thenAccept(staff ->
      staffHasPermission(staff.id(), "ticket.answer")
        .thenAccept(hasPermission -> ticketDatabaseTable.ticketExists(ticketId)
          .thenAccept(exists -> answerTicket(staff, ticketId,
            body.getString("message"), hasPermission, exists))));
  }

  private void answerTicket(
    Staff staff, UUID ticketId, String message, boolean hasPermission,
    boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    ticketDatabaseTable.findTicket(ticketId).thenAccept(ticket ->
      ticketMessageDatabaseTable.generateAvailableTicketMessageId()
        .thenAccept(id -> memberDatabaseTable.findMember(ticket.creator())
          .thenAccept(creator -> answerTicket(staff, ticket,
            message, id, creator))));
  }

  private static final String TICKET_URL_FORMAT = "https://%s/ticket/%s/";

  private void answerTicket(
    Staff staff, Ticket ticket, String message, UUID messageId,
    Member creator
  ) {
    ticket.addMessage(messageId);
    ticket.resetExpiration();
    ticket.updateLastMessageSeen(false);
    ticketDatabaseTable.deleteTicket(ticket.id())
      .thenAccept(deleteValue -> ticketDatabaseTable.insertTicket(ticket)
        .thenAccept(insertValue -> ticketMessageDatabaseTable
          .insertTicketMessage(TicketMessage.create(messageId, ticket.id(),
            staff.id(), TicketMessageAuthorType.TEAM, message,
            System.currentTimeMillis()))));
    var title = translation.translate(creator, "ticket.answer.email.title");
    var body = String.format(translation.translate(creator, "ticket.answer.email.body"),
      creator.name(), ticket.title(), String.format(TICKET_URL_FORMAT,
        environment.domain(), ticket.id()));
    supportMail.send(creator, title, body);
  }

  @RequestMapping(path = "/ticket/message/delete/", method = RequestMethod.POST)
  public void deleteTicketMessage(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var ticketId = body.getUUID("ticket");
    staffHasPermission(findStaffId(request), "ticket.message.delete")
      .thenAccept(hasPermission -> ticketDatabaseTable.ticketExists(ticketId)
        .thenAccept(exists -> deleteTicketMessage(ticketId,
          body.getUUID("message"), hasPermission, exists)));
  }

  private void deleteTicketMessage(
    UUID ticketId, UUID messageId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    ticketDatabaseTable.findTicket(ticketId).thenAccept(ticket ->
      deleteTicketMessage(ticket, messageId));
  }

  private void deleteTicketMessage(Ticket ticket, UUID messageId) {
    if (!ticket.messages().contains(messageId) || ticket.messages().indexOf(messageId) == 0) {
      return;
    }
    ticketMessageDatabaseTable.deleteTicketMessage(messageId);
    ticketDatabaseTable.removeTicketMessage(ticket.id(), messageId);
  }

  @RequestMapping(path = "/ticket/close/", method = RequestMethod.POST)
  public void closeTicket(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var ticketId = body.getUUID("ticket");
    staffHasPermission(findStaffId(request), "ticket.close")
      .thenAccept(hasPermission -> ticketDatabaseTable.ticketExists(ticketId)
        .thenAccept(exists -> closeTicket(ticketId, hasPermission, exists)));
  }

  private void closeTicket(
    UUID ticketId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    ticketDatabaseTable.updateTicketStatus(ticketId, Ticket.Status.CLOSED);
  }
}
