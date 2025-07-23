package com.ii.backend.controller;

import com.ii.backend.model.Holding;
import com.ii.backend.model.Portfolio;
import com.ii.backend.repository.HoldingRepository;
import com.ii.backend.repository.PortfolioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holdings")
@CrossOrigin(origins = "http://localhost:4200")
public class HoldingController {

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @GetMapping("/portfolio/{portfolioId}")
    public List<Holding> getHoldingsByPortfolio(@PathVariable Long portfolioId) {
        return holdingRepository.findByPortfolioId(portfolioId);
    }

    @PostMapping
    public ResponseEntity<?> createHolding(@RequestBody Holding holding) {
        if (holding.getPortfolio() != null && holding.getPortfolio().getId() != null) {
            Portfolio portfolio = portfolioRepository.findById(holding.getPortfolio().getId()).orElse(null);
            if (portfolio == null) {
                return ResponseEntity.badRequest().body("Portfolio not found.");
            }
            holding.setPortfolio(portfolio);
        }

        Holding saved = holdingRepository.save(holding);
        return ResponseEntity.ok(saved);
    }
}
