package com.ii.backend.controller;

import com.ii.backend.repository.HoldingRepository;
import com.ii.backend.repository.PortfolioRepository;
import com.ii.backend.repository.ClientRepository;
import com.ii.backend.repository.AdvisorRepository;
import com.ii.backend.repository.InvestmentInsightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cleanup")
@CrossOrigin(origins = "http://localhost:4200")
public class CleanupController {

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AdvisorRepository advisorRepository;

    @Autowired
    private InvestmentInsightRepository investmentInsightRepository;

    @GetMapping("/holdings")
    public ResponseEntity<List<Object[]>> getAllHoldings() {
        // This will show all holdings with their portfolio info
        List<Object[]> holdings = holdingRepository.findAll().stream()
                .map(holding -> new Object[]{
                        holding.getId(),
                        holding.getTicker(),
                        holding.getShares(),
                        holding.getPricePerShare(),
                        holding.getPortfolio() != null ? holding.getPortfolio().getId() : "NULL"
                })
                .toList();
        return ResponseEntity.ok(holdings);
    }

    @DeleteMapping("/holdings/{id}")
    public ResponseEntity<String> deleteHolding(@PathVariable Long id) {
        if (holdingRepository.existsById(id)) {
            holdingRepository.deleteById(id);
            return ResponseEntity.ok("Holding with ID " + id + " deleted successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/holdings/portfolio/{portfolioId}")
    public ResponseEntity<String> deleteHoldingsByPortfolio(@PathVariable Long portfolioId) {
        List<com.ii.backend.model.Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        if (!holdings.isEmpty()) {
            holdingRepository.deleteAll(holdings);
            return ResponseEntity.ok("Deleted " + holdings.size() + " holdings for portfolio " + portfolioId);
        } else {
            return ResponseEntity.ok("No holdings found for portfolio " + portfolioId);
        }
    }

    @DeleteMapping("/holdings/all")
    public ResponseEntity<String> deleteAllHoldings() {
        long count = holdingRepository.count();
        holdingRepository.deleteAll();
        return ResponseEntity.ok("Deleted all " + count + " holdings");
    }

    @DeleteMapping("/portfolios/all")
    public ResponseEntity<String> deleteAllPortfolios() {
        long count = portfolioRepository.count();
        portfolioRepository.deleteAll();
        return ResponseEntity.ok("Deleted all " + count + " portfolios");
    }

    @DeleteMapping("/clients/all")
    public ResponseEntity<String> deleteAllClients() {
        long count = clientRepository.count();
        clientRepository.deleteAll();
        return ResponseEntity.ok("Deleted all " + count + " clients");
    }

    @DeleteMapping("/advisors/all")
    public ResponseEntity<String> deleteAllAdvisors() {
        long count = advisorRepository.count();
        advisorRepository.deleteAll();
        return ResponseEntity.ok("Deleted all " + count + " advisors");
    }

    @DeleteMapping("/insights/all")
    public ResponseEntity<String> deleteAllInsights() {
        long count = investmentInsightRepository.count();
        investmentInsightRepository.deleteAll();
        return ResponseEntity.ok("Deleted all " + count + " investment insights");
    }

    @DeleteMapping("/everything")
    public ResponseEntity<String> deleteEverything() {
        // Delete in order to respect foreign key constraints
        long holdingsCount = holdingRepository.count();
        long portfoliosCount = portfolioRepository.count();
        long clientsCount = clientRepository.count();
        long advisorsCount = advisorRepository.count();
        long insightsCount = investmentInsightRepository.count();

        holdingRepository.deleteAll();
        portfolioRepository.deleteAll();
        investmentInsightRepository.deleteAll();
        clientRepository.deleteAll();
        advisorRepository.deleteAll();

        String message = String.format("Deleted all data: %d holdings, %d portfolios, %d clients, %d advisors, %d insights", 
                holdingsCount, portfoliosCount, clientsCount, advisorsCount, insightsCount);
        
        return ResponseEntity.ok(message);
    }

    @GetMapping("/status")
    public ResponseEntity<String> getDatabaseStatus() {
        long holdingsCount = holdingRepository.count();
        long portfoliosCount = portfolioRepository.count();
        long clientsCount = clientRepository.count();
        long advisorsCount = advisorRepository.count();
        long insightsCount = investmentInsightRepository.count();

        String status = String.format("Database Status: %d holdings, %d portfolios, %d clients, %d advisors, %d insights", 
                holdingsCount, portfoliosCount, clientsCount, advisorsCount, insightsCount);
        
        return ResponseEntity.ok(status);
    }
} 