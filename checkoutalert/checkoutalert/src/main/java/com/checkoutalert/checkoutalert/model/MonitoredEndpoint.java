package com.checkoutalert.checkoutalert.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
public class MonitoredEndpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String userId;
    private String name;
    private String url;
    private String httpMethod;
    private int expectedStatus;
    private int thresholdMs;
    private String alertEmail;
    private boolean active;
    private LocalDateTime createdAt;
    @Column(length = 2000)
    private String customHeaders;
    public MonitoredEndpoint() {}


    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getHttpMethod() { return httpMethod; }
    public int getExpectedStatus() { return expectedStatus; }
    public int getThresholdMs() { return thresholdMs; }
    public String getAlertEmail() { return alertEmail; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCustomHeaders() { return customHeaders; }


    public void setCustomHeaders(String customHeaders) { this.customHeaders = customHeaders; }
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setUrl(String url) { this.url = url; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public void setExpectedStatus(int expectedStatus) { this.expectedStatus = expectedStatus; }
    public void setThresholdMs(int thresholdMs) { this.thresholdMs = thresholdMs; }
    public void setAlertEmail(String alertEmail) { this.alertEmail = alertEmail; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
