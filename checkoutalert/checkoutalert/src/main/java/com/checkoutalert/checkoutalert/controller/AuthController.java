package com.checkoutalert.checkoutalert.controller;

import com.checkoutalert.checkoutalert.model.User;
import com.checkoutalert.checkoutalert.repository.UserRepository;
import com.checkoutalert.checkoutalert.service.JwtService;
import com.checkoutalert.checkoutalert.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepo,
                          JwtService jwtService,
                          OtpService otpService,
                          PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }
    @Value("${app.production:false}")
    private boolean isProduction;
    private void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("auth_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction); // true in prod, false in local dev
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        cookie.setAttribute("SameSite", "None");
        response.addCookie(cookie);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs — take the first one
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPANEBc6Q";

    @PostMapping("/register/send-otp")
    public ResponseEntity<?> registerSendOtp(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            String ip = getClientIp(request);

            if (otpService.isSendRateLimited(ip)) {
                return ResponseEntity.status(429)
                        .body("Too many OTP requests. Try again in an hour.");
            }

            String email = body.get("email");
            String password = body.get("password");

            if (email == null || password == null) {
                return ResponseEntity.badRequest()
                        .body("Email and password required");
            }

            if (password.length() < 8) {
                return ResponseEntity.badRequest()
                        .body("Password must be at least 8 characters");
            }

            if (!userRepo.existsByEmail(email)) {
                otpService.sendOtp(email, "REGISTER");
            }

            // same response regardless — attacker can't tell the difference
            return ResponseEntity.ok(Map.of(
                    "message", "If this email is available, an OTP has been sent."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Registration error: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body("Internal server error: " + e.getMessage());
        }
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> registerVerifyOtp(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String email = body.get("email");
        String otp = body.get("otp");
        String password = body.get("password");
        String name = body.get("name");
        String ip = getClientIp(request);

        // check if already registered — at verify step it's safe to tell them
        if (userRepo.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body("This email is already registered. Please sign in.");
        }

        OtpService.VerifyResult result =
                otpService.verifyOtp(email, otp, "REGISTER", ip);

        switch (result) {
            case RATE_LIMITED:
                return ResponseEntity.status(429)
                        .body("Too many attempts. Try again in 15 minutes.");
            case EXPIRED:
                return ResponseEntity.badRequest()
                        .body("OTP has expired. Request a new one.");
            case INVALID:
                return ResponseEntity.badRequest()
                        .body("Invalid OTP. Please try again.");
            case SUCCESS:
                break;
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);
        user.setCreatedAt(LocalDateTime.now());
        userRepo.save(user);

        String token = jwtService.generateToken(email);
        setAuthCookie(response, token);

        return ResponseEntity.ok(Map.of(
                "email", email,
                "name", name != null ? name : ""
        ));
    }

    @PostMapping("/login/send-otp")
    public ResponseEntity<?> loginSendOtp(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String ip = getClientIp(request);

        if (otpService.isSendRateLimited(ip)) {
            return ResponseEntity.status(429)
                    .body("Too many OTP requests. Try again in an hour.");
        }

        String email = body.get("email");
        String password = body.get("password");

        Optional<User> userOpt = userRepo.findByEmail(email);

        String hashToCheck = userOpt.map(User::getPassword).orElse(DUMMY_HASH);
        boolean passwordMatches = passwordEncoder.matches(password, hashToCheck);

        if (userOpt.isEmpty() || !passwordMatches) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        try {
            otpService.sendOtp(email, "LOGIN");
        } catch (RuntimeException e) {
            return ResponseEntity.status(429)
                    .body("Too many OTP requests for this email. Try again tomorrow.");
        }
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + email));
    }



    @PostMapping("/login/verify-otp")
    public ResponseEntity<?> loginVerifyOtp(
            @RequestBody Map<String, String> body,
            HttpServletRequest request, HttpServletResponse response) {
        String email = body.get("email");
        String otp = body.get("otp");
        String ip = getClientIp(request);

        OtpService.VerifyResult result =
                otpService.verifyOtp(email, otp, "LOGIN", ip);

        switch (result) {
            case RATE_LIMITED:
                return ResponseEntity.status(429)
                        .body("Too many attempts. Try again in 45 minutes.");
            case EXPIRED:
                return ResponseEntity.badRequest()
                        .body("OTP has expired. Request a new one.");
            case INVALID:
                return ResponseEntity.badRequest()
                        .body("Invalid OTP. Please try again.");
            case SUCCESS:
                break;
        }

        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        String token = jwtService.generateToken(email);
        setAuthCookie(response, token);  // set httpOnly cookie

        return ResponseEntity.ok(Map.of(
                "email", email,
                "name", userOpt.get().getName() != null ? userOpt.get().getName() : ""
                // don't send token in body anymore
        ));
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("auth_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // delete immediately
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}