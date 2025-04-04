package com.dulno.team.access.setting;

import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.member.session.SessionStatus;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.StaffDatabaseTable;
import com.dulno.team.staff.session.StaffSession;
import com.dulno.team.staff.session.StaffSessionDatabaseTable;
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
public final class SessionSettingController extends TeamController {
  private final StaffSessionDatabaseTable sessionDatabaseTable;

  private SessionSettingController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    StaffSessionDatabaseTable sessionDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.sessionDatabaseTable = sessionDatabaseTable;
  }

  @RequestMapping(path = "/settings/sessions/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> sessions(
    HttpServletRequest request
  ) {
    var staffSessionId = findSessionId(request);
    return sessionDatabaseTable.findSessionsOfStaffByStatus(
      findStaffId(request), SessionStatus.ACTIVE)
      .thenApply(sessions -> Map.of("sessions", sessions.stream().map(session ->
        assemblySessionInformation(staffSessionId, session)).toList()));
  }

  private Map<String, Object> assemblySessionInformation(
    UUID staffSessionId, StaffSession session
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", session.id());
    information.put("platform", session.devicePlatform());
    information.put("country", session.country());
    information.put("city", session.city());
    information.put("openTime", session.openTime());
    information.put("status", session.status());
    information.put("isCurrent", staffSessionId.equals(session.id()));
    return information;
  }

  @RequestMapping(path = "/settings/session/close/", method = RequestMethod.POST)
  public void closeSession(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var sessionId = body.getUUID("session");
    var staffId = findStaffId(request);
    var staffSessionId = findSessionId(request);
    sessionDatabaseTable.sessionExists(sessionId)
      .thenAccept(exists -> closeSession(sessionId, staffId, staffSessionId, exists));
  }

  private void closeSession(
    UUID sessionId, UUID staffId, UUID staffSessionId, boolean sessionExists
  ) {
    if (!sessionExists) {
      return;
    }
    sessionDatabaseTable.findSession(sessionId)
      .thenAccept(session -> closeSession(session, staffId, staffSessionId));
  }

  private void closeSession(StaffSession session, UUID staffId, UUID staffSessionId) {
    if (!session.staffId().equals(staffId)) {
      return;
    }
    if (staffSessionId.equals(session.id())) {
      return;
    }
    sessionDatabaseTable.closeSession(session.id());
  }
}