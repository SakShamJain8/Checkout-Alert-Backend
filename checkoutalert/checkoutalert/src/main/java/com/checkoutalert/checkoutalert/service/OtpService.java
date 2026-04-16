package com.checkoutalert.checkoutalert.service;

import com.checkoutalert.checkoutalert.model.OtpToken;
import com.checkoutalert.checkoutalert.repository.OtpTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    private JavaMailSender mailSender;
    private RedisTemplate<String, String> redisTemplate;

    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpTokenRepository otpRepo) {
        this.otpRepo = otpRepo;
    }

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Autowired(required = false)
    public void setRedisTemplate(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isSendRateLimited(String ipAddress) {
        if (redisTemplate == null) return false;

        try {
            String key = "otp_send:ip:" + ipAddress;
            Long attempts = redisTemplate.opsForValue().increment(key);

            if (attempts != null && attempts == 1) {
                redisTemplate.expire(key, Duration.ofHours(1));
            }
            return attempts != null && attempts > 10;
        } catch (Exception e) {
            // Redis unavailable - allow request to proceed
            return false;
        }
    }

    public boolean isDailyLimitReached(String email) {
        if (redisTemplate == null) return false;

        try {
            String key = "otp_daily:" + email;
            Long count = redisTemplate.opsForValue().increment(key);

            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofHours(24));
            }
            return count != null && count > 7;
        } catch (Exception e) {
            // Redis unavailable - allow request to proceed
            return false;
        }
    }

    public void sendOtp(String email, String purpose) {

        if (isDailyLimitReached(email)) {
            throw new RuntimeException("Daily OTP limit reached");
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

   
        if (mailSender != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("CheckoutAlert — Your OTP");
            message.setText("Your OTP is: " + otp);
            mailSender.send(message);
        }
    }

    private boolean isRateLimited(String email, String ipAddress) {
        if (redisTemplate == null) return false;

        try {
            String emailKey = "otp_attempts:email:" + email;
            Long emailAttempts = redisTemplate.opsForValue().increment(emailKey);

            if (emailAttempts != null && emailAttempts == 1) {
                redisTemplate.expire(emailKey, Duration.ofMinutes(15));
            }
            if (emailAttempts != null && emailAttempts > 5) return true;

            String ipKey = "otp_attempts:ip:" + ipAddress;
            Long ipAttempts = redisTemplate.opsForValue().increment(ipKey);

            if (ipAttempts != null && ipAttempts == 1) {
                redisTemplate.expire(ipKey, Duration.ofMinutes(45));
            }
            return ipAttempts != null && ipAttempts > 20;
        } catch (Exception e) {
            // Redis unavailable - allow request to proceed
            return false;
        }
    }

    private void resetRateLimit(String email, String ipAddress) {
        if (redisTemplate == null) return;

        try {
            redisTemplate.delete("otp_attempts:email:" + email);
            redisTemplate.delete("otp_attempts:ip:" + ipAddress);
        } catch (Exception e) {
            // Redis unavailable - silently ignore
        }
    }

    public VerifyResult verifyOtp(String email, String otp, String purpose, String ipAddress) {

        if (isRateLimited(email, ipAddress)) {
            return VerifyResult.RATE_LIMITED;
        }

        Optional<OtpToken> tokenOpt = otpRepo
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose);

        if (tokenOpt.isEmpty()) return VerifyResult.INVALID;

        OtpToken token = tokenOpt.get();

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return VerifyResult.EXPIRED;
        }

        if (!token.getOtp().equals(otp)) {
            return VerifyResult.INVALID;
        }

        token.setUsed(true);
        otpRepo.save(token);

        resetRateLimit(email, ipAddress);

        return VerifyResult.SUCCESS;
    }

    public enum VerifyResult {
        SUCCESS,
        INVALID,
        EXPIRED,
        RATE_LIMITED
    }
}