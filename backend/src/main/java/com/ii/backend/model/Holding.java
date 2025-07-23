package com.ii.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Holding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker;
    private int shares;
    private BigDecimal pricePerShare;
    private String sector; // e.g., "Technology", "Healthcare"
    private String assetType; // e.g., "Stock", "Bond", "ETF", "Cash", "REIT"
    private Double beta; // Optional - measures volatility vs market
    private Double dividendYield; // Annual dividend yield as percentage

    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    public Holding() {}

    public Holding(String ticker, int shares, BigDecimal pricePerShare, String sector, String assetType, Double beta, Double dividendYield, Portfolio portfolio) {
        this.ticker = ticker;
        this.shares = shares;
        this.pricePerShare = pricePerShare;
        this.sector = sector;
        this.assetType = assetType;
        this.beta = beta;
        this.dividendYield = dividendYield;
        this.portfolio = portfolio;
    }

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public int getShares() {
        return shares;
    }

    public void setShares(int shares) {
        this.shares = shares;
    }

    public BigDecimal getPricePerShare() {
        return pricePerShare;
    }

    public void setPricePerShare(BigDecimal pricePerShare) {
        this.pricePerShare = pricePerShare;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public Double getBeta() {
        return beta;
    }

    public void setBeta(Double beta) {
        this.beta = beta;
    }

    public Double getDividendYield() {
        return dividendYield;
    }

    public void setDividendYield(Double dividendYield) {
        this.dividendYield = dividendYield;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }
}
