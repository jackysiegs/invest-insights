package com.ii.backend.controller;

import com.ii.backend.model.Portfolio;
import com.ii.backend.model.Holding;
import com.ii.backend.repository.PortfolioRepository;
import com.ii.backend.repository.ClientRepository;
import com.ii.backend.repository.HoldingRepository;
import com.ii.backend.model.Client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@CrossOrigin(origins = "http://localhost:4200")
public class PortfolioController {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @GetMapping
    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAll();
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Portfolio>> getPortfoliosByClient(@PathVariable Long clientId) {
        List<Portfolio> portfolios = portfolioRepository.findByClientId(clientId);
        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/advisor/{advisorId}")
    public ResponseEntity<List<Portfolio>> getPortfoliosByAdvisor(@PathVariable Long advisorId) {
        List<Portfolio> portfolios = portfolioRepository.findByClientAdvisorId(advisorId);
        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<Portfolio> getPortfolioById(@PathVariable Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId).orElse(null);
        if (portfolio == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/{portfolioId}/holdings")
    public ResponseEntity<List<Holding>> getHoldingsByPortfolio(@PathVariable Long portfolioId) {
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        return ResponseEntity.ok(holdings);
    }

    @PostMapping
    public ResponseEntity<?> createPortfolio(@RequestBody Portfolio portfolio) {
        // Optional: validate that client exists
        if (portfolio.getClient() != null && portfolio.getClient().getId() != null) {
            Client client = clientRepository.findById(portfolio.getClient().getId()).orElse(null);
            if (client == null) {
                return ResponseEntity.badRequest().body("Client not found.");
            }
            portfolio.setClient(client);
        }

        Portfolio saved = portfolioRepository.save(portfolio);
        return ResponseEntity.ok(saved);
    }
}
