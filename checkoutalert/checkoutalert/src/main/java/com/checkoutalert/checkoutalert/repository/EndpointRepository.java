package com.checkoutalert.checkoutalert.repository;

import com.checkoutalert.checkoutalert.model.MonitoredEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EndpointRepository extends JpaRepository<MonitoredEndpoint, String> {
    List<MonitoredEndpoint> findByUserId(String userId);

    // only return active endpoints for this user
    List<MonitoredEndpoint> findByUserIdAndActiveTrue(String userId);

    // check ownership before delete/toggle
    Optional<MonitoredEndpoint> findByIdAndUserId(String id, String userId);
    List<MonitoredEndpoint> findAllByActiveTrue();
}
