package com.dulno.team.access.member;

import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.member.MemberDatabaseTable;
import com.dulno.core.member.session.Session;
import com.dulno.core.member.session.SessionDatabaseTable;
import com.dulno.core.member.session.SessionStatus;
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
public final class MemberSessionController extends TeamController {
  private final MemberDatabaseTable memberDatabaseTable;
  private final SessionDatabaseTable sessionDatabaseTable;

  private MemberSessionController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, MemberDatabaseTable memberDatabaseTable,
    SessionDatabaseTable sessionDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.memberDatabaseTable = memberDatabaseTable;
    this.sessionDatabaseTable = sessionDatabaseTable;
  }


  @RequestMapping(path = "/member/sessions/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findMemberSessions(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var memberId = body.getUUID("member");
    return staffHasPermission(findStaffId(request), "member.sessions.find")
      .thenCompose(hasPermission -> memberDatabaseTable.memberExists(memberId)
        .thenCompose(exists -> findMemberSessions(memberId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findMemberSessions(
    UUID memberId, boolean hasPermission, boolean memberExists
  ) {
    if (!hasPermission || !memberExists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return sessionDatabaseTable.findSessionsOfMemberByStatus(memberId,
        SessionStatus.ACTIVE)
      .thenApply(sessions -> Map.of("sessions", sessions.stream()
        .map(this::assemblySessionInformation).toList()));
  }

  private Map<String, Object> assemblySessionInformation(Session session) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", session.id());
    information.put("platform", session.devicePlatform());
    information.put("country", session.country());
    information.put("city", session.city());
    information.put("openTime", session.openTime());
    information.put("lastRefresh", session.lastRefresh());
    information.put("status", session.status());
    return information;
  }

  @RequestMapping(path = "/member/session/close/", method = RequestMethod.POST)
  public void closeMemberSession(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var memberId = body.getUUID("member");
    var sessionId = body.getUUID("session");
    staffHasPermission(findStaffId(request), "member.session.close")
      .thenAccept(hasPermission -> memberDatabaseTable.memberExists(memberId)
        .thenAccept(exists -> closeMemberSession(memberId, sessionId,
          hasPermission, exists)));
  }

  private void closeMemberSession(
    UUID memberId, UUID sessionId, boolean hasPermission, boolean memberExists
  ) {
    if (!hasPermission || !memberExists) {
      return;
    }
    sessionDatabaseTable.sessionExists(sessionId)
      .thenAccept(exists -> closeMemberSession(sessionId, memberId, exists));
  }

  private void closeMemberSession(
    UUID sessionId, UUID memberId, boolean sessionExists
  ) {
    if (!sessionExists) {
      return;
    }
    sessionDatabaseTable.findSession(sessionId)
      .thenAccept(session -> closeMemberSession(session, memberId));
  }

  private void closeMemberSession(Session session, UUID memberId) {
    if (!session.memberId().equals(memberId)) {
      return;
    }
    sessionDatabaseTable.closeSession(session.id());
  }
}