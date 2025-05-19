package com.dulno.team.access.member;

import com.dulno.core.partner.member.PartnerMemberDatabaseTable;
import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.core.member.Member;
import com.dulno.core.member.MemberDatabaseTable;
import com.dulno.core.partner.Partner;
import com.dulno.core.partner.PartnerDatabaseTable;
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
public final class MemberGeneralController extends TeamController {
  private final MemberDatabaseTable memberDatabaseTable;
  private final PartnerMemberDatabaseTable partnerMemberDatabaseTable;
  private final PartnerDatabaseTable partnerDatabaseTable;
  private final TicketDatabaseTable ticketDatabaseTable;

  private MemberGeneralController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, MemberDatabaseTable memberDatabaseTable,
    PartnerMemberDatabaseTable partnerMemberDatabaseTable,
    PartnerDatabaseTable partnerDatabaseTable,
    TicketDatabaseTable ticketDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.memberDatabaseTable = memberDatabaseTable;
    this.partnerMemberDatabaseTable = partnerMemberDatabaseTable;
    this.partnerDatabaseTable = partnerDatabaseTable;
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
      .thenCompose(member -> partnerMemberDatabaseTable.findMemberPartners(memberId)
        .thenCompose(partnerIds -> AsyncIterator.execute(partnerIds,
            partnerDatabaseTable::findPartner)
          .thenCompose(partners -> ticketDatabaseTable.findAllOpenTicketsOfCreator(memberId)
            .thenApply(tickets -> assemblyMemberInformation(member, partners, tickets)))));
  }

  private Map<String, Object> assemblyMemberInformation(
    Member member, List<Partner> partners, List<Ticket> tickets
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("found", true);
    information.put("id", member.id());
    information.put("email", member.email());
    information.put("name", member.name());
    information.put("language", member.language());
    information.put("joinDate", member.accession());
    var partnersInformation = Lists.<Map<String, Object>>newArrayList();
    for (var partner : partners) {
      var partnerInformation = Maps.<String, Object>newHashMap();
      partnerInformation.put("id", partner.id());
      partnerInformation.put("name", partner.name());
      partnersInformation.add(partnerInformation);
    }
    information.put("partners", partnersInformation);
    var ticketsInformation = Lists.<Map<String, Object>>newArrayList();
    for (var ticket : tickets) {
      var ticketInformation = Maps.<String, Object>newHashMap();
      ticketInformation.put("id", ticket.id());
      ticketInformation.put("name", ticket.title());
      ticketInformation.put("type", ticket.type());
      ticketInformation.put("status", ticket.status());
      ticketsInformation.add(ticketInformation);
    }
    information.put("tickets", ticketsInformation);
    return information;
  }
}