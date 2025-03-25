package com.dulno.team.access.member;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.member.Member;
import com.dulno.core.member.MemberDatabaseTable;
import com.dulno.core.ticket.Ticket;
import com.dulno.core.ticket.TicketDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.StaffDatabaseTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class MemberInformationController extends TeamController {
  private final MemberDatabaseTable memberDatabaseTable;
  private final TicketDatabaseTable ticketDatabaseTable;

  private MemberInformationController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, MemberDatabaseTable memberDatabaseTable,
    TicketDatabaseTable ticketDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.memberDatabaseTable = memberDatabaseTable;
    this.ticketDatabaseTable = ticketDatabaseTable;
  }

  @RequestMapping(path = "/member/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findMember(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var memberId = body.getUUID("member");
    return staffHasPermission(findStaffId(request), "member.find")
      .thenCompose(hasPermission -> memberDatabaseTable.memberExists(memberId)
        .thenCompose(exists -> findMember(memberId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findMember(
    UUID memberId, boolean hasPermission, boolean memberExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!memberExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return memberDatabaseTable.findMember(memberId)
      .thenCompose(member -> ticketDatabaseTable.findAllOpenTicketsOfCreator(memberId)
        .thenApply(tickets -> assemblyMemberInformation(member, tickets)));
  }

  private Map<String, Object> assemblyMemberInformation(
    Member member, List<Ticket> tickets
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("found", true);
    information.put("id", member.id());
    information.put("email", member.email());
    information.put("name", member.name());
    information.put("language", member.language());
    information.put("joinDate", member.accession());
    var ticketInformation = Lists.<Map<String, Object>>newArrayList();
    for (var ticket : tickets) {
      ticketInformation.add(Map.of("id", ticket.id(),
        "name", ticket.title(), "type", ticket.type(), "status", ticket.status()));
    }
    information.put("tickets", ticketInformation);
    return information;
  }
}