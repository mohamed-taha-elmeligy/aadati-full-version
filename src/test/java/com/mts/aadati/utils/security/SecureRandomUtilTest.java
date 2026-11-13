package com.mts.aadati.utils.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecureRandomUtilTest {

    private static final int ITERATIONS = 10_000;

    // ---------- generateOtp ----------

    @Test
    void generateOtp_isAlwaysSixAsciiDigits() {
        for (int i = 0; i < ITERATIONS; i++) {
            String otp = SecureRandomUtil.generateOtp();

            assertTrue(otp.matches("[0-9]{6}"), "Unexpected OTP format: " + otp);
        }
    }

    @Test
    void generateOtp_keepsLeadingZeros() {
        // ~10% of OTPs start with '0'; over 10,000 runs, missing all of them is practically impossible.
        boolean sawLeadingZero = false;
        for (int i = 0; i < ITERATIONS; i++) {
            String otp = SecureRandomUtil.generateOtp();
            assertEquals(6, otp.length());
            if (otp.charAt(0) == '0') {
                sawLeadingZero = true;
            }
        }
        assertTrue(sawLeadingZero);
    }

    @Test
    void generateOtp_usesAsciiDigits_evenWithArabicDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));

            for (int i = 0; i < 1_000; i++) {
                String otp = SecureRandomUtil.generateOtp();
                assertTrue(otp.matches("[0-9]{6}"), "Locale-dependent digits in OTP: " + otp);
            }
        } finally {
            Locale.setDefault(original);
        }
    }

    // ---------- generateNumber ----------

    @Test
    void generateNumber_staysWithinBound_andCoversWholeRange() {
        int bound = 10;
        Set<Integer> seen = new HashSet<>();

        for (int i = 0; i < ITERATIONS; i++) {
            int n = SecureRandomUtil.generateNumber(bound);
            assertTrue(n >= 0 && n < bound, "Out of range: " + n);
            seen.add(n);
        }
        assertEquals(bound, seen.size());
    }

    @Test
    void generateNumber_rejectsNonPositiveBound() {
        assertThrows(IllegalArgumentException.class, () -> SecureRandomUtil.generateNumber(0));
        assertThrows(IllegalArgumentException.class, () -> SecureRandomUtil.generateNumber(-5));
    }

    // ---------- generateBytes ----------

    @Test
    void generateBytes_returnsRequestedLength() {
        assertEquals(1, SecureRandomUtil.generateBytes(1).length);
        assertEquals(16, SecureRandomUtil.generateBytes(16).length);
        assertEquals(64, SecureRandomUtil.generateBytes(64).length);
    }

    @Test
    void generateBytes_isNotAllZeros() {
        byte[] bytes = SecureRandomUtil.generateBytes(32);

        boolean allZero = true;
        for (byte b : bytes) {
            if (b != 0) {
                allZero = false;
                break;
            }
        }
        assertFalse(allZero);
    }

    @Test
    void generateBytes_differsBetweenCalls() {
        assertFalse(java.util.Arrays.equals(
                SecureRandomUtil.generateBytes(32),
                SecureRandomUtil.generateBytes(32)));
    }

    @Test
    void generateBytes_rejectsNonPositiveLength() {
        assertThrows(IllegalArgumentException.class, () -> SecureRandomUtil.generateBytes(0));
        assertThrows(IllegalArgumentException.class, () -> SecureRandomUtil.generateBytes(-1));
    }

    // ---------- generateToken ----------

    @Test
    void generateToken_hasExpectedLength() {
        // Base64 without padding: 32 bytes -> 43 chars, 16 bytes -> 22 chars.
        assertEquals(43, SecureRandomUtil.generateToken(32).length());
        assertEquals(22, SecureRandomUtil.generateToken(16).length());
    }

    @Test
    void generateToken_isUrlSafe() {
        for (int i = 0; i < 1_000; i++) {
            String token = SecureRandomUtil.generateToken(32);

            assertTrue(token.matches("[A-Za-z0-9_-]+"), "Not URL-safe: " + token);
        }
    }

    @Test
    void generateToken_decodesBackToRequestedByteLength() {
        byte[] decoded = Base64.getUrlDecoder().decode(SecureRandomUtil.generateToken(32));

        assertEquals(32, decoded.length);
    }

    @Test
    void generateToken_isUniqueAcrossManyCalls() {
        Set<String> tokens = new HashSet<>();

        for (int i = 0; i < ITERATIONS; i++) {
            tokens.add(SecureRandomUtil.generateToken(32));
        }
        assertEquals(ITERATIONS, tokens.size());
    }

    @Test
    void generateToken_rejectsNonPositiveLength() {
        assertThrows(IllegalArgumentException.class, () -> SecureRandomUtil.generateToken(0));
    }
}