package com.mts.aadati.utils.security;

import com.mts.aadati.utils.security.HmacUtil.HmacAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class HmacUtilTest {

    private static final String KEY = "a-very-long-secret-key-for-tests-0123456789";
    private static final String OTHER_KEY = "another-very-long-secret-key-for-tests-9876";
    private static final String VALUE = "payload";

    @ParameterizedTest
    @EnumSource(HmacAlgorithm.class)
    void signThenVerify_succeeds_forAllAlgorithms(HmacAlgorithm algorithm) {
        String signature = HmacUtil.sign(algorithm, KEY, VALUE);

        assertTrue(HmacUtil.verify(algorithm, KEY, signature, VALUE));
    }

    @Test
    void sign_isDeterministic() {
        assertEquals(
                HmacUtil.sign(HmacAlgorithm.SHA256, KEY, VALUE),
                HmacUtil.sign(HmacAlgorithm.SHA256, KEY, VALUE));
    }

    @Test
    void sign_handlesNonAsciiValues() {
        String value = "مرحبا بالعالم";
        String signature = HmacUtil.sign(HmacAlgorithm.SHA256, KEY, value);

        assertTrue(HmacUtil.verify(HmacAlgorithm.SHA256, KEY, signature, value));
    }

    // ---------- Multiple fields ----------

    @Test
    void multipleFields_signThenVerify() {
        String signature = HmacUtil.sign(HmacAlgorithm.SHA256, KEY, "reset", "42");

        assertTrue(HmacUtil.verify(HmacAlgorithm.SHA256, KEY, signature, "reset", "42"));
    }

    @Test
    void multipleFields_areUnambiguous() {
        assertNotEquals(
                HmacUtil.sign(HmacAlgorithm.SHA256, KEY, "ab", "c"),
                HmacUtil.sign(HmacAlgorithm.SHA256, KEY, "a", "bc"));
    }

    @Test
    void multipleFields_orderMatters() {
        String signature = HmacUtil.sign(HmacAlgorithm.SHA256, KEY, "reset", "42");

        assertFalse(HmacUtil.verify(HmacAlgorithm.SHA256, KEY, signature, "42", "reset"));
    }

    @Test
    void multipleFields_signatureForOneFieldDoesNotMatchTwoFields() {
        String signature = HmacUtil.sign(HmacAlgorithm.SHA256, KEY, "reset");

        assertFalse(HmacUtil.verify(HmacAlgorithm.SHA256, KEY, signature, "reset", "42"));
    }

    @ParameterizedTest
    @EnumSource(HmacAlgorithm.class)
    void sign_isUrlSafe_andHasNoPadding(HmacAlgorithm algorithm) {
        String signature = HmacUtil.sign(algorithm, KEY, VALUE);

        assertFalse(signature.contains("+"));
        assertFalse(signature.contains("/"));
        assertFalse(signature.contains("="));
    }

    @Test
    void sign_matchesIndependentReferenceVectors() {
        String key = "k".repeat(32);

        assertEquals("2eiKeJlc98OUh2heQ-gw3drXhZgTvp1RAlyEpNXDgkU",
                HmacUtil.sign(HmacAlgorithm.SHA256, key, "hello"));
        assertEquals("uEQsSVwNXOzBHfd1Zh3nBebK9BhDKopy1h1ptflYIYfiCthJcTx5GM56CfAjIHbi",
                HmacUtil.sign(HmacAlgorithm.SHA384, key, "hello"));
        assertEquals("ObYpz7AlgH170UG7DtsrAtmWuLoXKCVs7bMhWjQ4D23Wd4KfgQJNdMz8pHs-mSWDkpHGILVtS3pVD00PtnR1Qg",
                HmacUtil.sign(HmacAlgorithm.SHA512, key, "hello"));
    }

    @Test
    void sign_multipleFields_matchesIndependentReferenceVector() {
        String key = "k".repeat(32);

        assertEquals("dTicRMd6CnJ_08C3jf5rmniwavO5qQIpQ56y1GqznyQ",
                HmacUtil.sign(HmacAlgorithm.SHA256, key, "reset", "42"));
    }

    @Test
    void verify_rejectsTamperedValue() {
        String signature = HmacUtil.sign(HmacAlgorithm.SHA256, KEY, VALUE);

        assertFalse(HmacUtil.verify(HmacAlgorithm.SHA256, KEY, signature, "tampered"));
    }

    @Test
    void verify_rejectsWrongKey() {
        String signature = HmacUtil.sign(HmacAlgorithm.SHA256, KEY, VALUE);

        assertFalse(HmacUtil.verify(HmacAlgorithm.SHA256, OTHER_KEY, signature, VALUE));
    }

    @Test
    void verify_rejectsSignatureFromDifferentAlgorithm() {
        String sha512Signature = HmacUtil.sign(HmacAlgorithm.SHA512, KEY, VALUE);

        assertFalse(HmacUtil.verify(HmacAlgorithm.SHA256, KEY, sha512Signature, VALUE));
    }

    @Test
    void verify_rejectsTamperedSignature() {
        String signature = HmacUtil.sign(HmacAlgorithm.SHA256, KEY, VALUE);
        char last = signature.charAt(signature.length() - 1);
        char replacement = last == 'A' ? 'B' : 'A';
        String tampered = signature.substring(0, signature.length() - 1) + replacement;

        assertFalse(HmacUtil.verify(HmacAlgorithm.SHA256, KEY, tampered, VALUE));
    }

    @Test
    void verify_rejectsMalformedSignatures_withoutThrowing() {
        assertFalse(HmacUtil.verify(HmacAlgorithm.SHA256, KEY, null, VALUE));
        assertFalse(HmacUtil.verify(HmacAlgorithm.SHA256, KEY, "", VALUE));
        assertFalse(HmacUtil.verify(HmacAlgorithm.SHA256, KEY, "not-base64!!", VALUE));
        assertFalse(HmacUtil.verify(HmacAlgorithm.SHA256, KEY, "c2hvcnQ", VALUE)); // valid Base64, wrong length
    }

    @Test
    void sign_rejectsShortKey() {
        assertThrows(IllegalArgumentException.class,
                () -> HmacUtil.sign(HmacAlgorithm.SHA256, "short", VALUE));
    }

    @Test
    void sign_acceptsKeyOfExactlyMinimumLength() {
        String key = "k".repeat(32);

        assertDoesNotThrow(() -> HmacUtil.sign(HmacAlgorithm.SHA256, key, VALUE));
    }

    @Test
    void sign_rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> HmacUtil.sign(null, KEY, VALUE));
        assertThrows(NullPointerException.class, () -> HmacUtil.sign(HmacAlgorithm.SHA256, null, VALUE));
        assertThrows(NullPointerException.class, () -> HmacUtil.sign(HmacAlgorithm.SHA256, KEY, (String[]) null));
        assertThrows(NullPointerException.class, () -> HmacUtil.sign(HmacAlgorithm.SHA256, KEY, "a", null));
    }

    @Test
    void sign_rejectsNoValues() {
        assertThrows(IllegalArgumentException.class, () -> HmacUtil.sign(HmacAlgorithm.SHA256, KEY));
    }
}