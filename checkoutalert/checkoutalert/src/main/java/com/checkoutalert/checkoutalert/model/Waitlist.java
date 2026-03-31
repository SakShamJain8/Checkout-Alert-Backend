package com.checkoutalert.checkoutalert.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist")
public class Waitlist {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true)
    private String email;
    private String name;
    private LocalDateTime joinedAt;

    public Waitlist() {}

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public LocalDateTime getJoinedAt() { return joinedAt; }

    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setName(String name) { this.name = name; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
