package com.checkoutalert.checkoutalert.controller;

import com.checkoutalert.checkoutalert.model.MonitoredEndpoint;
import com.checkoutalert.checkoutalert.model.PingResult;
import com.checkoutalert.checkoutalert.repository.EndpointRepository;
import com.checkoutalert.checkoutalert.repository.PingResultRepository;
import com.checkoutalert.checkoutalert.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/endpoints")
public class EndpointController {
    private final EndpointRepository endpointRepo;
    private final PingResultRepository pingResultRepo;
    private final JwtUtil jwtUtil;

    public EndpointController(EndpointRepository endpointRepo,
                              PingResultRepository pingResultRepo, JwtUtil jwtUtil) {
        this.endpointRepo = endpointRepo;
        this.pingResultRepo = pingResultRepo;
        this.jwtUtil = jwtUtil;
    }
    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body("Unauthorized");
    }

    private ResponseEntity<?> validateEndpoint(MonitoredEndpoint endpoint) {
        // validate URL format
        try {
            java.net.URL url = new java.net.URL(endpoint.getUrl());
            if (!url.getProtocol().equals("http") &&
                    !url.getProtocol().equals("https")) {
                return ResponseEntity.badRequest()
                        .body("URL must use http or https protocol");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid URL format");
        }

        // whitelist HTTP methods
        Set<String> allowedMethods = Set.of("GET", "POST", "PUT", "DELETE", "PATCH");
        if (!allowedMethods.contains(endpoint.getHttpMethod().toUpperCase())) {
            return ResponseEntity.badRequest().body("Invalid HTTP method");
        }

        // validate threshold
        if (endpoint.getThresholdMs() < 100 || endpoint.getThresholdMs() > 30000) {
            return ResponseEntity.badRequest()
                    .body("Threshold must be between 100ms and 30000ms");
        }

        // validate expected status
        if (endpoint.getExpectedStatus() < 100 || endpoint.getExpectedStatus() > 599) {
            return ResponseEntity.badRequest()
                    .body("Expected status must be a valid HTTP status code");
        }

        // validate name length
        if (endpoint.getName() == null || endpoint.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Name is required");
        }
        if (endpoint.getName().length() > 100) {
            return ResponseEntity.badRequest()
                    .body("Name must be under 100 characters");
        }

        return null; // null means valid
    }

    // GET /api/endpoints — list all monitored endpoints
    @GetMapping
    public ResponseEntity<?> getAllEndpoints(HttpServletRequest request) {
        String email = jwtUtil.extractEmail(request);
        if (email == null) return unauthorized();
        return ResponseEntity.ok(endpointRepo.findByUserId(email));
    }

    // POST /api/endpoints — add a new endpoint to monitor
    @PostMapping
    public ResponseEntity<?> addEndpoint(
            @RequestBody MonitoredEndpoint endpoint,
            HttpServletRequest request) {
        String email = jwtUtil.extractEmail(request);
        if (email == null) return unauthorized();

        // validate input
        ResponseEntity<?> validationError = validateEndpoint(endpoint);
        if (validationError != null) return validationError;

        long count = endpointRepo.findByUserId(email).size();
        if (count >= 5) return ResponseEntity.status(403).body("LIMIT_REACHED");

        endpoint.setUserId(email);
        endpoint.setActive(true);
        endpoint.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(endpointRepo.save(endpoint));
    }

    // DELETE /api/endpoints/{id} — remove an endpoint
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEndpoint(
            @PathVariable String id,
            HttpServletRequest request) {
        String email = jwtUtil.extractEmail(request);
        if (email == null) return unauthorized();
        Optional<MonitoredEndpoint> ep = endpointRepo.findByIdAndUserId(id, email);
        if (ep.isEmpty()) return ResponseEntity.notFound().build();
        endpointRepo.deleteById(id);
        return ResponseEntity.ok("Endpoint deleted");
    }

    // GET /api/endpoints/{id}/history — last 10 ping results
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(
            @PathVariable String id,
            HttpServletRequest request) {
        String email = jwtUtil.extractEmail(request);
        if (email == null) return unauthorized();
        if (endpointRepo.findByIdAndUserId(id, email).isEmpty()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(
                pingResultRepo.findTop10ByEndpointIdOrderByPingedAtDesc(id));
    }

    // PATCH /api/endpoints/{id}/toggle — pause or resume monitoring
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleEndpoint(
            @PathVariable String id,
            HttpServletRequest request) {
        String email = jwtUtil.extractEmail(request);
        if (email == null) return unauthorized();
        return endpointRepo.findByIdAndUserId(id, email).map(ep -> {
            ep.setActive(!ep.isActive());
            return ResponseEntity.ok(endpointRepo.save(ep));
        }).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/{id}/uptime")
    public ResponseEntity<?> getUptime(
            @PathVariable String id,
            HttpServletRequest request) {
        String email = jwtUtil.extractEmail(request);
        if (email == null) return unauthorized();
        if (endpointRepo.findByIdAndUserId(id, email).isEmpty()) {
            return ResponseEntity.status(403).build();
        }
        long total = pingResultRepo.countByEndpointId(id);
        long successful = pingResultRepo.countByEndpointIdAndSuccessTrue(id);
        double uptime = total == 0 ? 100.0 : (successful * 100.0) / total;
        uptime = Math.round(uptime * 100.0) / 100.0;
        return ResponseEntity.ok(Map.of(
                "endpointId", id,
                "totalPings", total,
                "successfulPings", successful,
                "uptimePercent", uptime
        ));
    }
}
