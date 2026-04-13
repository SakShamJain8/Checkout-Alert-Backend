package com.checkoutalert.checkoutalert.service;

import com.checkoutalert.checkoutalert.model.MonitoredEndpoint;
import com.checkoutalert.checkoutalert.model.PingResult;
import com.checkoutalert.checkoutalert.repository.EndpointRepository;
import com.checkoutalert.checkoutalert.repository.PingResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class PingSchedulerService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PingSchedulerService.class);
    private final EndpointRepository endpointRepo;
    private final PingResultRepository pingResultRepo;
    private final BaselineCacheService baselineCache;
    private final AnomalyDetectorService anomalyDetector;
    private final AlertService alertService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PingSchedulerService(EndpointRepository endpointRepo,
                                PingResultRepository pingResultRepo,
                                BaselineCacheService baselineCache,
                                AnomalyDetectorService anomalyDetector,
                                AlertService alertService) {
        this.endpointRepo = endpointRepo;
        this.pingResultRepo = pingResultRepo;
        this.baselineCache = baselineCache;
        this.anomalyDetector = anomalyDetector;
        this.alertService = alertService;
    }

    // thread pool — pings all endpoints concurrently
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private final RestTemplate restTemplate = new RestTemplate();

    private static final Set<String> BLOCKED_HEADERS = Set.of(
            "host", "content-length", "transfer-encoding",
            "connection", "upgrade", "x-forwarded-for",
            "x-forwarded-host", "x-forwarded-proto"
    );
    private boolean isValidHeaderName(String name) {
        if (name == null || name.isBlank()) return false;
        // only allow alphanumeric, hyphens, underscores
        if (!name.matches("^[a-zA-Z0-9\\-_]+$")) return false;
        if (BLOCKED_HEADERS.contains(name.toLowerCase())) return false;
        return true;
    }

    private boolean isValidHeaderValue(String value) {
        if (value == null) return false;
        // block newlines — prevent header injection
        if (value.contains("\n") || value.contains("\r")) return false;
        // max 500 chars
        if (value.length() > 500) return false;
        return true;
    }

    @Scheduled(fixedDelay = 45000) // runs every 45 seconds
    public void pingAllEndpoints() {
        List<MonitoredEndpoint> endpoints = endpointRepo.findAllByActiveTrue();
        log.info("Pinging {} endpoints...", endpoints.size());

        List<CompletableFuture<Void>> futures = endpoints.stream()
                .map(endpoint -> CompletableFuture.runAsync(
                        () -> pingEndpoint(endpoint), threadPool))
                .toList();

        // wait for all pings to finish
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void pingEndpoint(MonitoredEndpoint endpoint) {
        long start = System.currentTimeMillis();
        int statusCode = 0;
        boolean success = false;

        try {
            // build headers
            HttpHeaders headers = new HttpHeaders();

            // parse custom headers if present
            if (endpoint.getCustomHeaders() != null
                    && !endpoint.getCustomHeaders().isBlank()) {
                try {
                    Map<String, String> headerMap = objectMapper.readValue(
                            endpoint.getCustomHeaders(),
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}
                    );
                    for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                        String name = entry.getKey();
                        String value = entry.getValue();
                        // validate before injecting
                        if (isValidHeaderName(name) && isValidHeaderValue(value)) {
                            headers.set(name, value);
                        } else {
                            log.warn("Blocked invalid header: {}", name);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Invalid custom headers for {}: {}",
                            endpoint.getUrl(), e.getMessage());
                }
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint.getUrl(),
                    HttpMethod.valueOf(endpoint.getHttpMethod()),
                    entity,
                    String.class
            );
            statusCode = response.getStatusCode().value();
            success = true;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // treat 4xx as a valid response — endpoint is up, just rejecting us
            statusCode = e.getStatusCode().value();
            success = true;
        } catch (Exception e) {
            statusCode = 503;
            success = false;
        }

        long latency = System.currentTimeMillis() - start;

        PingResult result = new PingResult(
                null, endpoint.getId(), statusCode, latency, success, LocalDateTime.now()
        );
        pingResultRepo.save(result);

        baselineCache.recordLatency(endpoint.getId(), latency);

        double baseline = baselineCache.getBaseline(endpoint.getId());

        if (anomalyDetector.isAnomaly(endpoint.getId(), latency,
                statusCode, endpoint.getExpectedStatus(), endpoint.getThresholdMs())) {
            log.warn("Anomaly detected on {}", endpoint.getUrl());
            alertService.sendAlert(endpoint, latency, statusCode, baseline);
        }
    }
}
