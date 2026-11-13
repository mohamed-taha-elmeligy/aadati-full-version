package com.mts.aadati.utils.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

public final class SecureRandomUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private SecureRandomUtil() {}

    public static int generateNumber(int bound) {
        return RANDOM.nextInt(bound);
    }

    public static String generateOtp() {
        return String.format(Locale.ROOT, "%06d", generateNumber(1_000_000));
    }

    public static byte[] generateBytes(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static String generateToken(int byteLength) {
        return URL_ENCODER.encodeToString(generateBytes(byteLength));
    }
}