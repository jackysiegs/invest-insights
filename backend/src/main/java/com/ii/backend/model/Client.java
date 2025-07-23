package com.ii.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String riskTolerance;
    private String investmentGoals;
    private Integer yearsUntilGoal; // Years until retirement or goal
    private BigDecimal annualIncomeGoal; // Target annual income from investments

    @ManyToOne
    @JoinColumn(name = "advisor_id")
    private Advisor advisor;

    // Constructors, Getters, Setters

    public Client() {}

    public Client(String name, String email, String riskTolerance, String investmentGoals, Integer yearsUntilGoal, BigDecimal annualIncomeGoal, Advisor advisor) {
        this.name = name;
        this.email = email;
        this.riskTolerance = riskTolerance;
        this.investmentGoals = investmentGoals;
        this.yearsUntilGoal = yearsUntilGoal;
        this.annualIncomeGoal = annualIncomeGoal;
        this.advisor = advisor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRiskTolerance() {
        return riskTolerance;
    }

    public void setRiskTolerance(String riskTolerance) {
        this.riskTolerance = riskTolerance;
    }

    public String getInvestmentGoals() {
        return investmentGoals;
    }

    public void setInvestmentGoals(String investmentGoals) {
        this.investmentGoals = investmentGoals;
    }

    public Integer getYearsUntilGoal() {
        return yearsUntilGoal;
    }

    public void setYearsUntilGoal(Integer yearsUntilGoal) {
        this.yearsUntilGoal = yearsUntilGoal;
    }

    public BigDecimal getAnnualIncomeGoal() {
        return annualIncomeGoal;
    }

    public void setAnnualIncomeGoal(BigDecimal annualIncomeGoal) {
        this.annualIncomeGoal = annualIncomeGoal;
    }

    public Advisor getAdvisor() {
        return advisor;
    }

    public void setAdvisor(Advisor advisor) {
        this.advisor = advisor;
    }
}
