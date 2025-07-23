package com.ii.backend.repository;

import com.ii.backend.model.Advisor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdvisorRepository extends JpaRepository<Advisor, Long> {
    Optional<Advisor> findByEmail(String email);
    Optional<Advisor> findByUsername(String username);
}
