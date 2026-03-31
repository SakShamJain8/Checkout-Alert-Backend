package com.checkoutalert.checkoutalert.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Entity
@Data
public class PingResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String endpointId;
    private int statusCode;
    private long latencyMs;
    private boolean success;
    private LocalDateTime pingedAt;

    public PingResult() {}

    public PingResult(String id, String endpointId, int statusCode,
                      long latencyMs, boolean success, LocalDateTime pingedAt) {
        this.id = id;
        this.endpointId = endpointId;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.success = success;
        this.pingedAt = pingedAt;
    }

    public String getId() { return id; }
    public String getEndpointId() { return endpointId; }
    public int getStatusCode() { return statusCode; }
    public long getLatencyMs() { return latencyMs; }
    public boolean isSuccess() { return success; }
    public LocalDateTime getPingedAt() { return pingedAt; }

    public void setId(String id) { this.id = id; }
    public void setEndpointId(String endpointId) { this.endpointId = endpointId; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public void setSuccess(boolean success) { this.success = success; }
    public void setPingedAt(LocalDateTime pingedAt) { this.pingedAt = pingedAt; }
}
