package com.dulno.team.access.partner;

import com.dulno.core.partner.*;
import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.core.member.Member;
import com.dulno.core.member.MemberDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.StaffDatabaseTable;
import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class PartnerInformationController extends TeamController {
  private final PartnerDatabaseTable partnerDatabaseTable;
  private final PartnerMemberDatabaseTable partnerMemberDatabaseTable;
  private final PartnerLogoDatabaseTable partnerLogoDatabaseTable;
  private final MemberDatabaseTable memberDatabaseTable;

  private PartnerInformationController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    PartnerDatabaseTable partnerDatabaseTable,
    PartnerMemberDatabaseTable partnerMemberDatabaseTable,
    PartnerLogoDatabaseTable partnerLogoDatabaseTable,
    MemberDatabaseTable memberDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.partnerDatabaseTable = partnerDatabaseTable;
    this.partnerMemberDatabaseTable = partnerMemberDatabaseTable;
    this.partnerLogoDatabaseTable = partnerLogoDatabaseTable;
    this.memberDatabaseTable = memberDatabaseTable;
  }

  @RequestMapping(path = "/partners/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> listPartners(
    HttpServletRequest request
  ) {
    return staffHasPermission(findStaffId(request), "partners.find")
      .thenCompose(this::listPartners);
  }

  private CompletableFuture<Map<String, Object>> listPartners(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return partnerDatabaseTable.findAllPartners()
      .thenCompose(partnerIds -> AsyncIterator.execute(partnerIds,
          this::collectPartnerInformation)
        .thenApply(information -> Map.of("partners", information)));
  }

  @RequestMapping(path = "/partner/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findPartner(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    return staffHasPermission(findStaffId(request), "partner.find")
      .thenCompose(hasPermission -> partnerDatabaseTable.partnerExists(partnerId)
        .thenCompose(exists -> findPartner(partnerId, hasPermission, exists)));
  }

  public CompletableFuture<Map<String, Object>> findPartner(
    UUID partnerId, boolean hasPermission, boolean partnerExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!partnerExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return partnerDatabaseTable.findPartner(partnerId)
      .thenCompose(this::collectPartnerInformation);
  }

  private CompletableFuture<Map<String, Object>> collectPartnerInformation(
    Partner partner
  ) {
    return partnerLogoDatabaseTable.findPartnerLogo(partner.id())
      .thenCompose(logo -> partnerMemberDatabaseTable.findPartnerMembers(partner.id())
        .thenCompose(memberIds ->
          AsyncIterator.execute(memberIds, memberDatabaseTable::findMember)
            .thenApply(members -> assemblyPartnerInformation(partner, logo, members))));
  }

  private Map<String, Object> assemblyPartnerInformation(
    Partner partner, PartnerLogo logo, List<Member> members
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("found", true);
    information.put("id", partner.id());
    information.put("name", partner.name());
    information.put("description", partner.description());
    information.put("accession", partner.accession());
    information.put("logoId", logo.logoId());
    information.put("logo", Base64.getEncoder().encodeToString(logo.content()));
    var membersInformation = Lists.<Map<String, Object>>newArrayList();
    for (var member : members) {
      var memberInformation = Maps.<String, Object>newHashMap();
      memberInformation.put("id", member.id());
      memberInformation.put("name", member.name());
      membersInformation.add(memberInformation);
    }
    information.put("members", membersInformation);
    return information;
  }
}