package com.dulno.team.access.offer;

import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.offer.Offer;
import com.dulno.core.offer.OfferDatabaseTable;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class OfferInformationController extends TeamController {
  private final OfferDatabaseTable offerDatabaseTable;

  private OfferInformationController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, OfferDatabaseTable offerDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.offerDatabaseTable = offerDatabaseTable;
  }

  @RequestMapping(path = "/offers/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findOffers(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    return staffHasPermission(findStaffId(request), "offers.find")
      .thenCompose(hasPermission -> findOffers(partnerId, hasPermission));
  }

  private CompletableFuture<Map<String, Object>> findOffers(
    UUID partnerId, boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return offerDatabaseTable.findOffersByPartner(partnerId)
      .thenApply(offers -> Map.of("offers", offers.stream()
        .map(this::assemblyOfferInformation).toList()));
  }

  @RequestMapping(path = "/offer/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findOffer(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var offerId = body.getUUID("offer");
    return staffHasPermission(findStaffId(request), "offer.find")
      .thenCompose(hasPermission -> offerDatabaseTable.offerExists(offerId)
        .thenCompose(exists -> findOffer(offerId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findOffer(
    UUID offerId, boolean hasPermission, boolean offerExists
  ) {
    if (!hasPermission || !offerExists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return offerDatabaseTable.findOffer(offerId)
      .thenApply(this::assemblyOfferInformation);
  }

  private Map<String, Object> assemblyOfferInformation(Offer offer) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", offer.id().toString());
    information.put("status", offer.offerStatus().toString());
    information.put("type", offer.bundleType().toString());
    information.put("runtime", offer.bundleRuntime().toString());
    information.put("price", offer.price());
    information.put("portalAccess", offer.portalAccess());
    information.put("collectionCardAccess", offer.collectionCardAccess());
    information.put("valueCardAccess", offer.valueCardAccess());
    information.put("memberCardAccess", offer.memberCardAccess());
    information.put("analysisAccess", offer.analysisAccess());
    information.put("balancesAccess", offer.balancesAccess());
    information.put("campaignAccess", offer.campaignAccess());
    information.put("notificationAccess", offer.notificationAccess());
    return information;
  }
}
