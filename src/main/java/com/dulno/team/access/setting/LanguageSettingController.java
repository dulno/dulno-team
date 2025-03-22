package com.dulno.team.access.setting;

import com.dulno.core.access.DulnoRequestBody;
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
public final class LanguageSettingController extends TeamController {
  private LanguageSettingController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
  }

  @RequestMapping(path = "/settings/language/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> language(
    HttpServletRequest request
  ) {
    return findStaff(request).thenApply(member ->
      Map.of("language", member.language()));
  }

  @RequestMapping(path = "/settings/language/change/", method = RequestMethod.POST)
  public void changeLanguage(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    staffDatabaseTable().changeStaffLanguage(findStaffId(request),
      body.getString("language"));
  }
}
