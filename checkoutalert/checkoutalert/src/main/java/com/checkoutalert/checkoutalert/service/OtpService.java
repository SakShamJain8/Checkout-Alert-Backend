package com.checkoutalert.checkoutalert.service;

import com.checkoutalert.checkoutalert.model.OtpToken;
import com.checkoutalert.checkoutalert.repository.OtpTokenRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private final OtpTokenRepository otpRepo;
    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    // cryptographically secure random — not java.util.Random
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpTokenRepository otpRepo,
                      @Autowired(required = false) JavaMailSender mailSender,
                      @Autowired(required = false) RedisTemplate<String, String> redisTemplate) {
        this.otpRepo = otpRepo;
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
    }

    public boolean isSendRateLimited(String ipAddress) {
        String key = "otp_send:ip:" + ipAddress;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, Duration.ofHours(1));
        }
        // max 10 OTP send requests per IP per hour
        return attempts != null && attempts > 10;
    }

    public boolean isDailyLimitReached(String email) {
        String key = "otp_daily:" + email;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // reset at midnight — 24 hour TTL
            redisTemplate.expire(key, Duration.ofHours(24));
        }
        return count != null && count > 7;
    }

    // generate and send OTP
    public void sendOtp(String email, String purpose) {
        if (isDailyLimitReached(email)) {
            throw new RuntimeException("Daily OTP limit reached for this email");
        }
        otpRepo.deleteByEmail(email);
        String otp = String.format("%06d", secureRandom.nextInt(1000000));

        OtpToken token = new OtpToken();
        token.setEmail(email);
        token.setOtp(otp);
        token.setPurpose(purpose);
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        token.setCreatedAt(LocalDateTime.now());
        otpRepo.save(token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("CheckoutAlert — Your OTP");
        message.setText(
                "Your OTP for CheckoutAlert " +
                        (purpose.equals("REGISTER") ? "registration" : "login") + " is:\n\n" +
                        otp + "\n\n" +
                        "This OTP expires in 5 minutes.\n" +
                        "Do not share this with anyone.\n\n" +
                        "If you did not request this, ignore this email.\n\n" +
                        "— CheckoutAlert"
        );
        mailSender.send(message);
    }

    private boolean isRateLimited(String email, String ipAddress) {
        // check email-based limit
        String emailKey = "otp_attempts:email:" + email;
        Long emailAttempts = redisTemplate.opsForValue().increment(emailKey);
        if (emailAttempts != null && emailAttempts == 1) {
            redisTemplate.expire(emailKey, Duration.ofMinutes(15));
        }
        if (emailAttempts != null && emailAttempts > 5) {
            return true;
        }

        // check IP-based limit
        String ipKey = "otp_attempts:ip:" + ipAddress;
        Long ipAttempts = redisTemplate.opsForValue().increment(ipKey);
        if (ipAttempts != null && ipAttempts == 1) {
            redisTemplate.expire(ipKey, Duration.ofMinutes(45));
        }
        if (ipAttempts != null && ipAttempts > 20) {
            // IP limit is higher — 20 attempts per 15 min
            // allows multiple legit users on same network
            return true;
        }

        return false;
    }

    // reset rate limit on successful verification
    private void resetRateLimit(String email, String ipAddress) {
        redisTemplate.delete("otp_attempts:email:" + email);
        redisTemplate.delete("otp_attempts:ip:" + ipAddress);
    }

    // verify OTP
    public VerifyResult verifyOtp(String email, String otp, String purpose, String ipAddress) {
        // check rate limit first
        if (isRateLimited(email, ipAddress)) {
            return VerifyResult.RATE_LIMITED;
        }

        Optional<OtpToken> tokenOpt = otpRepo
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        email, purpose);

        if (tokenOpt.isEmpty()) {
            return VerifyResult.INVALID;
        }

        OtpToken token = tokenOpt.get();

        // check expiry
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return VerifyResult.EXPIRED;
        }

        // check OTP matches
        if (!token.getOtp().equals(otp)) {
            return VerifyResult.INVALID;
        }

        // success — mark as used and reset rate limit
        token.setUsed(true);
        otpRepo.save(token);
        resetRateLimit(email, ipAddress);

        return VerifyResult.SUCCESS;
    }

    // result enum — cleaner than boolean
    public enum VerifyResult {
        SUCCESS,
        INVALID,
        EXPIRED,
        RATE_LIMITED
    }
}