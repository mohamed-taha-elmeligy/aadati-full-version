package com.mts.aadati.otp;

import com.mts.aadati.utils.security.HmacUtil;
import com.mts.aadati.utils.security.SecureRandomUtil;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class OtpService {

    private static final Pattern OTP_FORMAT = Pattern.compile("\\d{6}");

    private final OtpRepository otpRepository;
    private final OtpProperties properties;

    public OtpService(OtpRepository otpRepository, OtpProperties properties) {
        this.otpRepository = otpRepository;
        this.properties = properties;
    }

    public String generate(String value, OtpPurpose purpose) {
        Objects.requireNonNull(purpose, "purpose");
        String normalized = normalize(value);

        String otp = SecureRandomUtil.generateOtp();
        otpRepository.save(normalized, hmac(normalized, otp, purpose), properties.expiration(), purpose);
        return otp;
    }

    public OtpVerifyResult verify(String value, String otp, OtpPurpose purpose) {
        if (purpose == null || value == null || otp == null || !OTP_FORMAT.matcher(otp).matches()) {
            return OtpVerifyResult.INVALID;
        }

        String normalized = normalize(value);
        return otpRepository.verify(
                normalized,
                hmac(normalized, otp, purpose),
                properties.maxAttempts(),
                properties.failWindow(),
                purpose);
    }

    public void clearFailures(String value, OtpPurpose purpose) {
        otpRepository.clearFailures(normalize(value), purpose);
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "value");
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String hmac(String normalizedValue, String otp, OtpPurpose purpose) {
        return HmacUtil.sign(
                HmacUtil.HmacAlgorithm.SHA512,
                properties.hmac().key(),
                purpose.name(), normalizedValue, otp);
    }
}