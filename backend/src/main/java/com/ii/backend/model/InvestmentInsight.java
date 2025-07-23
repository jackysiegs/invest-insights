package com.ii.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
public class InvestmentInsight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String aiGeneratedText;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    private String portfolioName; // Name of the portfolio this insight was generated for

    // Structured insight fields
    private Integer riskScore;
    private Integer diversificationScore;
    private Integer goalAlignment;
    
    @Column(columnDefinition = "TEXT")
    private String structuredData; // JSON string for sections and recommendations
    
    @Column(columnDefinition = "TEXT")
    private String debugInfo; // JSON string for debugging information

    // Constructors
    public InvestmentInsight() {}

    public InvestmentInsight(String summary, String aiGeneratedText, Client client) {
        this.summary = summary;
        this.aiGeneratedText = aiGeneratedText;
        this.client = client;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getAiGeneratedText() { return aiGeneratedText; }
    public void setAiGeneratedText(String aiGeneratedText) { this.aiGeneratedText = aiGeneratedText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    // Structured insight getters and setters
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public Integer getDiversificationScore() { return diversificationScore; }
    public void setDiversificationScore(Integer diversificationScore) { this.diversificationScore = diversificationScore; }

    public Integer getGoalAlignment() { return goalAlignment; }
    public void setGoalAlignment(Integer goalAlignment) { this.goalAlignment = goalAlignment; }

    public String getStructuredData() { return structuredData; }
    public void setStructuredData(String structuredData) { this.structuredData = structuredData; }

    public String getPortfolioName() { return portfolioName; }
    public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }
    
    public String getDebugInfo() { return debugInfo; }
    public void setDebugInfo(String debugInfo) { this.debugInfo = debugInfo; }
}
