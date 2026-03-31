package com.checkoutalert.checkoutalert.repository;

import com.checkoutalert.checkoutalert.model.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistRepository extends JpaRepository<Waitlist, String> {
    boolean existsByEmail(String email);
}
