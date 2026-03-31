package com.checkoutalert.checkoutalert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public class AnomalyDetectorService {
    private final BaselineCacheService baselineCache;
    public AnomalyDetectorService(BaselineCacheService baselineCache) {
        this.baselineCache = baselineCache;
    }
    private static final double SPIKE_MULTIPLIER = 3.0;

    public boolean isAnomaly(String endpointId, long latencyMs,
                             int statusCode, int expectedStatus, int thresholdMs) {
        // check 1 — wrong status code
        if (statusCode != expectedStatus) return true;

        // check 2 — exceeds configured threshold
        if (latencyMs > thresholdMs) return true;

        // check 3 — latency spike vs baseline
        double baseline = baselineCache.getBaseline(endpointId);
        if (baseline > 0 && latencyMs > baseline * SPIKE_MULTIPLIER) return true;

        return false;
    }
}
