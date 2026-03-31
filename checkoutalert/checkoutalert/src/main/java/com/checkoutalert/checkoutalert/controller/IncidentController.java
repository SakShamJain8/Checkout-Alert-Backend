package com.checkoutalert.checkoutalert.controller;

import com.checkoutalert.checkoutalert.model.Incident;
import com.checkoutalert.checkoutalert.repository.EndpointRepository;
import com.checkoutalert.checkoutalert.repository.IncidentRepository;
import com.checkoutalert.checkoutalert.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepo;
    private final EndpointRepository endpointRepo;
    private final JwtUtil jwtUtil;

    public IncidentController(IncidentRepository incidentRepo,
                              EndpointRepository endpointRepo,
                              JwtUtil jwtUtil) {
        this.incidentRepo = incidentRepo;
        this.endpointRepo = endpointRepo;
        this.jwtUtil = jwtUtil;
    }
    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body("Unauthorized");
    }

    // GET /api/incidents — only return incidents for THIS user's endpoints
    @GetMapping
    public ResponseEntity<?> getAllIncidents(HttpServletRequest request) {
        String email = jwtUtil.extractEmail(request);
        if (email == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(
                incidentRepo.findTop50ByUserIdOrderByDetectedAtDesc(email));
    }


    // GET /api/incidents/{endpointId} — only if user owns the endpoint
    @GetMapping("/{endpointId}")
    public ResponseEntity<?> getByEndpoint(
            @PathVariable String endpointId,
            HttpServletRequest request) {
        String email = jwtUtil.extractEmail(request);
        if (email == null) return ResponseEntity.status(401).body("Unauthorized");

        boolean owns = endpointRepo.findByIdAndUserId(endpointId, email).isPresent();
        if (!owns) return ResponseEntity.status(403).build();

        return ResponseEntity.ok(
                incidentRepo.findByEndpointIdOrderByDetectedAtDesc(endpointId));
    }
}