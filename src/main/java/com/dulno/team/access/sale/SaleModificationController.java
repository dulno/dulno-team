package com.dulno.team.access.sale;

import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.mail.Mail;
import com.dulno.core.sale.Sale;
import com.dulno.core.sale.SaleDatabaseTable;
import com.dulno.core.sale.SaleMessageDatabaseTable;
import com.dulno.core.sale.SaleMessageSenderType;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.Staff;
import com.dulno.team.staff.StaffDatabaseTable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.UUID;

@RestController
public final class SaleModificationController extends TeamController {
  private final Mail saleMail;
  private final SaleDatabaseTable saleDatabaseTable;
  private final SaleMessageDatabaseTable saleMessageDatabaseTable;

  private SaleModificationController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    @Qualifier("saleMail") Mail saleMail,
    SaleDatabaseTable saleDatabaseTable,
    SaleMessageDatabaseTable saleMessageDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.saleMail = saleMail;
    this.saleDatabaseTable = saleDatabaseTable;
    this.saleMessageDatabaseTable = saleMessageDatabaseTable;
  }

  @RequestMapping(path = "/sale/close/", method = RequestMethod.POST)
  public void closeSale(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var saleId = body.getUUID("sale");
    staffHasPermission(findStaffId(request), "sale.close")
      .thenAccept(hasPermission -> saleDatabaseTable.saleExists(saleId)
        .thenAccept(exists -> closeSale(saleId, hasPermission, exists)));
  }

  private void closeSale(UUID saleId, boolean hasPermission, boolean exists) {
    if (!hasPermission || !exists) {
      return;
    }
    saleDatabaseTable.updateSaleStatus(saleId, Sale.Status.CLOSED);
  }

  @RequestMapping(path = "/sale/answer/", method = RequestMethod.POST)
  public void answerSale(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var saleId = body.getUUID("sale");
    findStaff(request).thenAccept(staff ->
      staffHasPermission(staff.id(), "sale.answer")
        .thenAccept(hasPermission -> saleDatabaseTable.saleExists(saleId)
          .thenAccept(exists -> answerSale(staff, saleId,
            body.getString("message"), hasPermission, exists))));
  }

  private void answerSale(
    Staff staff, UUID saleId, String message, boolean hasPermission,
    boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    saleDatabaseTable.findSale(saleId).thenAccept(sale ->
      saleMessageDatabaseTable.generateAvailableMessageId().thenAccept(id ->
        answerSale(staff, sale, message, id)));
  }

  private void answerSale(
    Staff staff, Sale sale, String message, UUID messageId
  ) {
    sale.resetExpiration();
    saleDatabaseTable.updateSale(sale);
    saleMail.send(sale.sender(), "de", "Re: " + sale.title(), message)
      .thenAccept(publicId -> saleMessageDatabaseTable.insertSaleMessage(
        messageId, publicId, sale.id(), staff.id().toString(),
        SaleMessageSenderType.TEAM, message, System.currentTimeMillis()));
  }
}
