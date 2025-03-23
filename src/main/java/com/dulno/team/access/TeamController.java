package com.dulno.team.access;

import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.Staff;
import com.dulno.team.staff.StaffDatabaseTable;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.security.Key;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter(AccessLevel.PROTECTED)
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamController {
  private final Key secretKey;
  private final StaffDatabaseTable staffDatabaseTable;
  private final GroupDatabaseTable groupDatabaseTable;

  /**
   * Is used to find the id of the staff that send the request
   * @param request The request
   * @return The id of the staff
   */
  protected UUID findStaffId(HttpServletRequest request) {
    return findStaffId(findApiKey(request));
  }

  /**
   * Is used to find the id of a staff inside an api key
   * @param apiKey The api key
   * @return The id of the staff
   */
  protected UUID findStaffId(String apiKey) {
    return UUID.fromString(Jwts.parser().setSigningKey(secretKey).build()
      .parseClaimsJws(apiKey).getPayload().get("id", String.class));
  }

  /**
   * Is used to find the staff that send the request
   * @param request The request
   * @return A future that contains the staff
   */
  protected CompletableFuture<Staff> findStaff(HttpServletRequest request) {
    var apiKey = request.getHeader("Authorization").replace("Bearer ", "");
    return staffDatabaseTable.findStaff(findStaffId(apiKey));
  }

  /**
   * Is used to find the id of the session that send the request
   * @param request The request
   * @return The id of the session
   */
  protected UUID findSessionId(HttpServletRequest request) {
    return findSessionId(findApiKey(request));
  }

  /**
   * Is used to find the id of a session inside an api key
   * @param apiKey The api key
   * @return The id of the session
   */
  protected UUID findSessionId(String apiKey) {
    return UUID.fromString(Jwts.parser().setSigningKey(secretKey).build()
      .parseClaimsJws(apiKey).getPayload().get("session", String.class));
  }

  /**
   * Is used to find the api key that is sent via a request
   * @param request The request
   * @return The api key
   */
  protected String findApiKey(HttpServletRequest request) {
    return request.getHeader("Authorization").replace("Bearer ", "");
  }

  /**
   * Checks whether an api key is valid
   * @param apiKey The api key
   * @return Is true if api key is valid, otherwise false
   */
  protected boolean isValidApiKey(String apiKey) {
    try {
      Jwts.parser()
        .setSigningKey(secretKey())
        .build()
        .parseClaimsJws(apiKey);
      return true;
    } catch (Exception exception) {
      return false;
    }
  }

  /**
   * Checks whether a team staff has a certain permission
   * @param staffId The staff
   * @param permission The permission to be checked
   * @return Is true when staff has permission, otherwise false
   */
  protected CompletableFuture<Boolean> staffHasPermission(
    UUID staffId, String permission
  ) {
    var completableFuture = new CompletableFuture<Boolean>();
    staffDatabaseTable.findStaff(staffId).thenAccept(staff ->
      staffHasPermission(staff, permission).thenAccept(completableFuture::complete));
    return completableFuture;
  }

  private CompletableFuture<Boolean> staffHasPermission(
    Staff staff, String permission
  ) {
    if (staff.permissions().contains(permission) || staff.permissions().contains("*")) {
      return CompletableFuture.completedFuture(true);
    }
    return groupDatabaseTable.findGroup(staff.group()).thenApply(group ->
      group.permissions().contains(permission) || group.permissions().contains("*"));
  }
}
