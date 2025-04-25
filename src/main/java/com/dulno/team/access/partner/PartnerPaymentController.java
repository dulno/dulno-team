package com.dulno.team.access.partner;

import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.error.ErrorRepository;
import com.dulno.core.stripe.StripeDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.StaffDatabaseTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.stripe.StripeClient;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentListParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class PartnerPaymentController extends TeamController {
  private final StripeDatabaseTable stripeDatabaseTable;
  private final StripeClient stripeClient;
  private final ErrorRepository errorRepository;

  private PartnerPaymentController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, StripeDatabaseTable stripeDatabaseTable,
    StripeClient stripeClient, ErrorRepository errorRepository
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.stripeDatabaseTable = stripeDatabaseTable;
    this.stripeClient = stripeClient;
    this.errorRepository = errorRepository;
  }

  @RequestMapping(path = "/partner/payments/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findPartnerPayments(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    return staffHasPermission(findStaffId(request), "partner.payments.find")
      .thenCompose(hasPermission ->
        stripeDatabaseTable.stripeAccountExistsByPartner(partnerId)
          .thenComposeAsync(stripeExists -> findPartnerPayments(partnerId,
            hasPermission, stripeExists)));
  }

  private CompletableFuture<Map<String, Object>> findPartnerPayments(
    UUID target, boolean hasPermission, boolean stripeAccountExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!stripeAccountExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return stripeDatabaseTable.findStripeAccountByPartner(target)
      .thenApplyAsync(account -> findPayments(account.accountId()));
  }

  private Map<String, Object> findPayments(String accountId) {
    try {
      var parameters = PaymentIntentListParams.builder()
        .setCustomer(accountId).build();
      var payments = stripeClient.paymentIntents().list(parameters);
      var information = Lists.newArrayList();
      for (var payment : payments.getData()) {
        var invoice = stripeClient.invoices().retrieve(payment.getInvoice());
        information.add(assemblyPaymentInformation(payment, invoice));
      }
      return Map.of("payments", information);
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return Maps.newHashMap();
    }
  }

  private Map<String, Object> assemblyPaymentInformation(
    PaymentIntent payment, Invoice invoice
  ) throws Exception {
    var information = Maps.<String, Object>newHashMap();
    information.put("time", formatTime(payment.getCreated() * 1000));
    information.put("amount", payment.getAmount() / 100D);
    information.put("method", stripeClient.paymentMethods()
      .retrieve(payment.getPaymentMethod()).getType());
    information.put("status", payment.getStatus());
    information.put("invoice", invoice.getInvoicePdf());
    return information;
  }

  private String formatTime(long time) {
    var calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    return new SimpleDateFormat("dd.MM.yyyy").format(calendar.getTime());
  }
}