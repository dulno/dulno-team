package com.dulno.team.access.partner;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.bundle.Bundle;
import com.dulno.core.bundle.BundleDatabaseTable;
import com.dulno.core.error.ErrorRepository;
import com.dulno.core.stripe.StripeAccount;
import com.dulno.core.stripe.StripeDatabaseTable;
import com.dulno.core.stripe.TerminationDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.StaffDatabaseTable;
import com.google.common.collect.Maps;
import com.stripe.StripeClient;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionListParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class PartnerTerminationController extends TeamController {
  private final BundleDatabaseTable bundleDatabaseTable;
  private final TerminationDatabaseTable terminationDatabaseTable;
  private final StripeDatabaseTable stripeDatabaseTable;
  private final StripeClient stripeClient;
  private final ErrorRepository errorRepository;

  private PartnerTerminationController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable,
    TerminationDatabaseTable terminationDatabaseTable,
    StripeDatabaseTable stripeDatabaseTable, StripeClient stripeClient,
    ErrorRepository errorRepository
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.bundleDatabaseTable = bundleDatabaseTable;
    this.terminationDatabaseTable = terminationDatabaseTable;
    this.stripeDatabaseTable = stripeDatabaseTable;
    this.stripeClient = stripeClient;
    this.errorRepository = errorRepository;
  }

  @RequestMapping(path = "/partner/termination/status/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findPartnerTerminationStatus(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    return staffHasPermission(findStaffId(request), "partner.termination.status")
      .thenCompose(hasPermission -> bundleDatabaseTable.bundleExists(partnerId)
        .thenCompose(bundleExists ->
          stripeDatabaseTable.stripeAccountExistsByPartner(partnerId)
            .thenCompose(stripeExists -> findPartnerTerminationStatus(
              partnerId, hasPermission, bundleExists, stripeExists))));
  }

  private CompletableFuture<Map<String, Object>> findPartnerTerminationStatus(
    UUID partnerId, boolean hasPermission, boolean bundleExists,
    boolean stripeAccountExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!bundleExists || !stripeAccountExists) {
      return CompletableFuture.completedFuture(Map.of("terminable", false));
    }
    return bundleDatabaseTable.findBundle(partnerId)
      .thenCompose(bundle -> stripeDatabaseTable.findStripeAccountByPartner(partnerId)
        .thenCompose(stripeAccount -> findPartnerTerminationStatus(partnerId, bundle,
          stripeAccount.accountId())));
  }

  private CompletableFuture<Map<String, Object>> findPartnerTerminationStatus(
    UUID partnerId, Bundle bundle, String stripeAccountId
  ) {
    if (bundle.bundleRuntime().isUnbound()) {
      return CompletableFuture.completedFuture(Map.of("terminable", false,
        "terminated", false));
    }
    try {
      var subscriptions = stripeClient.subscriptions()
        .list(SubscriptionListParams.builder().setCustomer(stripeAccountId).build())
        .getData();
      if (subscriptions.isEmpty()) {
        return CompletableFuture.completedFuture(Map.of("terminable", true,
          "terminated", true));
      }
      return terminationDatabaseTable.terminationExists(partnerId)
        .thenApply(exists -> findPartnerTerminationStatus(bundle, exists));
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
  }

  private Map<String, Object> findPartnerTerminationStatus(
    Bundle bundle, boolean terminationExists
  ) {
    if (!terminationExists) {
      return Map.of("terminable", true, "terminated", false, "directlyEffective",
        isDirectlyTerminable(bundle));
    }
    return Map.of("terminable", true, "terminated", true);
  }

  @RequestMapping(path = "/partner/bundle/terminate/", method = RequestMethod.POST)
  public void terminatePartnerBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    staffHasPermission(findStaffId(request), "partner.bundle.terminate")
      .thenAccept(hasPermission -> bundleDatabaseTable.bundleExists(partnerId)
        .thenAccept(bundleExists ->
          stripeDatabaseTable.stripeAccountExistsByPartner(partnerId)
            .thenAccept(stripeExists -> terminatePartnerBundle(partnerId,
              hasPermission, bundleExists, stripeExists))));
  }

  private void terminatePartnerBundle(
    UUID partnerId, boolean hasPermission, boolean bundleExists,
    boolean stripeAccountExists
  ) {
    if (!hasPermission || !bundleExists || !stripeAccountExists) {
      return;
    }
    bundleDatabaseTable.findBundle(partnerId)
      .thenAccept(bundle -> stripeDatabaseTable.findStripeAccountByPartner(partnerId)
        .thenAccept(stripeAccount -> terminatePartnerBundle(partnerId, bundle,
          stripeAccount)));
  }

  private void terminatePartnerBundle(
    UUID partnerId, Bundle bundle, StripeAccount account
  ) {
    if (bundle.bundleRuntime().isMonthly()) {
      cancelSubscription(account);
    } else if (bundle.bundleRuntime().isYearly()) {
      terminateYearly(partnerId, bundle, account);
    }
  }

  private void terminateYearly(UUID partnerId, Bundle bundle, StripeAccount account) {
    if (isDirectlyTerminable(bundle)) {
      cancelSubscription(account);
      return;
    }
    terminationDatabaseTable.insertTermination(partnerId);
  }

  private CompletableFuture<Void> cancelSubscription(StripeAccount account) {
    try {
      var subscriptions = stripeClient.subscriptions()
        .list(SubscriptionListParams.builder().setCustomer(account.accountId()).build())
        .getData();
      if (subscriptions.isEmpty()) {
        return CompletableFuture.completedFuture(null);
      }
      return cancelSubscription(subscriptions.get(0), account);
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return CompletableFuture.completedFuture(null);
    }
  }

  private CompletableFuture<Void> cancelSubscription(
    Subscription subscription, StripeAccount account
  ) {
    try {
      subscription.cancel();
      return stripeDatabaseTable.updateStripeAccount(account.accountId(),
        account.partnerId(), account.memberId(), "");
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return CompletableFuture.completedFuture(null);
    }
  }

  private boolean isDirectlyTerminable(Bundle bundle) {
    var current = ZonedDateTime.ofInstant(Instant.ofEpochMilli(bundle.expiration()),
      ZoneId.systemDefault());
    var next = current.minusMonths(1);
    if (next.getDayOfMonth() != current.getDayOfMonth()) {
      next = next.withDayOfMonth(next.getMonth().length(
        next.toLocalDate().isLeapYear()));
    }
    return System.currentTimeMillis() > next.toInstant().toEpochMilli();
  }
}