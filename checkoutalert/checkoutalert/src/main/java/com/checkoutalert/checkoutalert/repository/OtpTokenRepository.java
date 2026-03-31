package com.checkoutalert.checkoutalert.repository;

import com.checkoutalert.checkoutalert.model.OtpToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, String> {
    Optional<OtpToken> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String email, String purpose);
    @Transactional
    void deleteByEmail(String email);
}
