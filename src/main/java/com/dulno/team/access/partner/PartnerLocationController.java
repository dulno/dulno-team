package com.dulno.team.access.partner;

import com.dulno.core.partner.Partner;
import com.dulno.core.partner.PartnerDatabaseTable;
import com.dulno.core.partner.location.PartnerLocation;
import com.dulno.core.partner.location.PartnerLocationDatabaseTable;
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
public final class PartnerLocationController extends TeamController {
  private final PartnerDatabaseTable partnerDatabaseTable;
  private final PartnerLocationDatabaseTable partnerLocationDatabaseTable;

  private PartnerLocationController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    PartnerDatabaseTable partnerDatabaseTable,
    PartnerLocationDatabaseTable partnerLocationDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.partnerDatabaseTable = partnerDatabaseTable;
    this.partnerLocationDatabaseTable = partnerLocationDatabaseTable;
  }

  @RequestMapping(path = "/partner/locations/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findPartner(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    return staffHasPermission(findStaffId(request), "partner.locations.find")
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
    return partnerLocationDatabaseTable.findPartnerLocations(partner.id())
      .thenApply(this::assemblyPartnerLocationInformation);
  }

  private Map<String, Object> assemblyPartnerLocationInformation(
    List<PartnerLocation> locations
  ) {
    var locationsInformation = Lists.<Map<String, Object>>newArrayList();
    for (var location : locations) {
      var locationInformation = Maps.<String, Object>newHashMap();
      locationInformation.put("address", location.address());
      locationInformation.put("latitude", location.latitude());
      locationInformation.put("longitude", location.longitude());
      locationInformation.put("creation", location.creation());
      locationsInformation.add(locationInformation);
    }
    return Map.of("locations", locationsInformation);
  }
}