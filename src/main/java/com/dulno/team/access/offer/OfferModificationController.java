package com.dulno.team.access.offer;

import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.bundle.*;
import com.dulno.core.environment.DulnoEnvironment;
import com.dulno.core.error.ErrorRepository;
import com.dulno.core.locale.Translation;
import com.dulno.core.mail.Mail;
import com.dulno.core.member.Member;
import com.dulno.core.member.MemberDatabaseTable;
import com.dulno.core.offer.Offer;
import com.dulno.core.offer.OfferDatabaseTable;
import com.dulno.core.offer.OfferStatus;
import com.dulno.core.partner.PartnerMemberDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.StaffDatabaseTable;
import com.google.common.collect.Maps;
import com.stripe.StripeClient;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceUpdateParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class OfferModificationController extends TeamController {
  private final OfferDatabaseTable offerDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;
  private final StripeClient stripeClient;
  private final MemberDatabaseTable memberDatabaseTable;
  private final PartnerMemberDatabaseTable partnerMemberDatabaseTable;
  private final Mail saleMail;
  private final Translation translation;
  private final ErrorRepository errorRepository;
  private final DulnoEnvironment environment;

  private OfferModificationController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    OfferDatabaseTable offerDatabaseTable,
    BundlePresetRepository bundlePresetRepository, StripeClient stripeClient,
    MemberDatabaseTable memberDatabaseTable,
    PartnerMemberDatabaseTable partnerMemberDatabaseTable,
    @Qualifier("saleMail") Mail saleMail, Translation translation,
    ErrorRepository errorRepository, DulnoEnvironment environment
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.offerDatabaseTable = offerDatabaseTable;
    this.bundlePresetRepository = bundlePresetRepository;
    this.stripeClient = stripeClient;
    this.memberDatabaseTable = memberDatabaseTable;
    this.partnerMemberDatabaseTable = partnerMemberDatabaseTable;
    this.saleMail = saleMail;
    this.translation = translation;
    this.errorRepository = errorRepository;
    this.environment = environment;
  }

  @RequestMapping(path = "/offer/price/suggestion/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> createOfferPriceSuggestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    return staffHasPermission(findStaffId(request), "offer.price.suggestion")
      .thenApply(hasPermission -> createOfferPriceSuggestion(body, hasPermission));
  }

  private Map<String, Object> createOfferPriceSuggestion(
    DulnoRequestBody body, boolean hasPermission
  ) {
    if (!hasPermission) {
      return Maps.newHashMap();
    }
    var preset = findBundlePreset(body);
    var runtime = BundleRuntime.valueOf(body.getString("runtime"));
    var price = preset.hasPrice() ? (runtime.isMonthly() ?
      preset.monthlyPrice() : preset.yearlyPrice()) : 0;
    return Map.of("price", price);
  }

  @RequestMapping(path = "/offer/create/", method = RequestMethod.POST)
  public void createOffer(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    staffHasPermission(findStaffId(request), "offer.create")
      .thenAcceptAsync(hasPermission -> createOffer(partnerId, body, hasPermission));
  }

  private void createOffer(
    UUID partnerId, DulnoRequestBody body, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    var preset = findBundlePreset(body);
    var runtime = BundleRuntime.valueOf(body.getString("runtime"));
    var price = Math.round(body.getDouble("price") * 100.0) / 100.0;
    var priceId = createStripeOffer(price);
    offerDatabaseTable.generateAvailableOfferId()
      .thenAccept(id -> createOffer(id, partnerId, priceId, preset, runtime,
        price));
  }

  private void createOffer(
    UUID id, UUID partnerId, String priceId, BundlePreset preset,
    BundleRuntime runtime, double price
  ) {
    offerDatabaseTable.insertOffer(Offer.of(id, partnerId,
      priceId, OfferStatus.PENDING, preset, runtime, price));
    partnerMemberDatabaseTable.findPartnerMembers(partnerId)
      .thenAccept(memberIds -> memberIds.stream()
        .forEach(memberId -> memberDatabaseTable.findMember(memberId)
          .thenAccept(member -> sendOfferEmail(member, id))));
  }

  private static final String OFFER_URL = "https://%s/offer/%s/";

  private void sendOfferEmail(Member member, UUID offerId) {
    var title = translation.translateMember(member, "offer.email.title");
    var body = String.format(translation.translateMember(member, "offer.email.body"),
      member.name(), String.format(OFFER_URL, environment.domain(),
        offerId.toString()));
    saleMail.send(member, title, body);
  }

  private String createStripeOffer(double price) {
    try {
      var product = PriceCreateParams.ProductData.builder()
        .setName("Dulno")
        .build();
      var recurring = PriceCreateParams.Recurring.builder()
        .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
        .build();
      var parameters = PriceCreateParams.builder()
        .setUnitAmount(Math.round(price * 100))
        .setRecurring(recurring)
        .setCurrency("eur")
        .setProductData(product)
        .build();
      var stripePrice = stripeClient.prices().create(parameters);
      return stripePrice.getId();
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return null;
    }
  }

  private BundlePreset findBundlePreset(DulnoRequestBody body) {
    return bundlePresetRepository.findPreset(
      BundleType.valueOf(body.getString("type"))).get();
  }

  @RequestMapping(path = "/offer/withdraw/", method = RequestMethod.POST)
  public void withdrawOffer(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var offerId = body.getUUID("offer");
    staffHasPermission(findStaffId(request), "offer.withdraw")
      .thenAccept(hasPermission -> offerDatabaseTable.offerExists(offerId)
        .thenAccept(exists -> withdrawOffer(offerId, hasPermission, exists)));
  }

  private void withdrawOffer(
    UUID offerId, boolean hasPermission, boolean offerExists
  ) {
    if (!hasPermission || !offerExists) {
      return;
    }
    offerDatabaseTable.findOffer(offerId).thenAccept(this::withdrawOffer);
  }

  private void withdrawOffer(Offer offer) {
    offerDatabaseTable.updateOfferStatus(offer, OfferStatus.WITHDRAWN);
    try {
      stripeClient.prices().update(offer.priceId(),
        PriceUpdateParams.builder().setActive(false).build());
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }
}
