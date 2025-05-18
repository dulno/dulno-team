package com.dulno.team.access.statistic;

import com.dulno.core.partner.analysis.card.PartnerNewCardDatabaseTable;
import com.dulno.core.scan.ScanDatabaseTable;
import com.dulno.core.user.statistic.UserAppOpen;
import com.dulno.core.user.statistic.UserAppOpenDatabaseTable;
import com.dulno.core.user.statistic.UserJoinDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.StaffDatabaseTable;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class StatisticController extends TeamController {
  private final UserJoinDatabaseTable userJoinDatabaseTable;
  private final UserAppOpenDatabaseTable userAppOpenDatabaseTable;
  private final ScanDatabaseTable scanDatabaseTable;
  private final PartnerNewCardDatabaseTable partnerNewCardDatabaseTable;

  private StatisticController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    UserJoinDatabaseTable userJoinDatabaseTable,
    UserAppOpenDatabaseTable userAppOpenDatabaseTable,
    ScanDatabaseTable scanDatabaseTable,
    PartnerNewCardDatabaseTable partnerNewCardDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.userJoinDatabaseTable = userJoinDatabaseTable;
    this.userAppOpenDatabaseTable = userAppOpenDatabaseTable;
    this.scanDatabaseTable = scanDatabaseTable;
    this.partnerNewCardDatabaseTable = partnerNewCardDatabaseTable;
  }

  @RequestMapping(path = "/statistics/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findStatistics(
    HttpServletRequest request
  ) {
    return staffHasPermission(findStaffId(request), "statistics.find")
      .thenCompose(this::findStatistics);
  }

  private CompletableFuture<Map<String, Object>> findStatistics(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return collectUserInformation()
      .thenCompose(userInformation -> collectStampInformation()
        .thenCompose(stampInformation -> collectCardInformation()
          .thenApply(cardInformation -> assemblyAnalysisInformation(
            userInformation, stampInformation, cardInformation))));
  }

  private Map<String, Object> assemblyAnalysisInformation(
    Map<String, Object> userInformation, Map<String, Object> stampInformation,
    Map<String, Object> cardInformation
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("success", true);
    information.putAll(userInformation);
    information.putAll(stampInformation);
    information.putAll(cardInformation);
    return information;
  }

  private CompletableFuture<Map<String, Object>> collectUserInformation() {
    return userJoinDatabaseTable.findAllUserJoins()
      .thenCompose(userJoinDates -> userAppOpenDatabaseTable.findAllUserAppOpenings()
        .thenApply(userAppOpens -> assemblyUserInformation(userJoinDates,
          userAppOpens)));
  }

  private Map<String, Object> assemblyUserInformation(
    List<Long> userJoinDates, List<UserAppOpen> userAppOpenings
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("users", userJoinDates.size());
    information.put("userJoinTimestamps", userJoinDates);
    information.put("userAppOpenings", userAppOpenings.stream()
      .map(this::assemblyUserAppOpenInformation).toList());
    return information;
  }

  private Map<String, Object> assemblyUserAppOpenInformation(UserAppOpen userAppOpen) {
    var information = Maps.<String, Object>newHashMap();
    information.put("timestamp", userAppOpen.date());
    information.put("version", userAppOpen.version());
    return information;
  }

  private CompletableFuture<Map<String, Object>> collectStampInformation() {
    return scanDatabaseTable.findAllScans()
      .thenApply(this::assemblyStampInformation);
  }

  private Map<String, Object> assemblyStampInformation(
    List<Long> scanDates
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("stamps", scanDates.size());
    information.put("stampTimestamps", scanDates);
    return information;
  }

  private CompletableFuture<Map<String, Object>> collectCardInformation() {
    return partnerNewCardDatabaseTable.findAllNewCards()
      .thenApply(this::assemblyCardInformation);
  }

  private Map<String, Object> assemblyCardInformation(
    List<Long> newCardDates
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("cards", newCardDates.size());
    information.put("cardTimestamps", newCardDates);
    return information;
  }
}
