package com.carthage.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordResetCodeUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_UPPER_BOUND = 1_000_000;

    private PasswordResetCodeUtil() {
        // utility class, no instance allowed
    }

    public static String generateCode() {
        int value = RANDOM.nextInt(CODE_UPPER_BOUND);
        return String.format("%06d", value);
    }

    public static String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible sur cette JVM", e);
        }
    }

}
