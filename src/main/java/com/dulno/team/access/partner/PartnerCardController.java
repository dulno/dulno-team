package com.dulno.team.access.partner;

import com.dulno.core.card.Card;
import com.dulno.core.card.CardDatabaseTable;
import com.dulno.core.card.icon.CardMemberIcon;
import com.dulno.core.card.icon.CardStampIcon;
import com.dulno.core.card.logo.CardLogo;
import com.dulno.core.card.logo.CardLogoDatabaseTable;
import com.dulno.core.iterator.AsyncIterator;
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
import org.json.JSONObject;
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
public final class PartnerCardController extends TeamController {
  private final PartnerDatabaseTable partnerDatabaseTable;
  private final CardDatabaseTable cardDatabaseTable;
  private final CardLogoDatabaseTable cardLogoDatabaseTable;

  private PartnerCardController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    PartnerDatabaseTable partnerDatabaseTable,
    CardDatabaseTable cardDatabaseTable,
    CardLogoDatabaseTable cardLogoDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.partnerDatabaseTable = partnerDatabaseTable;
    this.cardDatabaseTable = cardDatabaseTable;
    this.cardLogoDatabaseTable = cardLogoDatabaseTable;
  }

  @RequestMapping(path = "/partner/cards/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findPartnerCards(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    return staffHasPermission(findStaffId(request), "partner.cards.find")
      .thenCompose(hasPermission -> partnerDatabaseTable.partnerExists(partnerId)
        .thenCompose(exists -> findPartnerCards(partnerId, hasPermission, exists)));
  }

  public CompletableFuture<Map<String, Object>> findPartnerCards(
    UUID partnerId, boolean hasPermission, boolean partnerExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!partnerExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return partnerDatabaseTable.findPartner(partnerId)
      .thenCompose(this::collectPartnerCardsInformation);
  }

  private CompletableFuture<Map<String, Object>> collectPartnerCardsInformation(
    Partner partner
  ) {
    return cardDatabaseTable.findCardsOfPartner(partner.id())
      .thenCompose(cards -> AsyncIterator.execute(cards,
          this::gatherCardInformation)
        .thenApply(information -> Map.of("cards", information)));
  }

  private CompletableFuture<Map<String, Object>> gatherCardInformation(Card card) {
    return cardLogoDatabaseTable.findCardLogo(card.id())
      .thenApply(logo -> assemblyCardInformation(card, logo));
  }

  private Map<String, Object> assemblyCardInformation(Card card, CardLogo logo) {
    var information = Maps.<String, Object>newHashMap();
    information.put("success", true);
    information.put("id", card.id());
    information.put("backgroundColor", card.backgroundColor());
    information.put("foregroundColor", card.foregroundColor());
    information.put("logoId", logo.logoId());
    information.put("logo", Base64.getEncoder().encodeToString(logo.content()));
    information.put("type", card.type().toString());
    information.put("creation", card.creation());
    var content = new JSONObject(card.content());
    if (card.type().isCollection()) {
      information.putAll(assemblyCollectionCardInformation(content));
    } else if (card.type().isValue()) {
      information.putAll(assemblyValueCardInformation(content));
    } else if (card.type().isMember()) {
      information.putAll(assemblyMemberCardInformation(content));
    }
    return information;
  }

  private Map<String, Object> assemblyCollectionCardInformation(JSONObject content) {
    var information = Maps.<String, Object>newHashMap();
    information.put("stampIcon", content.getString("stamp_icon"));
    information.put("stampIconName",
      CardStampIcon.valueOf(content.getString("stamp_icon")).identifier());
    information.put("rewardNumber", content.getInt("reward_number"));
    information.put("rewardDescription", content.getString("reward_description"));
    return information;
  }

  private Map<String, Object> assemblyValueCardInformation(JSONObject content) {
    var information = Maps.<String, Object>newHashMap();
    information.put("stampIcon", content.getString("stamp_icon"));
    information.put("stampIconName",
      CardStampIcon.valueOf(content.getString("stamp_icon")).identifier());
    information.put("valueNumber", content.getInt("value_number"));
    information.put("valuePrice", content.getDouble("value_price"));
    information.put("valueDescription", content.getString("value_description"));
    return information;
  }

  private Map<String, Object> assemblyMemberCardInformation(JSONObject content) {
    var information = Maps.<String, Object>newHashMap();
    information.put("memberIcon", content.getString("member_icon"));
    information.put("memberIconName",
      CardMemberIcon.valueOf(content.getString("member_icon")).identifier());
    information.put("memberPrice", content.getDouble("member_price"));
    information.put("memberDescription", content.getString("member_description"));
    return information;
  }
}