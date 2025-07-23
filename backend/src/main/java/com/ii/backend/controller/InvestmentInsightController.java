package com.ii.backend.controller;

import com.ii.backend.model.InvestmentInsight;
import com.ii.backend.repository.InvestmentInsightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ii.backend.service.InvestmentInsightService;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "http://localhost:4200")
public class InvestmentInsightController {

    @Autowired
    private InvestmentInsightRepository insightRepository;

    @Autowired
    private InvestmentInsightService insightService;

    @GetMapping
    public List<InvestmentInsight> getAllInsights() {
        return insightRepository.findAll();
    }

    @PostMapping
    public InvestmentInsight createInsight(@RequestBody InvestmentInsight insight) {
        insight.setCreatedAt(java.time.LocalDateTime.now());
        return insightRepository.save(insight);
    }

    @PostMapping("/generate-manual")
    public InvestmentInsight generateInsightFromAI(
            @RequestParam List<String> holdings,
            @RequestParam String preferences,
            @RequestParam Long clientId
    ) {
        return insightService.generateInsight(holdings, preferences, clientId);
    }

    @PostMapping("/generate-portfolio/{portfolioId}")
    public ResponseEntity<?> generateInsightFromPortfolio(
        @PathVariable Long portfolioId,
        @RequestParam String clientId,
        @RequestBody Map<String, String> body
    ) {
        String preferences = body.get("preferences");
        try {
            Long clientIdLong = Long.parseLong(clientId);
            InvestmentInsight insight = insightService.generateInsightForPortfolio(portfolioId, preferences, clientIdLong);
            return ResponseEntity.ok(insight);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid client ID format: " + clientId);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<InvestmentInsight>> getInsightsByClient(@PathVariable Long clientId) {
        try {
            List<InvestmentInsight> insights = insightService.getInsightsByClient(clientId);
            return ResponseEntity.ok(insights);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<InvestmentInsight>> getInsightsByPortfolio(@PathVariable Long portfolioId) {
        try {
            // Get the portfolio to find the client, then get insights for that client
            return insightService.getInsightsByPortfolio(portfolioId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{insightId}")
    public ResponseEntity<InvestmentInsight> getInsightById(@PathVariable Long insightId) {
        try {
            InvestmentInsight insight = insightService.getInsightById(insightId);
            return ResponseEntity.ok(insight);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{insightId}")
    public ResponseEntity<Void> deleteInsight(@PathVariable Long insightId) {
        try {
            insightService.deleteInsight(insightId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllInsights() {
        try {
            insightRepository.deleteAll();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
