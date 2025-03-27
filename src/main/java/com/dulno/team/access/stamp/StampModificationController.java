package com.dulno.team.access.stamp;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.error.ErrorRepository;
import com.dulno.core.stamp.Stamp;
import com.dulno.core.stamp.StampDatabaseTable;
import com.dulno.core.stamp.StampSetupDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.StaffDatabaseTable;
import com.google.common.collect.Maps;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class StampModificationController extends TeamController {
  private final StampDatabaseTable stampDatabaseTable;
  private final StampSetupDatabaseTable stampSetupDatabaseTable;
  private final ErrorRepository errorRepository;

  private StampModificationController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, StampDatabaseTable stampDatabaseTable,
    StampSetupDatabaseTable stampSetupDatabaseTable,
    ErrorRepository errorRepository
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.stampDatabaseTable = stampDatabaseTable;
    this.stampSetupDatabaseTable = stampSetupDatabaseTable;
    this.errorRepository = errorRepository;
  }

  @RequestMapping(path = "/stamp/create/", method = RequestMethod.POST)
  public void createStamp(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var partnerId = body.getUUID("partner");
    var name = body.getSanitizedString("name");
    staffHasPermission(findStaffId(request), "stamp.create")
      .thenAccept(hasPermission -> createStamp(partnerId, name, hasPermission));
  }

  private void createStamp(UUID partnerId, String name, boolean hasPermission) {
    if (!hasPermission) {
      return;
    }
    stampDatabaseTable.generateAvailableStampId()
      .thenAccept(id -> stampDatabaseTable.insertStamp(id, partnerId, "", "",
        name, null, System.currentTimeMillis()));
  }

  @RequestMapping(path = "/stamp/setup/request/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> requestStampSetup(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var stampId = body.getUUID("stamp");
    return staffHasPermission(findStaffId(request), "stamp.setup")
      .thenCompose(hasPermission -> stampDatabaseTable.stampExists(stampId)
        .thenCompose(stampExists -> requestStampSetup(stampId, hasPermission,
          stampExists)));
  }

  private CompletableFuture<Map<String, Object>> requestStampSetup(
    UUID stampId, boolean hasPermission, boolean stampExists
  ) {
    if (!hasPermission || !stampExists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return stampDatabaseTable.findStamp(stampId).thenApply(this::requestStampSetup);
  }

  private Map<String, Object> requestStampSetup(Stamp stamp) {
    var setupToken = UUID.randomUUID().toString();
    stampSetupDatabaseTable.insertStampSetup(stamp.id(), setupToken);
    var masterKey = generateMasterKey();
    stamp.updateMasterKey(masterKey);
    stampDatabaseTable.updateStamp(stamp);
    return Map.of("qrCode", generateSetupQrCode(stamp.id(), setupToken, masterKey));
  }

  private String generateMasterKey() {
    byte[] rawMasterKey = new byte[16];
    new SecureRandom().nextBytes(rawMasterKey);
    return Hex.encodeHexString(rawMasterKey);
  }

  private static final int QR_CODE_SIZE = 256;

  private String generateSetupQrCode(UUID stampId, String token, String masterKey) {
    try {
      var payload = new JSONObject(Map.of("stamp", stampId, "token", token,
        "masterKey", masterKey)).toString();
      var qrCodeWriter = new QRCodeWriter();
      var bitMatrix = qrCodeWriter.encode(payload, BarcodeFormat.QR_CODE,
        QR_CODE_SIZE, QR_CODE_SIZE);
      var bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
      var outputStream = new ByteArrayOutputStream();
      ImageIO.write(bufferedImage, "png", outputStream);
      var qrCodeBytes = outputStream.toByteArray();
      return Base64.getEncoder().encodeToString(qrCodeBytes);
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return "";
    }
  }

  @RequestMapping(path = "/stamp/setup/complete/", method = RequestMethod.POST)
  public void completeStampSetup(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var stampId = body.getUUID("stamp");
    var setupToken = body.getString("token");
    var stampUid = body.getString("uid");
    staffHasPermission(findStaffId(request), "stamp.setup")
      .thenAccept(hasPermission -> stampSetupDatabaseTable.stampSetupExists(stampId)
        .thenAccept(setupExists -> completeStampSetup(stampId, setupToken,
          stampUid, hasPermission, setupExists)));
  }

  private void completeStampSetup(
    UUID stampId, String setupToken, String stampUid, boolean hasPermission,
    boolean setupExists
  ) {
    if (!hasPermission || !setupExists) {
      return;
    }
    stampSetupDatabaseTable.findStampSetupToken(stampId)
      .thenAccept(originalSetupToken -> stampDatabaseTable.findStamp(stampId)
        .thenAccept(stamp -> completeStampSetup(stamp, setupToken, stampUid,
          originalSetupToken)));
  }

  private void completeStampSetup(
    Stamp stamp, String setupToken, String stampUid, String originalSetupToken
  ) {
    if (!setupToken.equals(originalSetupToken)) {
      return;
    }
    stamp.updateUid(stampUid);
    stampDatabaseTable.updateStamp(stamp);
    stampSetupDatabaseTable.deleteStampSetup(stamp.id());
  }
}
