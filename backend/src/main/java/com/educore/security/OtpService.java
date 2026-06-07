package com.educore.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final SmsService    smsService;

    private static final int OTP_LENGTH         = 6;
    private static final int EXPIRATION_MINUTES = 30;
    private static final int RATE_LIMIT_COUNT   = 20;
    private static final int RATE_LIMIT_MINUTES = 60;
    private static final int MAX_ATTEMPTS       = 20;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public String generateAndSendOtp(String phone) {
        checkRateLimit(phone);

        String        otp       = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);

        OtpRecord record = OtpRecord.builder()
                .phone(phone)
                .otp(otp)
                .expiresAt(expiresAt)
                .build();
        otpRepository.save(record);

        try {
            smsService.sendOtp(phone, otp);
            log.info("OTP sent to phone={}", phone);
        } catch (SmsService.SmsException ex) {
            otpRepository.delete(record);
            log.error("SMS failed for phone={}: {}", phone, ex.getMessage());
            throw ex;
        }

        return otp;
    }

    @Transactional
    public String generateAndSendOtpWithReturn(String phone) {
        checkRateLimit(phone);

        String        otp       = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);

        OtpRecord record = OtpRecord.builder()
                .phone(phone)
                .otp(otp)
                .expiresAt(expiresAt)
                .build();
        otpRepository.save(record);

        try {
            smsService.sendOtp(phone, otp);
            log.info("OTP sent to phone={}", phone);
        } catch (SmsService.SmsException ex) {
            otpRepository.delete(record);
            log.error("SMS failed for phone={}: {}", phone, ex.getMessage());
            throw ex;
        }

        return otp;
    }

    @Transactional
    public void verifyOtp(String phone, String otp) {
        OtpRecord record = otpRepository
                .findTopByPhoneAndUsedFalseOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new IllegalStateException(
                        "No OTP found for this phone. Please request a new one."));

        if (record.isExpired()) {
            otpRepository.delete(record);
            throw new IllegalStateException("OTP has expired. Please request a new one.");
        }

        if (record.getFailedAttempts() >= MAX_ATTEMPTS) {
            otpRepository.delete(record);
            throw new IllegalStateException("Too many failed attempts. Please request a new OTP.");
        }

        if (!record.getOtp().equals(otp)) {
            record.incrementFailedAttempts();
            otpRepository.save(record);
            int remaining = MAX_ATTEMPTS - record.getFailedAttempts();
            throw new IllegalArgumentException("Invalid OTP. Remaining attempts: " + remaining);
        }

        otpRepository.delete(record);
        log.info("OTP verified successfully for phone={}", phone);
    }

    @Transactional(readOnly = true)
    public boolean hasValidOtp(String phone) {
        return otpRepository
                .findTopByPhoneAndUsedFalseOrderByCreatedAtDesc(phone)
                .map(OtpRecord::isValid)
                .orElse(false);
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000L)
    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteExpired(LocalDateTime.now());
        log.debug("Expired OTPs cleaned up");
    }

    private void checkRateLimit(String phone) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);
        long count          = otpRepository.countRecentByPhone(phone, since);

        if (count >= RATE_LIMIT_COUNT) {
            log.warn("OTP rate limit exceeded for phone={} ({} attempts in {}min)",
                    phone, count, RATE_LIMIT_MINUTES);
            throw new IllegalStateException(
                    "Rate limit exceeded. Please wait " + RATE_LIMIT_MINUTES + " minutes before trying again.");
        }
    }

    private String generateOtp() {
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        int max = (int) Math.pow(10, OTP_LENGTH) - 1;
        return String.valueOf(min + random.nextInt(max - min + 1));
    }
}
