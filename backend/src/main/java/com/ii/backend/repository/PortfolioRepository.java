package com.ii.backend.repository;

import com.ii.backend.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    
    List<Portfolio> findByClientId(Long clientId);
    
    @Query("SELECT p FROM Portfolio p WHERE p.client.advisor.id = :advisorId")
    List<Portfolio> findByClientAdvisorId(@Param("advisorId") Long advisorId);
}
