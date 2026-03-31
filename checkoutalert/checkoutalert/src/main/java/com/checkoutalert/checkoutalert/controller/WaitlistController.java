package com.checkoutalert.checkoutalert.controller;

import com.checkoutalert.checkoutalert.model.Waitlist;
import com.checkoutalert.checkoutalert.repository.WaitlistRepository;
import com.checkoutalert.checkoutalert.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistRepository waitlistRepo;
    private final JwtUtil jwtUtil;

    @Value("${admin.secret}")
    private String adminSecret;

    public WaitlistController(WaitlistRepository waitlistRepo, JwtUtil jwtUtil) {
        this.waitlistRepo = waitlistRepo;
        this.jwtUtil = jwtUtil;
    }
    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body("Unauthorized");
    }

    @PostMapping
    public ResponseEntity<?> joinWaitlist(HttpServletRequest request) {
        String email = jwtUtil.extractEmail(request);

        if (waitlistRepo.existsByEmail(email)) {
            return ResponseEntity.ok(Map.of(
                    "message", "Already on waitlist",
                    "alreadyJoined", true
            ));
        }

        Waitlist entry = new Waitlist();
        entry.setEmail(email);
        entry.setJoinedAt(LocalDateTime.now());
        waitlistRepo.save(entry);

        return ResponseEntity.ok(Map.of(
                "message", "Successfully joined waitlist",
                "alreadyJoined", false
        ));
    }

    @GetMapping("/admin")
    public ResponseEntity<?> getWaitlist(@RequestParam String secret) {
        // constant time comparison — prevents timing attacks
        if (!constantTimeEquals(secret, adminSecret)) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        List<Waitlist> entries = waitlistRepo.findAll();
        return ResponseEntity.ok(Map.of(
                "count", entries.size(),
                "entries", entries
        ));
    }

    // prevents timing attack — always takes same time regardless of match
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}