package com.ii.backend.repository;

import com.ii.backend.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByAdvisorId(Long advisorId);
}
