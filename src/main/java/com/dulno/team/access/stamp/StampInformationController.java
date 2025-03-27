package com.dulno.team.access.stamp;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.stamp.Stamp;
import com.dulno.core.stamp.StampDatabaseTable;
import com.dulno.core.stamp.StampSetupDatabaseTable;
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
public final class StampInformationController extends TeamController {
  private final StampDatabaseTable stampDatabaseTable;
  private final StampSetupDatabaseTable stampSetupDatabaseTable;

  private StampInformationController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, StampDatabaseTable stampDatabaseTable,
    StampSetupDatabaseTable stampSetupDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.stampDatabaseTable = stampDatabaseTable;
    this.stampSetupDatabaseTable = stampSetupDatabaseTable;
  }

  @RequestMapping(path = "/stamps/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findStamps(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    return staffHasPermission(findStaffId(request), "stamps.find")
      .thenCompose(hasPermission -> findStamps(partnerId, hasPermission));
  }

  private CompletableFuture<Map<String, Object>> findStamps(
    UUID partnerId, boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return stampDatabaseTable.findStampsOfPartner(partnerId)
      .thenApply(stamps -> Map.of("stamps", stamps.stream()
        .map(this::assemblyStampInformation).toList()));
  }

  private Map<String, Object> assemblyStampInformation(Stamp stamp) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", stamp.id());
    information.put("name", stamp.name());
    information.put("creation", stamp.creation());
    return information;
  }

  @RequestMapping(path = "/stamp/setup/status/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>>  findStampSetupStatus(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var stampId = body.getUUID("stamp");
    return staffHasPermission(findStaffId(request), "stamp.setup")
      .thenCompose(hasPermission -> findStampSetupStatus(stampId, hasPermission));
  }

  private CompletableFuture<Map<String, Object>> findStampSetupStatus(
    UUID stampId, boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return stampSetupDatabaseTable.stampSetupExists(stampId)
      .thenApply(exists -> Map.of("running", exists));
  }
}
