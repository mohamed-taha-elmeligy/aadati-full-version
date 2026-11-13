package com.mts.aadati.utils.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;

public final class HmacUtil {

    private static final int MIN_KEY_BYTES = 32;
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private HmacUtil() {}

    /**
     * Signs one or more fields. The fields are always length-prefixed first, so
     * ("ab", "c") and ("a", "bc") produce different signatures.
     */
    public static String sign(HmacAlgorithm algorithm, String key, String... values) {
        return ENCODER.encodeToString(compute(algorithm, key, canonicalize(values)));
    }

    /**
     * Constant-time verification. Note the order: the signature comes BEFORE the values
     * (varargs must be last). Returns false, never throws, for malformed signatures.
     */
    public static boolean verify(HmacAlgorithm algorithm, String key, String expectedSignature, String... values) {
        if (expectedSignature == null) {
            return false;
        }

        byte[] expected;
        try {
            expected = DECODER.decode(expectedSignature);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return MessageDigest.isEqual(compute(algorithm, key, canonicalize(values)), expected);
    }

    private static String canonicalize(String... values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) {
            throw new IllegalArgumentException("at least one value is required");
        }

        StringBuilder result = new StringBuilder();
        for (String value : values) {
            Objects.requireNonNull(value, "values must not contain null");
            result.append(value.length()).append(':').append(value);
        }
        return result.toString();
    }

    private static byte[] compute(HmacAlgorithm algorithm, String key, String value) {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(key, "key");

        byte[] keyBytes = key.getBytes(CHARSET);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalArgumentException("HMAC key must be at least " + MIN_KEY_BYTES + " bytes");
        }

        try {
            Mac mac = Mac.getInstance(algorithm.getAlgorithmName());
            mac.init(new SecretKeySpec(keyBytes, algorithm.getAlgorithmName()));
            return mac.doFinal(value.getBytes(CHARSET));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    public enum HmacAlgorithm {
        SHA256("HmacSHA256"),
        SHA384("HmacSHA384"),
        SHA512("HmacSHA512");

        private final String algorithmName;

        HmacAlgorithm(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }
    }
}