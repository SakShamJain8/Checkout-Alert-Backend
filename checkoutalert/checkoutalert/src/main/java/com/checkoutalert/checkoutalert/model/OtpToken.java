package com.checkoutalert.checkoutalert.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
public class OtpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String email;
    private String otp;
    private String purpose; // "REGISTER" or "LOGIN"
    private boolean used;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public OtpToken() {}

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getOtp() { return otp; }
    public String getPurpose() { return purpose; }
    public boolean isUsed() { return used; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setOtp(String otp) { this.otp = otp; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public void setUsed(boolean used) { this.used = used; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
