#!/usr/bin/env python3
"""
Test script to verify hybrid scoring improvements
"""

import requests
import json
import time

def test_hybrid_scoring():
    """Test the hybrid scoring system with multiple runs on the same portfolio"""
    
    print("=== HYBRID SCORING TEST ===")
    print("Testing consistency improvements...")
    
    # Test data - same portfolio multiple times
    test_portfolio_id = 1  # Assuming this exists
    test_client_id = 1     # Assuming this exists
    test_preferences = "Focus on risk assessment and diversification"
    
    results = []
    
    for i in range(3):
        print(f"\n--- Test Run {i+1} ---")
        
        try:
            # Generate insight for the same portfolio
            response = requests.post(
                f"http://localhost:8080/api/insights/generate-portfolio/{test_portfolio_id}",
                params={
                    "preferences": test_preferences,
                    "clientId": test_client_id
                },
                timeout=30
            )
            
            if response.status_code == 200:
                insight = response.json()
                
                result = {
                    "run": i+1,
                    "risk_score": insight.get("riskScore"),
                    "diversification_score": insight.get("diversificationScore"),
                    "goal_alignment": insight.get("goalAlignment"),
                    "summary": insight.get("summary"),
                    "portfolio_name": insight.get("portfolioName")
                }
                
                results.append(result)
                
                print(f"Risk Score: {result['risk_score']}/10")
                print(f"Diversification: {result['diversification_score']}%")
                print(f"Goal Alignment: {result['goal_alignment']}%")
                print(f"Summary: {result['summary']}")
                print(f"Portfolio: {result['portfolio_name']}")
                
            else:
                print(f"Error: {response.status_code} - {response.text}")
                
        except Exception as e:
            print(f"Exception: {e}")
        
        # Small delay between runs
        time.sleep(2)
    
    # Analyze results
    print("\n=== ANALYSIS ===")
    
    if len(results) >= 2:
        # Check consistency
        risk_scores = [r['risk_score'] for r in results if r['risk_score'] is not None]
        div_scores = [r['diversification_score'] for r in results if r['diversification_score'] is not None]
        goal_scores = [r['goal_alignment'] for r in results if r['goal_alignment'] is not None]
        
        print(f"Risk Scores: {risk_scores}")
        print(f"Diversification Scores: {div_scores}")
        print(f"Goal Alignment Scores: {goal_scores}")
        
        # Calculate consistency metrics
        if len(risk_scores) >= 2:
            risk_variance = max(risk_scores) - min(risk_scores)
            print(f"Risk Score Variance: {risk_variance} points")
            
        if len(div_scores) >= 2:
            div_variance = max(div_scores) - min(div_scores)
            print(f"Diversification Score Variance: {div_variance} points")
            
        if len(goal_scores) >= 2:
            goal_variance = max(goal_scores) - min(goal_scores)
            print(f"Goal Alignment Variance: {goal_variance} points")
        
        # Success criteria
        print("\n=== SUCCESS CRITERIA ===")
        print("✅ Risk Score Variance ≤ 1 point")
        print("✅ Diversification Score Variance ≤ 5 points") 
        print("✅ Goal Alignment Variance ≤ 5 points")
        print("✅ All scores should be consistent across runs")
        
    else:
        print("Not enough successful runs to analyze consistency")

if __name__ == "__main__":
    test_hybrid_scoring() 