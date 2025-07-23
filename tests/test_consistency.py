#!/usr/bin/env python3
"""
Test script to verify consistency improvements in investment insights
"""

import requests
import json
import time
from datetime import datetime

def test_insight_consistency():
    """Test that multiple insights for the same portfolio are consistent"""
    
    base_url = "http://localhost:8080"
    
    print("🧪 Testing Investment Insight Consistency")
    print("=" * 50)
    
    # Test parameters
    portfolio_id = 1
    client_id = 1
    test_count = 3
    
    print(f"Generating {test_count} insights for portfolio {portfolio_id}...")
    
    insights = []
    
    for i in range(test_count):
        print(f"\n📊 Generating insight {i+1}/{test_count}...")
        
        # Generate insight
        response = requests.post(
            f"{base_url}/api/insights/generate-portfolio/{portfolio_id}?clientId={client_id}",
            headers={"Content-Type": "application/json"},
            json={"preferences": "Provide a comprehensive portfolio analysis focusing on risk management and diversification."}
        )
        
        if response.status_code == 200:
            insight = response.json()
            insights.append(insight)
            
            print(f"  ✓ Insight generated successfully")
            print(f"  📈 Risk Score: {insight.get('riskScore', 'N/A')}/10")
            print(f"  🎯 Diversification Score: {insight.get('diversificationScore', 'N/A')}%")
            print(f"  🎯 Goal Alignment: {insight.get('goalAlignment', 'N/A')}%")
        else:
            print(f"  ✗ Failed to generate insight: {response.status_code}")
            print(f"  Error: {response.text}")
            return False
        
        # Small delay between requests
        time.sleep(2)
    
    # Analyze consistency
    print(f"\n📊 Consistency Analysis")
    print("=" * 30)
    
    risk_scores = [insight.get('riskScore') for insight in insights if insight.get('riskScore') is not None]
    div_scores = [insight.get('diversificationScore') for insight in insights if insight.get('diversificationScore') is not None]
    goal_scores = [insight.get('goalAlignment') for insight in insights if insight.get('goalAlignment') is not None]
    
    print(f"Risk Scores: {risk_scores}")
    print(f"Diversification Scores: {div_scores}")
    print(f"Goal Alignment Scores: {goal_scores}")
    
    # Check consistency
    risk_consistent = len(set(risk_scores)) <= 1
    div_consistent = len(set(div_scores)) <= 1
    goal_consistent = len(set(goal_scores)) <= 1
    
    print(f"\n✅ Consistency Results:")
    print(f"  Risk Score: {'✓ Consistent' if risk_consistent else '✗ Inconsistent'}")
    print(f"  Diversification Score: {'✓ Consistent' if div_consistent else '✗ Inconsistent'}")
    print(f"  Goal Alignment: {'✓ Consistent' if goal_consistent else '✗ Inconsistent'}")
    
    # Overall consistency
    overall_consistent = risk_consistent and div_consistent and goal_consistent
    
    if overall_consistent:
        print(f"\n🎉 SUCCESS: All scores are consistent across {test_count} insights!")
        return True
    else:
        print(f"\n⚠️  WARNING: Some scores are inconsistent. This may indicate remaining variability.")
        return False

if __name__ == "__main__":
    try:
        success = test_insight_consistency()
        exit(0 if success else 1)
    except Exception as e:
        print(f"❌ Test failed with error: {e}")
        exit(1) 