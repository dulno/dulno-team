package com.dulno.team.access.setting;

import com.dulno.core.request.DulnoRequestBody;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.StaffDatabaseTable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ProfileSettingController extends TeamController {
  private ProfileSettingController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
  }

  @RequestMapping(path = "/settings/profile/username/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> username(
    HttpServletRequest request
  ) {
    return findStaff(request).thenApply(staff ->
      Map.of("username", staff.name()));
  }

  @RequestMapping(path = "/settings/profile/id/", method = RequestMethod.GET)
  public Map<String, Object> id(HttpServletRequest request) {
    return Map.of("id", findStaffId(request));
  }

  @RequestMapping(path = "/settings/profile/username/change/", method = RequestMethod.POST)
  public void changeUsername(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    staffDatabaseTable().changeStaffName(findStaffId(request),
      body.getString("username"));
  }
}
