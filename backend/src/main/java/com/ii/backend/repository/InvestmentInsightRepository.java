package com.ii.backend.repository;

import com.ii.backend.model.InvestmentInsight;
import com.ii.backend.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvestmentInsightRepository extends JpaRepository<InvestmentInsight, Long> {
    List<InvestmentInsight> findByClientOrderByCreatedAtDesc(Client client);
}
