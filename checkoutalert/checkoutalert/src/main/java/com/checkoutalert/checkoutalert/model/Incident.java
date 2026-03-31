package com.checkoutalert.checkoutalert.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String endpointId;
    private String endpointName;
    private String endpointUrl;
    private int statusCode;
    private long latencyMs;
    private double baselineMs;

    @Column(length = 1000)
    private String geminiDiagnosis;

    private LocalDateTime detectedAt;
    private String userId;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Incident() {}

    public String getId() { return id; }
    public String getEndpointId() { return endpointId; }
    public String getEndpointName() { return endpointName; }
    public String getEndpointUrl() { return endpointUrl; }
    public int getStatusCode() { return statusCode; }
    public long getLatencyMs() { return latencyMs; }
    public double getBaselineMs() { return baselineMs; }
    public String getGeminiDiagnosis() { return geminiDiagnosis; }
    public LocalDateTime getDetectedAt() { return detectedAt; }

    public void setId(String id) { this.id = id; }
    public void setEndpointId(String endpointId) { this.endpointId = endpointId; }
    public void setEndpointName(String endpointName) { this.endpointName = endpointName; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public void setBaselineMs(double baselineMs) { this.baselineMs = baselineMs; }
    public void setGeminiDiagnosis(String geminiDiagnosis) { this.geminiDiagnosis = geminiDiagnosis; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
}
