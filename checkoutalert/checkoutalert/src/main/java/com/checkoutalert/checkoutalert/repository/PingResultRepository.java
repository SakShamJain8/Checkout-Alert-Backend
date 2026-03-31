package com.checkoutalert.checkoutalert.repository;

import com.checkoutalert.checkoutalert.model.PingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PingResultRepository extends JpaRepository<PingResult, String> {
    List<PingResult> findTop10ByEndpointIdOrderByPingedAtDesc(String endpointId);
    // count total pings for an endpoint
    long countByEndpointId(String endpointId);

    // count successful pings for an endpoint
    long countByEndpointIdAndSuccessTrue(String endpointId);

}
