package com.dulno.team.access.partner;

import com.dulno.core.request.DulnoRequestBody;
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
    information.put("runtime", bundle.bundleRuntime().toString());
    information.put("price", bundle.price());
    information.put("expiration", bundle.expiration());
    information.put("collectionCardAccess", bundle.collectionCardAccess());
    information.put("valueCardAccess", bundle.valueCardAccess());
    information.put("memberCardAccess", bundle.memberCardAccess());
    information.put("analysisAccess", bundle.analysisAccess());
    information.put("campaignAccess", bundle.campaignAccess());
    information.put("notificationAccess", bundle.notificationAccess());
    information.put("portalAccess", bundle.portalAccess());
    information.put("multiPortalUserAccess", bundle.multiPortalUserAccess());
    information.put("portalPermissionAccess", bundle.portalPermissionAccess());
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
    bundleDatabaseTable.deleteBundle(partnerId)
      .thenAccept(value -> bundleDatabaseTable.insertBundle(Bundle.of(partnerId,
        preset, runtime, price, expiration)));
    terminationDatabaseTable.deleteTermination(partnerId);
  }

  private double findBundlePrice(
    DulnoRequestBody body, BundlePreset preset, BundleRuntime runtime
  ) {
    if (!body.has("price")) {
      return preset.hasPrice() ? (runtime.isMonthly() ?
        preset.monthlyPrice() : preset.yearlyPrice()) : 0;
    }
    return body.getDouble("price");
  }

  private BundlePreset findBundlePreset(DulnoRequestBody body) {
    return bundlePresetRepository.findPreset(
      BundleType.valueOf(body.getString("type"))).get();
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