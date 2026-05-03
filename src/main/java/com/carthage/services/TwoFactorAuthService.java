package com.carthage.services;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;

public class TwoFactorAuthService {

    public String generateSecret() {
        SecretGenerator generator = new DefaultSecretGenerator();
        return generator.generate();
    }

    public Image generateQrCodeImage(String secret, String email) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("Carthage")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator generator = new ZxingPngQrGenerator();
        try {
            byte[] imageData = generator.generate(data);
            return new Image(new ByteArrayInputStream(imageData));
        } catch (QrGenerationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.trim().isEmpty()) {
            return false;
        }
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

        return verifier.isValidCode(secret, code.trim());
    }
}
