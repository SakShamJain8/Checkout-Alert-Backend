package com.checkoutalert.checkoutalert.repository;

import com.checkoutalert.checkoutalert.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, String> {
    List<Incident> findTop50ByUserIdOrderByDetectedAtDesc(String userId);
    List<Incident> findByEndpointIdOrderByDetectedAtDesc(String endpointId);
}
