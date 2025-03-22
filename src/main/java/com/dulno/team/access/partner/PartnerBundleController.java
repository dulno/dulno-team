package com.dulno.team.access.partner;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.bundle.*;
import com.dulno.core.stripe.TerminationDatabaseTable;
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
import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class PartnerBundleController extends TeamController {
  private final BundleDatabaseTable bundleDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;
  private final TerminationDatabaseTable terminationDatabaseTable;

  private PartnerBundleController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable,
    BundlePresetRepository bundlePresetRepository,
    TerminationDatabaseTable terminationDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.bundleDatabaseTable = bundleDatabaseTable;
    this.bundlePresetRepository = bundlePresetRepository;
    this.terminationDatabaseTable = terminationDatabaseTable;
  }

  @RequestMapping(path = "/partner/bundle/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findPartnerBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    return staffHasPermission(findStaffId(request), "partner.bundle.find")
      .thenCompose(hasPermission -> bundleDatabaseTable.bundleExists(partnerId)
        .thenCompose(exists -> findPartnerBundle(partnerId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findPartnerBundle(
    UUID partnerId, boolean hasPermission, boolean bundleExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!bundleExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return bundleDatabaseTable.findBundle(partnerId)
      .thenApply(this::assemblyBundleInformation);
  }

  private Map<String, Object> assemblyBundleInformation(Bundle bundle) {
    var information = Maps.<String, Object>newHashMap();
    information.put("found", true);
    information.put("type", bundle.bundleType().toString());
    information.put("class", bundle.bundleClass().toString());
    information.put("runtime", bundle.bundleRuntime().toString());
    information.put("price", bundle.price());
    information.put("expiration", bundle.expiration());
    information.put("workflowAccess", bundle.workflowAccess());
    information.put("workflowNumberLimit", bundle.workflowNumberLimit());
    information.put("workflowOperationLimit", bundle.workflowOperationLimit());
    information.put("workflowTemplateAccess", bundle.workflowTemplateAccess());
    information.put("processAccess", bundle.processAccess());
    information.put("processNumberLimit", bundle.processNumberLimit());
    information.put("databaseAccess", bundle.databaseAccess());
    information.put("databaseNumberLimit", bundle.databaseNumberLimit());
    information.put("databaseDataLimit", new DecimalFormat("#.#").format(
      bundle.databaseDataLimit()));
    information.put("webhookAccess", bundle.webhookAccess());
    information.put("webhookNumberLimit", bundle.webhookNumberLimit());
    information.put("organizationAccess", bundle.organizationAccess());
    information.put("organizationMemberLimit", bundle.organizationMemberLimit());
    information.put("organizationTeamLimit", bundle.organizationTeamLimit());
    information.put("deviceAccess", bundle.deviceAccess());
    information.put("accountsAccess", bundle.accountsAccess());
    information.put("accountsNumberLimit", bundle.accountsNumberLimit());
    return information;
  }

  @RequestMapping(path = "/partner/bundle/change/", method = RequestMethod.POST)
  public void changePartnerBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    staffHasPermission(findStaffId(request), "partner.bundle.change")
      .thenAccept(hasPermission -> changePartnerBundle(partnerId, body,
        hasPermission));
  }

  private void changePartnerBundle(
    UUID partnerId, DulnoRequestBody body, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    var preset = findBundlePreset(body);
    var runtime = BundleRuntime.valueOf(body.getString("runtime"));
    var expiration = runtime.isUnbound() ? System.currentTimeMillis() +
      body.getLong("expiration") : -1;
    var price = findBundlePrice(body, preset, runtime);
    var workflowNumberLimit = findBundleWorkflowNumberLimit(body, preset);
    var workflowOperationLimit = findBundleWorkflowOperationLimit(body, preset);
    var processNumberLimit = findBundleProcessNumberLimit(body, preset);
    var databaseNumberLimit = findBundleDatabaseNumberLimit(body, preset);
    var databaseDataLimit = findBundleDatabaseDataLimit(body, preset);
    var isOrganization = preset.bundleType().isTeam() ||
      preset.bundleType().isEnterprise();
    var organizationMemberLimit = isOrganization ?
      findBundleOrganizationMemberLimit(body, preset) : 0;
    var organizationTeamLimit = isOrganization ?
      findBundleOrganizationTeamLimit(body, preset) : 0;
    bundleDatabaseTable.deleteBundle(partnerId).thenAccept(value ->
      bundleDatabaseTable.insertBundle(Bundle.of(partnerId,
        preset, runtime, expiration, price, workflowNumberLimit,
        workflowOperationLimit, processNumberLimit, databaseNumberLimit,
        databaseDataLimit, organizationMemberLimit, organizationTeamLimit)));
    terminationDatabaseTable.deleteTermination(partnerId);
  }

  private double findBundlePrice(
    DulnoRequestBody body, BundlePreset preset, BundleRuntime runtime
  ) {
    if (runtime.isInfinite()) {
      return 0;
    }
    if (!body.has("price")) {
      return preset.hasPrice() ? (runtime.isMonthly() ?
        preset.monthlyPrice() : preset.yearlyPrice()) : 0;
    }
    return body.getDouble("price");
  }

  private long findBundleWorkflowNumberLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("workflowNumberLimit")) {
      return preset.workflowNumberLimit();
    }
    return body.getLong("workflowNumberLimit");
  }

  private long findBundleWorkflowOperationLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("workflowOperationLimit")) {
      return preset.hasWorkflowOperationLimit() ?
        preset.workflowOperationLimit() : 0;
    }
    return body.getLong("workflowOperationLimit");
  }

  private long findBundleProcessNumberLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("processNumberLimit")) {
      return preset.processNumberLimit();
    }
    return body.getLong("processNumberLimit");
  }

  private long findBundleDatabaseNumberLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("databaseNumberLimit")) {
      return preset.databaseNumberLimit();
    }
    return body.getLong("databaseNumberLimit");
  }

  private double findBundleDatabaseDataLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("databaseDataLimit")) {
      return preset.databaseDataLimit();
    }
    return body.getDouble("databaseDataLimit");
  }

  private long findBundleOrganizationMemberLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("organizationMemberLimit")) {
      return preset.organizationMemberLimit();
    }
    return body.getLong("organizationMemberLimit");
  }

  private long findBundleOrganizationTeamLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("organizationTeamLimit")) {
      return preset.organizationTeamLimit();
    }
    return body.getLong("organizationTeamLimit");
  }

  private BundlePreset findBundlePreset(DulnoRequestBody body) {
    var bundleType = BundleType.valueOf(body.getString("type"));
    if (bundleType.isTrial() || bundleType.isEnterprise()) {
      return bundlePresetRepository.findPreset(bundleType).get();
    }
    var bundleClass = BundleClass.valueOf(body.getString("class"));
    return bundlePresetRepository.findPreset(bundleType, bundleClass).get();
  }

  @RequestMapping(path = "/partner/bundle/delete/", method = RequestMethod.POST)
  public void deletePartnerBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    staffHasPermission(findStaffId(request), "partner.bundle.delete")
      .thenAccept(hasPermission -> bundleDatabaseTable.bundleExists(partnerId)
        .thenAccept(exists -> deletePartnerBundle(partnerId, hasPermission, exists)));
  }

  private void deletePartnerBundle(
    UUID partnerId, boolean hasPermission, boolean bundleExists
  ) {
    if (!hasPermission || !bundleExists) {
      return;
    }
    bundleDatabaseTable.deleteBundle(partnerId);
  }
}