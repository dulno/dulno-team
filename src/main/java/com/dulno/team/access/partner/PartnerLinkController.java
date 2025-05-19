package com.dulno.team.access.partner;

import com.dulno.core.partner.Partner;
import com.dulno.core.partner.PartnerDatabaseTable;
import com.dulno.core.partner.link.PartnerLink;
import com.dulno.core.partner.link.PartnerLinkDatabaseTable;
import com.dulno.core.request.DulnoRequestBody;
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
public final class PartnerLinkController extends TeamController {
  private final PartnerDatabaseTable partnerDatabaseTable;
  private final PartnerLinkDatabaseTable partnerLinkDatabaseTable;

  private PartnerLinkController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    PartnerDatabaseTable partnerDatabaseTable,
    PartnerLinkDatabaseTable partnerLinkDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.partnerDatabaseTable = partnerDatabaseTable;
    this.partnerLinkDatabaseTable = partnerLinkDatabaseTable;
  }

  @RequestMapping(path = "/partner/links/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findPartnerLinks(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    return staffHasPermission(findStaffId(request), "partner.links.find")
      .thenCompose(hasPermission -> partnerDatabaseTable.partnerExists(partnerId)
        .thenCompose(exists -> findPartnerLinks(partnerId, hasPermission, exists)));
  }

  public CompletableFuture<Map<String, Object>> findPartnerLinks(
    UUID partnerId, boolean hasPermission, boolean partnerExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!partnerExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return partnerDatabaseTable.findPartner(partnerId)
      .thenCompose(this::collectPartnerLinksInformation);
  }

  private CompletableFuture<Map<String, Object>> collectPartnerLinksInformation(
    Partner partner
  ) {
    return partnerLinkDatabaseTable.findPartnerLinks(partner.id())
      .thenApply(this::assemblyPartnerLinkInformation);
  }

  private Map<String, Object> assemblyPartnerLinkInformation(
    List<PartnerLink> links
  ) {
    var linksInformation = Lists.<Map<String, Object>>newArrayList();
    for (var link : links) {
      var linkInformation = Maps.<String, Object>newHashMap();
      linkInformation.put("type", link.type().toString());
      linkInformation.put("link", link.link());
      linkInformation.put("creation", link.creation());
      linksInformation.add(linkInformation);
    }
    return Map.of("links", linksInformation);
  }
}