package com.dulno.team.staff.mfa;

import com.dulno.team.staff.Staff;
import com.dulno.team.staff.StaffDatabaseTable;
import com.google.common.collect.Lists;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public final class StaffMultiFactorAuth {
  private final StaffMultiFactorAuthDatabaseTable multiFactorAuthDatabaseTable;
  private final StaffDatabaseTable staffDatabaseTable;
  private final UUID staffId;

  public CompletableFuture<Void> setup() {
    return multiFactorAuthDatabaseTable.insertAuth(staffId, generateSecret(),
      generateRecoveryCodes());
  }

  private String generateSecret() {
    var secretGenerator = new DefaultSecretGenerator();
    return secretGenerator.generate();
  }

  private List<String> generateRecoveryCodes() {
    var recoveryCodes = new RecoveryCodeGenerator();
    return Lists.newArrayList(recoveryCodes.generateCodes(16));
  }

  public CompletableFuture<byte[]> generateQRCode() {
    return staffDatabaseTable.findStaff(staffId)
      .thenCompose(staff -> multiFactorAuthDatabaseTable.findAuth(staffId)
        .thenApplyAsync(auth -> buildQRCode(staff, auth.secret())));
  }

  private byte[] buildQRCode(Staff staff, String secret) {
    var data = new QrData.Builder()
      .label(staff.email())
      .secret(secret)
      .issuer("Dulno Panel")
      .algorithm(HashingAlgorithm.SHA256)
      .digits(6)
      .period(30)
      .build();
    var generator = new ZxingPngQrGenerator();
    try {
      return generator.generate(data);
    } catch (Exception exception) {
      return new byte[0];
    }
  }

  public CompletableFuture<Boolean> verifyCode(String code) {
    return multiFactorAuthDatabaseTable.authExists(staffId)
      .thenCompose(exists -> verifyCode(code, exists));
  }

  private CompletableFuture<Boolean> verifyCode(
    String code, boolean multiFactorAuthEnabled
  ) {
    if (!multiFactorAuthEnabled) {
      return CompletableFuture.completedFuture(true);
    }
    if (code.contains("-")) {
      return verifyRecoveryCode(code);
    }
    var timeProvider = new SystemTimeProvider();
    var codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA256);
    var verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    return multiFactorAuthDatabaseTable.findAuth(staffId)
      .thenApplyAsync(auth -> verifier.isValidCode(auth.secret(), code));
  }

  public CompletableFuture<Boolean> verifyRecoveryCode(String recoveryCode) {
    return multiFactorAuthDatabaseTable.findAuth(staffId)
      .thenApply(auth -> auth.recoveryCodes().contains(recoveryCode));
  }
}
