package com.checkoutalert.checkoutalert.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class BaselineCacheService {
    private final RedisTemplate<String, String> redisTemplate;
    
    public BaselineCacheService(Optional<RedisTemplate<String, String>> redisTemplate) {
        this.redisTemplate = redisTemplate.orElse(null);
    }
    private static final int WINDOW_SIZE = 20; // keep last 20 latencies per endpoint

    // called after every ping — stores latency in Redis list
    public void recordLatency(String endpointId, long latencyMs) {
        if (redisTemplate == null) return;
        try {
            String key = "baseline:" + endpointId;
            redisTemplate.opsForList().rightPush(key, String.valueOf(latencyMs));
            redisTemplate.opsForList().trim(key, -WINDOW_SIZE, -1); // keep only last 20
            redisTemplate.expire(key, Duration.ofDays(7));
        } catch (Exception e) {
            // Redis unavailable - silently ignore
        }
    }

    // returns average latency from last 20 pings
    public double getBaseline(String endpointId) {
        if (redisTemplate == null) return -1;
        try {
            String key = "baseline:" + endpointId;
            List<String> values = redisTemplate.opsForList().range(key, 0, -1);
            if (values == null || values.size() < 5) return -1; // not enough data yet
            return values.stream()
                    .mapToLong(Long::parseLong)
                    .average()
                    .orElse(-1);
        } catch (Exception e) {
            // Redis unavailable - return no baseline
            return -1;
        }
    }
}
