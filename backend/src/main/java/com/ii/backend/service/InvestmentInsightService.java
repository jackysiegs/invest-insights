package com.ii.backend.service;

import com.ii.backend.model.InvestmentInsight;
import com.ii.backend.model.Client;
import com.ii.backend.model.Holding;
import com.ii.backend.model.Portfolio;
import com.ii.backend.repository.InvestmentInsightRepository;
import com.ii.backend.repository.ClientRepository;
import com.ii.backend.repository.HoldingRepository;
import com.ii.backend.repository.PortfolioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class InvestmentInsightService {

    private final InvestmentInsightRepository insightRepository;
    private final ClientRepository clientRepository;
    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final WebClient webClient;

    @Autowired
    public InvestmentInsightService(InvestmentInsightRepository insightRepository, ClientRepository clientRepository, HoldingRepository holdingRepository, PortfolioRepository portfolioRepository) {
        this.insightRepository = insightRepository;
        this.clientRepository = clientRepository;
        this.holdingRepository = holdingRepository;
        this.portfolioRepository = portfolioRepository;
        this.webClient = WebClient.create("http://localhost:8000"); // FastAPI address
    }

    public InvestmentInsight generateInsight(List<String> holdings, String preferences, Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientId));
    

        Mono<AIResponse> response = webClient.post()
                .uri("/generate-insight")
                .bodyValue(new InsightRequest(holdings, preferences))
                .retrieve()
                .bodyToMono(AIResponse.class);
    
        AIResponse aiResponse = response.block(); // Wait for result
    
        if (aiResponse != null) {
            InvestmentInsight aiInsight = new InvestmentInsight();
            aiInsight.setSummary(aiResponse.summary);
            aiInsight.setAiGeneratedText(aiResponse.aiGeneratedText);
            // Parse the timestamp from the AI service
            try {
                // Try multiple formats to handle different timestamp formats
                LocalDateTime parsedDateTime = null;
                String[] formats = {
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
                };
                
                for (String format : formats) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                        parsedDateTime = LocalDateTime.parse(aiResponse.createdAt, formatter);
                        break;
                    } catch (Exception e) {
                        // Continue to next format
                    }
                }
                
                if (parsedDateTime != null) {
                    aiInsight.setCreatedAt(parsedDateTime);
                } else {
                    // If all parsing fails, use current time
                    System.err.println("Failed to parse timestamp: " + aiResponse.createdAt + ", using current time");
                    aiInsight.setCreatedAt(LocalDateTime.now(java.time.ZoneId.of("America/New_York")));
                }
            } catch (Exception e) {
                System.err.println("Error parsing timestamp: " + aiResponse.createdAt + ", using current time. Error: " + e.getMessage());
                aiInsight.setCreatedAt(LocalDateTime.now(java.time.ZoneId.of("America/New_York")));
            }
            aiInsight.setClient(client);
            
            // Handle structured insight data (qualitative only)
            if (aiResponse.structuredInsight != null) {
                // Store qualitative insights and recommendations
                // Quantitative scores will be calculated mathematically in the backend
                
                // Convert structured data to JSON string
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String structuredDataJson = mapper.writeValueAsString(aiResponse.structuredInsight);
                    aiInsight.setStructuredData(structuredDataJson);
                } catch (Exception e) {
                    // If JSON conversion fails, store raw response
                    aiInsight.setStructuredData(aiResponse.structuredInsight.rawResponse);
                }
            }
            
            return insightRepository.save(aiInsight);
        }
    
        throw new RuntimeException("Failed to generate investment insight");
    }

    public InvestmentInsight generateInsightForPortfolio(Long portfolioId, String preferences, Long clientId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found with id: " + portfolioId));
        
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        if (holdings.isEmpty()) {
            throw new RuntimeException("No holdings found for this portfolio.");
        }

        // Get client information
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientId));
        String risk = client.getRiskTolerance();
        String goals = client.getInvestmentGoals();
        Integer yearsUntilGoal = client.getYearsUntilGoal();
        BigDecimal annualIncomeGoal = client.getAnnualIncomeGoal();
        String accountType = portfolio.getAccountType();

        // Calculate portfolio metrics
        BigDecimal totalValue = BigDecimal.ZERO;
        for (Holding holding : holdings) {
            BigDecimal holdingValue = holding.getPricePerShare().multiply(BigDecimal.valueOf(holding.getShares()));
            totalValue = totalValue.add(holdingValue);
        }

        // Build holdings breakdown with enhanced info
        StringBuilder holdingsSummary = new StringBuilder();
        for (Holding holding : holdings) {
            BigDecimal holdingValue = holding.getPricePerShare().multiply(BigDecimal.valueOf(holding.getShares()));
            BigDecimal percentage = holdingValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            holdingsSummary.append(String.format("• %s (%s/%s): %d shares @ $%.2f = $%,.2f (%.1f%% of portfolio)", 
                    holding.getTicker(), 
                    holding.getSector() != null && !holding.getSector().isEmpty() ? holding.getSector() : "Unknown Sector",
                    holding.getAssetType() != null && !holding.getAssetType().isEmpty() ? holding.getAssetType() : "Unknown Type",
                    holding.getShares(), 
                    holding.getPricePerShare(),
                    holdingValue,
                    percentage.doubleValue()));
            
            // Add risk metrics if available
            if (holding.getBeta() != null) {
                holdingsSummary.append(String.format(" [Beta: %.2f]", holding.getBeta()));
            }
            if (holding.getDividendYield() != null) {
                holdingsSummary.append(String.format(" [Yield: %.2f%%]", holding.getDividendYield()));
            }
            holdingsSummary.append("\n");
        }

        // Calculate sector exposure
        Map<String, BigDecimal> sectorExposure = holdings.stream()
                .filter(h -> h.getSector() != null && !h.getSector().isEmpty())
                .collect(Collectors.groupingBy(
                    Holding::getSector,
                    Collectors.reducing(BigDecimal.ZERO, 
                        h -> h.getPricePerShare().multiply(BigDecimal.valueOf(h.getShares())), 
                        BigDecimal::add)
                ));

        StringBuilder sectorSummary = new StringBuilder();
        if (sectorExposure.isEmpty()) {
            sectorSummary.append("No sector data available for holdings\n");
        } else {
            for (Map.Entry<String, BigDecimal> entry : sectorExposure.entrySet()) {
                BigDecimal percentage = entry.getValue().divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                sectorSummary.append(String.format("• %s: %.1f%% ($%,.2f)\n", 
                        entry.getKey(), 
                        percentage.doubleValue(),
                        entry.getValue()));
            }
        }

        // Calculate asset class breakdown
        Map<String, BigDecimal> assetClassExposure = holdings.stream()
                .filter(h -> h.getAssetType() != null && !h.getAssetType().isEmpty())
                .collect(Collectors.groupingBy(
                    Holding::getAssetType,
                    Collectors.reducing(BigDecimal.ZERO, 
                        h -> h.getPricePerShare().multiply(BigDecimal.valueOf(h.getShares())), 
                        BigDecimal::add)
                ));

        StringBuilder assetClassSummary = new StringBuilder();
        if (assetClassExposure.isEmpty()) {
            assetClassSummary.append("No asset class data available for holdings\n");
        } else {
            for (Map.Entry<String, BigDecimal> entry : assetClassExposure.entrySet()) {
                BigDecimal percentage = entry.getValue().divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                assetClassSummary.append(String.format("• %s: %.1f%% ($%,.2f)\n", 
                        entry.getKey(), 
                        percentage.doubleValue(),
                        entry.getValue()));
            }
        }

        // Calculate risk metrics
        BigDecimal totalDividendIncome = BigDecimal.ZERO;
        BigDecimal weightedBeta = BigDecimal.ZERO;
        BigDecimal totalBetaWeight = BigDecimal.ZERO;
        int holdingsWithBeta = 0;
        int holdingsWithYield = 0;

        for (Holding holding : holdings) {
            BigDecimal holdingValue = holding.getPricePerShare().multiply(BigDecimal.valueOf(holding.getShares()));
            
            if (holding.getBeta() != null) {
                weightedBeta = weightedBeta.add(holdingValue.multiply(BigDecimal.valueOf(holding.getBeta())));
                totalBetaWeight = totalBetaWeight.add(holdingValue);
                holdingsWithBeta++;
            }
            
            if (holding.getDividendYield() != null) {
                BigDecimal annualDividend = holdingValue.multiply(BigDecimal.valueOf(holding.getDividendYield() / 100));
                totalDividendIncome = totalDividendIncome.add(annualDividend);
                holdingsWithYield++;
            }
        }

        BigDecimal portfolioBeta = holdingsWithBeta > 0 ? weightedBeta.divide(totalBetaWeight, 2, RoundingMode.HALF_UP) : null;
        BigDecimal averageDividendYield = holdingsWithYield > 0 ? 
            totalDividendIncome.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : null;

        // Build risk metrics summary
        StringBuilder riskMetricsSummary = new StringBuilder();
        if (portfolioBeta != null) {
            riskMetricsSummary.append(String.format("• Portfolio Beta: %.2f (%s)\n", 
                portfolioBeta.doubleValue(),
                portfolioBeta.doubleValue() > 1.1 ? "more volatile than market" : 
                portfolioBeta.doubleValue() < 0.9 ? "less volatile than market" : "similar to market"));
        }
        if (averageDividendYield != null) {
            riskMetricsSummary.append(String.format("• Average Dividend Yield: %.2f%%\n", averageDividendYield.doubleValue()));
        }
        if (totalDividendIncome.compareTo(BigDecimal.ZERO) > 0) {
            riskMetricsSummary.append(String.format("• Annual Dividend Income: $%,.2f\n", totalDividendIncome));
        }

        // Extract tickers for the AI service
        List<String> tickers = holdings.stream()
                .map(Holding::getTicker)
                .toList();

        // Create qualitative analysis prompt
        String enhancedPreferences = String.format(
            "=== CLIENT PROFILE ===\n" +
            "Risk Tolerance: %s\n" +
            "Investment Goals: %s\n" +
            "Time Horizon: %s\n" +
            "Annual Income Goal: %s\n\n" +
            "=== PORTFOLIO METRICS ===\n" +
            "Total Portfolio Value: $%,.2f\n" +
            "Number of Holdings: %d\n" +
            "Account Type: %s\n\n" +
            "=== ASSET CLASS BREAKDOWN ===\n%s\n" +
            "=== SECTOR BREAKDOWN ===\n%s\n" +
            "=== RISK METRICS ===\n%s\n" +
            "=== INDIVIDUAL HOLDING WEIGHTS ===\n%s\n" +
            "=== QUALITATIVE ANALYSIS REQUEST ===\n" +
            "Provide a comprehensive qualitative investment analysis using the specific numbers above. Focus on:\n" +
            "1. Portfolio insights and strategy assessment\n" +
            "2. Risk factors and concerns (concentration, sector exposure, volatility)\n" +
            "3. Diversification strengths and gaps\n" +
            "4. Goal alignment and recommendations\n" +
            "5. Market context and opportunities\n" +
            "6. Specific actionable recommendations\n\n" +
            "IMPORTANT: Reference specific numbers, percentages, and dollar amounts in your analysis. " +
            "Provide insights and recommendations rather than quantitative scoring.\n\n" +
            "Format your response with these sections:\n" +
            "- SUMMARY (one line, max 80 characters - the key insight or action item)\n" +
            "- Portfolio Insights (qualitative analysis of the portfolio)\n" +
            "- Risk Assessment (key risk factors and concerns)\n" +
            "- Diversification Analysis (strengths and gaps)\n" +
            "- Goal Alignment (how well it matches objectives)\n" +
            "- Market Context (current conditions and opportunities)\n" +
            "- Specific Recommendations (actionable advice)\n" +
            "- Next Steps (immediate actions to take)",
            risk, goals, 
            yearsUntilGoal != null ? yearsUntilGoal + " years until goal" : "Not specified",
            annualIncomeGoal != null ? "$" + annualIncomeGoal.toString() : "Not specified",
            totalValue, holdings.size(), 
            accountType != null && !accountType.isEmpty() ? accountType : "Not specified",
            assetClassSummary.toString(), sectorSummary.toString(), riskMetricsSummary.toString(), holdingsSummary.toString()
        );

        InvestmentInsight insight = generateInsight(tickers, enhancedPreferences, clientId);
        insight.setPortfolioName(portfolio.getName()); // Set the portfolio name
        

        
        // Save the insight with portfolio name BEFORE calculating scores
        insight = insightRepository.save(insight);
        

        
        // Calculate mathematical scores and update the insight
        calculateMathematicalScores(insight, holdings, totalValue, client);
        
        // Save the updated insight with hybrid scores back to the database
        insight = insightRepository.save(insight);
        
        return insight;
    }

    public List<InvestmentInsight> getInsightsByClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientId));
        List<InvestmentInsight> insights = insightRepository.findByClientOrderByCreatedAtDesc(client);
        

        
        return insights;
    }

    public ResponseEntity<List<InvestmentInsight>> getInsightsByPortfolio(Long portfolioId) {
        try {
            // Get the portfolio to find the client
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                    .orElseThrow(() -> new RuntimeException("Portfolio not found with id: " + portfolioId));
            
            // Get insights for the client who owns this portfolio
            List<InvestmentInsight> insights = insightRepository.findByClientOrderByCreatedAtDesc(portfolio.getClient());
            

            
            return ResponseEntity.ok(insights);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    public InvestmentInsight getInsightById(Long insightId) {
        return insightRepository.findById(insightId)
                .orElseThrow(() -> new RuntimeException("Insight not found with id: " + insightId));
    }

    public void deleteInsight(Long insightId) {
        InvestmentInsight insight = getInsightById(insightId);
        insightRepository.delete(insight);
    }

    /**
     * Calculate mathematical scores and apply AI adjustments for hybrid scoring
     */
    private void calculateMathematicalScores(InvestmentInsight insight, List<Holding> holdings, 
                                           BigDecimal totalValue, Client client) {
        
        // Calculate mathematical base scores
        int baseRiskScore = calculateRiskScore(holdings, totalValue, client.getRiskTolerance());
        int baseDiversificationScore = calculateDiversificationScore(holdings, totalValue);
        int baseGoalAlignmentScore = calculateGoalAlignmentScore(holdings, totalValue, client);
        
        // Debug: Show mathematical calculations
        System.out.println("=== MATHEMATICAL SCORE CALCULATION DEBUG ===");
        System.out.println("Base Mathematical Risk Score: " + baseRiskScore);
        System.out.println("Base Mathematical Diversification Score: " + baseDiversificationScore);
        System.out.println("Base Mathematical Goal Alignment Score: " + baseGoalAlignmentScore);
        System.out.println("=============================================");
        
        // Apply AI adjustments if available
        int finalRiskScore = baseRiskScore;
        int finalDiversificationScore = baseDiversificationScore;
        int finalGoalAlignmentScore = baseGoalAlignmentScore;
        StructuredInsight structuredInsight = null;
        
        if (insight.getStructuredData() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                structuredInsight = mapper.readValue(insight.getStructuredData(), StructuredInsight.class);
                
                if (structuredInsight.scoreAdjustments != null) {
                    // Apply risk score adjustment
                    if (structuredInsight.scoreAdjustments.containsKey("risk")) {
                        Map<String, String> riskAdjustment = structuredInsight.scoreAdjustments.get("risk");
                        String adjustmentStr = riskAdjustment.get("adjustment");
                        if (adjustmentStr != null) {
                            int adjustment = Integer.parseInt(adjustmentStr);
                            finalRiskScore = Math.max(1, Math.min(10, baseRiskScore + adjustment));
                            System.out.println("AI Risk Adjustment: " + adjustment + " (Reasoning: " + riskAdjustment.get("reasoning") + ")");
                        }
                    }
                    
                    // Apply diversification score adjustment
                    if (structuredInsight.scoreAdjustments.containsKey("diversification")) {
                        Map<String, String> divAdjustment = structuredInsight.scoreAdjustments.get("diversification");
                        String adjustmentStr = divAdjustment.get("adjustment");
                        if (adjustmentStr != null) {
                            int adjustment = Integer.parseInt(adjustmentStr.replace("%", ""));
                            // Allow larger adjustments up to ±10%
                            finalDiversificationScore = Math.max(1, Math.min(100, baseDiversificationScore + adjustment));
                            System.out.println("AI Diversification Adjustment: " + adjustment + "% (Reasoning: " + divAdjustment.get("reasoning") + ")");
                        }
                    }
                    
                    // Apply goal alignment score adjustment
                    if (structuredInsight.scoreAdjustments.containsKey("goalAlignment")) {
                        Map<String, String> goalAdjustment = structuredInsight.scoreAdjustments.get("goalAlignment");
                        String adjustmentStr = goalAdjustment.get("adjustment");
                        if (adjustmentStr != null) {
                            int adjustment = Integer.parseInt(adjustmentStr.replace("%", ""));
                            // Allow larger adjustments up to ±10%
                            finalGoalAlignmentScore = Math.max(1, Math.min(100, baseGoalAlignmentScore + adjustment));
                            System.out.println("AI Goal Alignment Adjustment: " + adjustment + "% (Reasoning: " + goalAdjustment.get("reasoning") + ")");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing AI score adjustments: " + e.getMessage());
                // Fall back to base scores only
            }
        }
        
        // Debug: Show hybrid calculations
        System.out.println("=== HYBRID SCORE CALCULATION DEBUG ===");
        System.out.println("Base Risk Score: " + baseRiskScore + " -> Final Risk Score: " + finalRiskScore);
        System.out.println("Base Diversification Score: " + baseDiversificationScore + " -> Final Diversification Score: " + finalDiversificationScore);
        System.out.println("Base Goal Alignment Score: " + baseGoalAlignmentScore + " -> Final Goal Alignment Score: " + finalGoalAlignmentScore);
        System.out.println("=====================================");
        
        // Store debug information in the insight for frontend display
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("baseRiskScore", baseRiskScore);
        debugInfo.put("baseDiversificationScore", baseDiversificationScore);
        debugInfo.put("baseGoalAlignmentScore", baseGoalAlignmentScore);
        debugInfo.put("finalRiskScore", finalRiskScore);
        debugInfo.put("finalDiversificationScore", finalDiversificationScore);
        debugInfo.put("finalGoalAlignmentScore", finalGoalAlignmentScore);
        debugInfo.put("aiAdjustments", structuredInsight != null ? structuredInsight.scoreAdjustments : null);
        
        // Add portfolio statistics for verification
        Map<String, Object> portfolioStats = new HashMap<>();
        portfolioStats.put("totalValue", totalValue);
        portfolioStats.put("holdingsCount", holdings.size());
        portfolioStats.put("clientRiskTolerance", client.getRiskTolerance());
        portfolioStats.put("clientGoal", client.getInvestmentGoals());
        portfolioStats.put("clientTimeHorizon", client.getYearsUntilGoal());
        
        // Calculate sector distribution
        Map<String, BigDecimal> sectorDistribution = new HashMap<>();
        for (Holding holding : holdings) {
            String sector = holding.getSector();
            BigDecimal value = new BigDecimal(holding.getShares()).multiply(holding.getPricePerShare());
            sectorDistribution.merge(sector, value, BigDecimal::add);
        }
        portfolioStats.put("sectorDistribution", sectorDistribution);
        
        // Calculate top holdings
        List<Map<String, Object>> topHoldings = holdings.stream()
            .sorted((h1, h2) -> new BigDecimal(h2.getShares()).multiply(h2.getPricePerShare())
                .compareTo(new BigDecimal(h1.getShares()).multiply(h1.getPricePerShare())))
            .limit(5)
            .map(holding -> {
                Map<String, Object> holdingInfo = new HashMap<>();
                holdingInfo.put("symbol", holding.getTicker());
                holdingInfo.put("sector", holding.getSector());
                holdingInfo.put("shares", holding.getShares());
                holdingInfo.put("pricePerShare", holding.getPricePerShare());
                holdingInfo.put("value", new BigDecimal(holding.getShares()).multiply(holding.getPricePerShare()));
                holdingInfo.put("percentage", new BigDecimal(holding.getShares()).multiply(holding.getPricePerShare())
                    .divide(totalValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")));
                return holdingInfo;
            })
            .collect(Collectors.toList());
        portfolioStats.put("topHoldings", topHoldings);
        
        debugInfo.put("portfolioStats", portfolioStats);
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            insight.setDebugInfo(mapper.writeValueAsString(debugInfo));
        } catch (Exception e) {
            System.err.println("Error storing debug info: " + e.getMessage());
        }
        
        // Update the insight with hybrid scores
        insight.setRiskScore(finalRiskScore);
        insight.setDiversificationScore(finalDiversificationScore);
        insight.setGoalAlignment(finalGoalAlignmentScore);
        
        // Validate and potentially adjust asset recommendation priorities
        if (structuredInsight != null && structuredInsight.assetRecommendations != null) {
            validateAssetRecommendationPriorities(structuredInsight.assetRecommendations, holdings, client);
        }
        
        // Debug: Log the final scores being saved
        System.out.println("=== FINAL SCORES BEING SAVED ===");
        System.out.println("Risk Score: " + finalRiskScore);
        System.out.println("Diversification Score: " + finalDiversificationScore);
        System.out.println("Goal Alignment: " + finalGoalAlignmentScore);
        System.out.println("=================================");
    }
    


    private int calculateRiskScore(List<Holding> holdings, BigDecimal totalValue, String riskTolerance) {
        // Base risk score based on risk tolerance
        double riskScore;
        switch (riskTolerance.toLowerCase()) {
            case "conservative":
                riskScore = 3.0;
                break;
            case "aggressive":
                riskScore = 7.0;
                break;
            default: // moderate
                riskScore = 5.0;
                break;
        }
        
        // Calculate portfolio beta (weighted average)
        BigDecimal weightedBeta = BigDecimal.ZERO;
        BigDecimal totalBetaWeight = BigDecimal.ZERO;
        
        for (Holding holding : holdings) {
            if (holding.getBeta() != null) {
                BigDecimal holdingValue = holding.getPricePerShare().multiply(BigDecimal.valueOf(holding.getShares()));
                weightedBeta = weightedBeta.add(holdingValue.multiply(BigDecimal.valueOf(holding.getBeta())));
                totalBetaWeight = totalBetaWeight.add(holdingValue);
            }
        }
        
        double portfolioBeta = 1.0; // Default beta
        if (totalBetaWeight.compareTo(BigDecimal.ZERO) > 0) {
            portfolioBeta = weightedBeta.divide(totalBetaWeight, 2, RoundingMode.HALF_UP).doubleValue();
        }
        
        // Concentration adjustments (more granular)
        for (Holding holding : holdings) {
            BigDecimal holdingValue = holding.getPricePerShare().multiply(BigDecimal.valueOf(holding.getShares()));
            double percentage = holdingValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            
            // Check if this is an ETF (less risky than individual stocks)
            boolean isETF = holding.getAssetType() != null && 
                (holding.getAssetType().toLowerCase().contains("etf") || 
                 holding.getAssetType().toLowerCase().contains("fund"));
            
            if (isETF) {
                // ETFs get reduced concentration penalties
                if (percentage > 50) riskScore += 1.5;      // Extreme concentration (was 3.0)
                else if (percentage > 40) riskScore += 1.0; // Very concentrated (was 2.0)
                else if (percentage > 30) riskScore += 0.75; // Highly concentrated (was 1.5)
                else if (percentage > 20) riskScore += 0.5; // Moderately concentrated (was 1.0)
                else if (percentage > 15) riskScore += 0.25; // Somewhat concentrated (was 0.5)
            } else {
                // Individual stocks get full concentration penalties
                if (percentage > 50) riskScore += 3.0;      // Extreme concentration
                else if (percentage > 40) riskScore += 2.0; // Very concentrated
                else if (percentage > 30) riskScore += 1.5; // Highly concentrated
                else if (percentage > 20) riskScore += 1.0; // Moderately concentrated
                else if (percentage > 15) riskScore += 0.5; // Somewhat concentrated
            }
        }
        
        // Sector concentration adjustments
        Map<String, BigDecimal> sectorExposure = holdings.stream()
                .filter(h -> h.getSector() != null && !h.getSector().isEmpty())
                .collect(Collectors.groupingBy(
                    Holding::getSector,
                    Collectors.reducing(BigDecimal.ZERO, 
                        h -> h.getPricePerShare().multiply(BigDecimal.valueOf(h.getShares())), 
                        BigDecimal::add)
                ));
        
        for (Map.Entry<String, BigDecimal> entry : sectorExposure.entrySet()) {
            double percentage = entry.getValue().divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            
            // Check if this sector is primarily ETFs (less risky)
            String sectorName = entry.getKey();
            boolean isETFSector = holdings.stream()
                .filter(h -> h.getSector() != null && h.getSector().equals(sectorName))
                .allMatch(h -> h.getAssetType() != null && 
                    (h.getAssetType().toLowerCase().contains("etf") || 
                     h.getAssetType().toLowerCase().contains("fund")));
            
            if (isETFSector) {
                // ETF sectors get reduced risk penalties
                if (percentage > 70) riskScore += 1.0;      // Extreme sector concentration (was 2.0)
                else if (percentage > 60) riskScore += 0.75; // Very concentrated sector (was 1.5)
                else if (percentage > 50) riskScore += 0.5; // Highly concentrated sector (was 1.0)
                else if (percentage > 40) riskScore += 0.25; // Moderately concentrated sector (was 0.5)
                else if (percentage > 30) riskScore += 0.1; // Somewhat concentrated sector (was 0.25)
            } else {
                // Individual stock sectors get full risk penalties
                if (percentage > 70) riskScore += 2.0;      // Extreme sector concentration
                else if (percentage > 60) riskScore += 1.5; // Very concentrated sector
                else if (percentage > 50) riskScore += 1.0; // Highly concentrated sector
                else if (percentage > 40) riskScore += 0.5; // Moderately concentrated sector
                else if (percentage > 30) riskScore += 0.25; // Somewhat concentrated sector
            }
        }
        
        // Beta adjustments (more granular)
        if (portfolioBeta > 1.5) riskScore += 1.0;      // Very high volatility
        else if (portfolioBeta > 1.3) riskScore += 0.5; // High volatility
        else if (portfolioBeta > 1.1) riskScore += 0.25; // Moderately high volatility
        else if (portfolioBeta < 0.9) riskScore -= 0.25; // Low volatility
        else if (portfolioBeta < 0.7) riskScore -= 0.5;  // Very low volatility
        
        // Asset class diversification bonus
        Map<String, BigDecimal> assetClassExposure = holdings.stream()
                .filter(h -> h.getAssetType() != null && !h.getAssetType().isEmpty())
                .collect(Collectors.groupingBy(
                    Holding::getAssetType,
                    Collectors.reducing(BigDecimal.ZERO, 
                        h -> h.getPricePerShare().multiply(BigDecimal.valueOf(h.getShares())), 
                        BigDecimal::add)
                ));
        
        if (assetClassExposure.size() >= 3) {
            riskScore -= 0.5; // Diversification bonus for 3+ asset classes
        } else if (assetClassExposure.size() == 1) {
            riskScore += 1.0; // Penalty for single asset class
        } else if (assetClassExposure.size() == 2) {
            riskScore += 0.5; // Small penalty for limited asset classes
        }
        
        // ETF risk reduction bonus (ETFs provide inherent diversification)
        long etfCount = holdings.stream()
            .filter(h -> h.getAssetType() != null && 
                (h.getAssetType().toLowerCase().contains("etf") || 
                 h.getAssetType().toLowerCase().contains("fund")))
            .count();
        
        if (etfCount >= 3) riskScore -= 1.0; // Multiple ETFs significantly reduce risk
        else if (etfCount >= 2) riskScore -= 0.5; // Two ETFs reduce risk
        else if (etfCount >= 1) riskScore -= 0.25; // One ETF reduces risk slightly
        
        // Quality adjustments for blue-chip holdings
        for (Holding holding : holdings) {
            String ticker = holding.getTicker().toUpperCase();
            if (ticker.equals("MSFT") || ticker.equals("AAPL") || ticker.equals("KO") || 
                ticker.equals("GOOGL") || ticker.equals("AMZN") || ticker.equals("TSLA")) {
                riskScore -= 0.25; // Blue-chip discount
            }
            if (ticker.equals("BND") || ticker.equals("AGG") || ticker.equals("TLT")) {
                riskScore -= 0.5; // Investment-grade bond discount
            }
            if (ticker.equals("SPY") || ticker.equals("VTI") || ticker.equals("QQQ")) {
                riskScore -= 0.25; // Broad market ETF discount
            }
        }
        
        // Ensure score is within 1-10 range
        return Math.max(1, Math.min(10, (int) Math.round(riskScore)));
    }

    private int calculateDiversificationScore(List<Holding> holdings, BigDecimal totalValue) {
        // Calculate sector diversification
        Map<String, BigDecimal> sectorExposure = holdings.stream()
                .filter(h -> h.getSector() != null && !h.getSector().isEmpty())
                .collect(Collectors.groupingBy(
                    Holding::getSector,
                    Collectors.reducing(BigDecimal.ZERO, 
                        h -> h.getPricePerShare().multiply(BigDecimal.valueOf(h.getShares())), 
                        BigDecimal::add)
                ));
        
        // Calculate asset class diversification
        Map<String, BigDecimal> assetClassExposure = holdings.stream()
                .filter(h -> h.getAssetType() != null && !h.getAssetType().isEmpty())
                .collect(Collectors.groupingBy(
                    Holding::getAssetType,
                    Collectors.reducing(BigDecimal.ZERO, 
                        h -> h.getPricePerShare().multiply(BigDecimal.valueOf(h.getShares())), 
                        BigDecimal::add)
                ));
        
        // Base diversification score based on number of sectors
        double diversificationScore;
        if (sectorExposure.size() >= 5) diversificationScore = 80.0;
        else if (sectorExposure.size() >= 4) diversificationScore = 65.0;
        else if (sectorExposure.size() >= 3) diversificationScore = 50.0;
        else if (sectorExposure.size() >= 2) diversificationScore = 35.0;
        else diversificationScore = 20.0;
        
        // Asset class bonus
        if (assetClassExposure.size() >= 4) diversificationScore += 30;
        else if (assetClassExposure.size() >= 3) diversificationScore += 20;
        else if (assetClassExposure.size() >= 2) diversificationScore += 10;
        
        // ETF diversification bonus (ETFs provide inherent diversification)
        long etfCount = holdings.stream()
            .filter(h -> h.getAssetType() != null && 
                (h.getAssetType().toLowerCase().contains("etf") || 
                 h.getAssetType().toLowerCase().contains("fund")))
            .count();
        
        if (etfCount >= 3) diversificationScore += 15; // Multiple ETFs provide good diversification
        else if (etfCount >= 2) diversificationScore += 10; // Two ETFs provide some diversification
        else if (etfCount >= 1) diversificationScore += 5; // One ETF provides some diversification
        

        
        // Single holding concentration penalties (only for holdings >15%)
        for (Holding holding : holdings) {
            BigDecimal holdingValue = holding.getPricePerShare().multiply(BigDecimal.valueOf(holding.getShares()));
            double percentage = holdingValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            
            // Check if this is an ETF (less penalized than individual stocks)
            boolean isETF = holding.getAssetType() != null && 
                (holding.getAssetType().toLowerCase().contains("etf") || 
                 holding.getAssetType().toLowerCase().contains("fund"));
            
            double penalty = 0;
            if (isETF) {
                // ETFs get reduced penalties since they provide inherent diversification
                if (percentage > 50) penalty = 15; // Extreme concentration (was 25)
                else if (percentage > 40) penalty = 12; // Very concentrated (was 20)
                else if (percentage > 30) penalty = 8; // Highly concentrated (was 15)
                else if (percentage > 20) penalty = 5; // Moderately concentrated (was 10)
                else if (percentage > 15) penalty = 2; // Somewhat concentrated (was 5)
            } else {
                // Individual stocks get full penalties
                if (percentage > 50) penalty = 25; // Extreme concentration
                else if (percentage > 40) penalty = 20; // Very concentrated
                else if (percentage > 30) penalty = 15; // Highly concentrated
                else if (percentage > 20) penalty = 10; // Moderately concentrated
                else if (percentage > 15) penalty = 5; // Somewhat concentrated
            }
            
            if (penalty > 0) {
                diversificationScore -= penalty;
            }
        }
        
        // Sector concentration penalties (only for sectors >20%)
        for (Map.Entry<String, BigDecimal> entry : sectorExposure.entrySet()) {
            double percentage = entry.getValue().divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            
            // Check if this sector is primarily ETFs (less penalized)
            String sectorName = entry.getKey();
            boolean isETFSector = holdings.stream()
                .filter(h -> h.getSector() != null && h.getSector().equals(sectorName))
                .allMatch(h -> h.getAssetType() != null && 
                    (h.getAssetType().toLowerCase().contains("etf") || 
                     h.getAssetType().toLowerCase().contains("fund")));
            
            double penalty = 0;
            if (isETFSector) {
                // ETF sectors get reduced penalties
                if (percentage > 60) penalty = 12; // Extreme sector concentration (was 20)
                else if (percentage > 50) penalty = 8; // Very concentrated sector (was 15)
                else if (percentage > 40) penalty = 5; // Highly concentrated sector (was 10)
                else if (percentage > 30) penalty = 3; // Moderately concentrated sector (was 5)
                else if (percentage > 20) penalty = 1; // Somewhat concentrated sector (was 2)
            } else {
                // Individual stock sectors get full penalties
                if (percentage > 60) penalty = 20; // Extreme sector concentration
                else if (percentage > 50) penalty = 15; // Very concentrated sector
                else if (percentage > 40) penalty = 10; // Highly concentrated sector
                else if (percentage > 30) penalty = 5; // Moderately concentrated sector
                else if (percentage > 20) penalty = 2; // Somewhat concentrated sector
            }
            
            if (penalty > 0) {
                diversificationScore -= penalty;
            }
        }
        
        // Geographic diversification bonus (if international holdings present)
        boolean hasInternational = holdings.stream()
                .anyMatch(h -> h.getSector() != null && 
                    (h.getSector().toLowerCase().contains("international") || 
                     h.getSector().toLowerCase().contains("global")));
        
        if (hasInternational) {
            diversificationScore += 10; // International diversification bonus
        }
        
        // Additional check for international exposure by ticker
        boolean hasInternationalTicker = holdings.stream()
                .anyMatch(h -> h.getTicker() != null && 
                    (h.getTicker().toUpperCase().contains("VXUS") || 
                     h.getTicker().toUpperCase().contains("EFA") ||
                     h.getTicker().toUpperCase().contains("EEM")));
        
        if (hasInternationalTicker && !hasInternational) {
            diversificationScore += 10; // International diversification bonus
        }
        

        
        // Ensure score is within 1-100 range
        return Math.max(1, Math.min(100, (int) Math.round(diversificationScore)));
    }

    private int calculateGoalAlignmentScore(List<Holding> holdings, BigDecimal totalValue, Client client) {
        // Base score starts at 50
        double goalAlignmentScore = 50.0;
        
        // Calculate dividend income
        BigDecimal totalDividendIncome = BigDecimal.ZERO;
        for (Holding holding : holdings) {
            if (holding.getDividendYield() != null) {
                BigDecimal holdingValue = holding.getPricePerShare().multiply(BigDecimal.valueOf(holding.getShares()));
                BigDecimal annualDividend = holdingValue.multiply(BigDecimal.valueOf(holding.getDividendYield() / 100));
                totalDividendIncome = totalDividendIncome.add(annualDividend);
            }
        }
        
        // Income goal alignment (max 30 points)
        if (client.getAnnualIncomeGoal() != null) {
            double incomeGoal = client.getAnnualIncomeGoal().doubleValue();
            double currentIncome = totalDividendIncome.doubleValue();
            double incomeRatio = currentIncome / incomeGoal;
            
            if (incomeRatio >= 0.8) goalAlignmentScore += 30; // Close to goal
            else if (incomeRatio >= 0.5) goalAlignmentScore += 20; // Halfway to goal
            else if (incomeRatio >= 0.25) goalAlignmentScore += 10; // Quarter to goal
        }
        
        // Risk tolerance alignment (max 20 points)
        String riskTolerance = client.getRiskTolerance().toLowerCase();
        double portfolioBeta = calculatePortfolioBeta(holdings, totalValue);
        
        if (riskTolerance.equals("conservative") && portfolioBeta < 0.8) goalAlignmentScore += 20;
        else if (riskTolerance.equals("moderate") && portfolioBeta >= 0.8 && portfolioBeta <= 1.2) goalAlignmentScore += 20;
        else if (riskTolerance.equals("aggressive") && portfolioBeta > 1.2) goalAlignmentScore += 20;
        else if (riskTolerance.equals("conservative") && portfolioBeta < 1.0) goalAlignmentScore += 10;
        else if (riskTolerance.equals("aggressive") && portfolioBeta > 1.0) goalAlignmentScore += 10;
        
        // Ensure score is within 1-100 range
        return Math.max(1, Math.min(100, (int) Math.round(goalAlignmentScore)));
    }

    private double calculatePortfolioBeta(List<Holding> holdings, BigDecimal totalValue) {
        BigDecimal weightedBeta = BigDecimal.ZERO;
        BigDecimal totalBetaWeight = BigDecimal.ZERO;
        
        for (Holding holding : holdings) {
            if (holding.getBeta() != null) {
                BigDecimal holdingValue = holding.getPricePerShare().multiply(BigDecimal.valueOf(holding.getShares()));
                weightedBeta = weightedBeta.add(holdingValue.multiply(BigDecimal.valueOf(holding.getBeta())));
                totalBetaWeight = totalBetaWeight.add(holdingValue);
            }
        }
        
        if (totalBetaWeight.compareTo(BigDecimal.ZERO) > 0) {
            return weightedBeta.divide(totalBetaWeight, 2, RoundingMode.HALF_UP).doubleValue();
        }
        return 1.0; // Default to market beta if no data
    }

    // Inner class for sending request payload
    static class InsightRequest {
        public List<String> holdings;
        public String preferences;

        public InsightRequest(List<String> holdings, String preferences) {
            this.holdings = holdings;
            this.preferences = preferences;
        }
    }

    // Inner class for parsing FastAPI response
    static class AIResponse {
        public String summary;
        
        @JsonProperty("aiGeneratedText")
        public String aiGeneratedText;
        
        @JsonProperty("createdAt")
        public String createdAt;
        
        @JsonProperty("structuredInsight")
        public StructuredInsight structuredInsight;
    }
    
    // Inner class for asset recommendation
    static class AssetRecommendation {
        @JsonProperty("ticker")
        public String ticker;
        
        @JsonProperty("assetName")
        public String assetName;
        
        @JsonProperty("allocation")
        public String allocation;
        
        @JsonProperty("category")
        public String category;
        
        @JsonProperty("reasoning")
        public String reasoning;
        
        @JsonProperty("priority")
        public String priority;
        
        @JsonProperty("expectedImpact")
        public String expectedImpact;
    }
    
    // Inner class for structured insight data
    static class StructuredInsight {
        @JsonProperty("mainRecommendations")
        public List<String> mainRecommendations;
        
        @JsonProperty("sections")
        public Map<String, Object> sections;
        
        @JsonProperty("rawResponse")
        public String rawResponse;
        
        @JsonProperty("qualitativeInsights")
        public Map<String, String> qualitativeInsights;
        
        @JsonProperty("scoreAdjustments")
        public Map<String, Map<String, String>> scoreAdjustments;
        
        @JsonProperty("assetRecommendations")
        public List<AssetRecommendation> assetRecommendations;
    }

    /**
     * Validates and potentially adjusts asset recommendation priorities based on objective criteria
     */
    private void validateAssetRecommendationPriorities(List<AssetRecommendation> recommendations, List<Holding> holdings, Client client) {
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }

        // Calculate current portfolio characteristics using sector and asset class data
        Map<String, BigDecimal> sectorDistribution = new HashMap<>();
        Map<String, BigDecimal> assetClassDistribution = new HashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        
        // Track portfolio characteristics based on sector and asset class, not specific tickers
        boolean hasInternationalExposure = false;
        boolean hasBondExposure = false;
        boolean hasDividendFocus = false;
        boolean hasRealEstate = false;
        boolean hasCommodities = false;
        boolean hasSmallCap = false;
        boolean hasLargeCap = false;
        boolean hasTechnology = false;
        boolean hasHealthcare = false;
        boolean hasFinancials = false;

        for (Holding holding : holdings) {
            BigDecimal value = holding.getPricePerShare().multiply(BigDecimal.valueOf(holding.getShares()));
            totalValue = totalValue.add(value);
            
            String sector = holding.getSector() != null ? holding.getSector().toLowerCase() : "";
            String assetClass = getAssetClassFromSector(sector);
            
            // Track sector distribution
            sectorDistribution.merge(sector, value, BigDecimal::add);
            assetClassDistribution.merge(assetClass, value, BigDecimal::add);
            
            // Determine portfolio characteristics based on sector/asset class
            if (sector.contains("international") || sector.contains("global") || 
                assetClass.contains("international") || assetClass.contains("global")) {
                hasInternationalExposure = true;
            }
            
            if (sector.contains("bond") || sector.contains("fixed income") || 
                assetClass.contains("bond") || assetClass.contains("fixed income")) {
                hasBondExposure = true;
            }
            
            if (sector.contains("dividend") || sector.contains("income") || 
                assetClass.contains("dividend") || assetClass.contains("income")) {
                hasDividendFocus = true;
            }
            
            if (sector.contains("real estate") || sector.contains("reit")) {
                hasRealEstate = true;
            }
            
            if (sector.contains("commodity") || sector.contains("gold") || sector.contains("oil")) {
                hasCommodities = true;
            }
            
            if (sector.contains("small cap") || sector.contains("small-cap")) {
                hasSmallCap = true;
            }
            
            if (sector.contains("large cap") || sector.contains("large-cap") || 
                sector.contains("broad market") || sector.contains("total market")) {
                hasLargeCap = true;
            }
            
            if (sector.contains("technology") || sector.contains("tech")) {
                hasTechnology = true;
            }
            
            if (sector.contains("healthcare") || sector.contains("medical")) {
                hasHealthcare = true;
            }
            
            if (sector.contains("financial") || sector.contains("bank")) {
                hasFinancials = true;
            }
        }

        // Validate each recommendation based on category and portfolio gaps
        for (AssetRecommendation rec : recommendations) {
            String category = rec.category.toLowerCase();
            String currentPriority = rec.priority;
            String suggestedPriority = currentPriority; // Default to AI's suggestion

            // HIGH PRIORITY validation - check if it's truly high priority
            if (currentPriority.equalsIgnoreCase("HIGH")) {
                if (category.contains("international") && hasInternationalExposure) {
                    suggestedPriority = "MEDIUM"; // Already has international exposure
                } else if (category.contains("bond") && hasBondExposure && 
                          client.getRiskTolerance().equalsIgnoreCase("conservative")) {
                    suggestedPriority = "MEDIUM"; // Already has bonds and conservative
                } else if (category.contains("dividend") && hasDividendFocus && 
                          client.getInvestmentGoals().toLowerCase().contains("income")) {
                    suggestedPriority = "MEDIUM"; // Already has dividend focus and income goals
                } else if (category.contains("real estate") && hasRealEstate) {
                    suggestedPriority = "MEDIUM"; // Already has real estate exposure
                } else if (category.contains("commodity") && hasCommodities) {
                    suggestedPriority = "MEDIUM"; // Already has commodity exposure
                }
            }

            // MEDIUM PRIORITY validation - check if it should be high priority
            if (currentPriority.equalsIgnoreCase("MEDIUM")) {
                if (category.contains("international") && !hasInternationalExposure) {
                    suggestedPriority = "HIGH"; // Missing international exposure
                } else if (category.contains("bond") && !hasBondExposure && 
                          client.getRiskTolerance().equalsIgnoreCase("conservative")) {
                    suggestedPriority = "HIGH"; // Conservative client missing bonds
                } else if (category.contains("dividend") && !hasDividendFocus && 
                          client.getInvestmentGoals().toLowerCase().contains("income")) {
                    suggestedPriority = "HIGH"; // Income goals missing dividend focus
                } else if (category.contains("real estate") && !hasRealEstate) {
                    suggestedPriority = "HIGH"; // Missing real estate diversification
                } else if (category.contains("small cap") && !hasSmallCap && hasLargeCap) {
                    suggestedPriority = "HIGH"; // Has large cap but missing small cap
                } else if (category.contains("technology") && !hasTechnology) {
                    suggestedPriority = "HIGH"; // Missing technology sector
                } else if (category.contains("healthcare") && !hasHealthcare) {
                    suggestedPriority = "HIGH"; // Missing healthcare sector
                } else if (category.contains("financial") && !hasFinancials) {
                    suggestedPriority = "HIGH"; // Missing financial sector
                }
            }

            // LOW PRIORITY validation - check if it should be higher priority
            if (currentPriority.equalsIgnoreCase("LOW")) {
                if (category.contains("international") && !hasInternationalExposure) {
                    suggestedPriority = "MEDIUM"; // Missing international exposure
                } else if (category.contains("bond") && !hasBondExposure && 
                          client.getRiskTolerance().equalsIgnoreCase("conservative")) {
                    suggestedPriority = "MEDIUM"; // Conservative client missing bonds
                } else if (category.contains("dividend") && !hasDividendFocus && 
                          client.getInvestmentGoals().toLowerCase().contains("income")) {
                    suggestedPriority = "MEDIUM"; // Income goals missing dividend focus
                } else if (category.contains("real estate") && !hasRealEstate) {
                    suggestedPriority = "MEDIUM"; // Missing real estate diversification
                }
            }

            // Update priority if validation suggests a change
            if (!suggestedPriority.equals(currentPriority)) {
                System.out.println("Priority validation: " + rec.ticker + " changed from " + currentPriority + " to " + suggestedPriority);
                rec.priority = suggestedPriority;
            }
        }
    }

    /**
     * Helper method to determine asset class from sector information
     */
    private String getAssetClassFromSector(String sector) {
        if (sector == null) return "unknown";
        
        sector = sector.toLowerCase();
        
        if (sector.contains("bond") || sector.contains("fixed income") || sector.contains("treasury")) {
            return "fixed income";
        } else if (sector.contains("international") || sector.contains("global") || 
                   sector.contains("emerging market") || sector.contains("developed market")) {
            return "international";
        } else if (sector.contains("dividend") || sector.contains("income")) {
            return "dividend";
        } else if (sector.contains("real estate") || sector.contains("reit")) {
            return "real estate";
        } else if (sector.contains("commodity") || sector.contains("gold") || sector.contains("oil")) {
            return "commodity";
        } else if (sector.contains("small cap") || sector.contains("small-cap")) {
            return "small cap";
        } else if (sector.contains("large cap") || sector.contains("large-cap")) {
            return "large cap";
        } else if (sector.contains("broad market") || sector.contains("total market")) {
            return "broad market";
        } else if (sector.contains("technology") || sector.contains("tech")) {
            return "technology";
        } else if (sector.contains("healthcare") || sector.contains("medical")) {
            return "healthcare";
        } else if (sector.contains("financial") || sector.contains("bank")) {
            return "financial";
        } else if (sector.contains("consumer") || sector.contains("retail")) {
            return "consumer";
        } else if (sector.contains("industrial") || sector.contains("manufacturing")) {
            return "industrial";
        } else if (sector.contains("energy") || sector.contains("oil") || sector.contains("gas")) {
            return "energy";
        } else if (sector.contains("utility") || sector.contains("utilities")) {
            return "utility";
        } else if (sector.contains("material") || sector.contains("mining")) {
            return "materials";
        } else if (sector.contains("communication") || sector.contains("telecom")) {
            return "communication";
        } else if (sector.contains("mutual fund") || sector.contains("fund")) {
            return "mutual fund";
        } else if (sector.contains("stock") || sector.contains("equity")) {
            return "individual stock";
        }
        
        return "equity"; // Default to equity for unknown sectors
    }

} 