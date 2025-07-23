#!/usr/bin/env python3
"""
Test script to validate AI improvements for RJ Invest Insights
Tests granular adjustments and specific reasoning requirements
"""

import requests
import json
import time
from typing import Dict, List, Any

# Test configurations
AI_SERVICE_URL = "http://localhost:8000/generate-insight"

# Test portfolios
TEST_PORTFOLIOS = {
    "etf_heavy": {
        "name": "ETF-Heavy Portfolio",
        "holdings": ["SPY", "VTI", "QQQ", "VEA", "VWO"],
        "description": "Diversified ETF portfolio with broad market exposure"
    },
    "individual_stocks": {
        "name": "Individual Stock Portfolio", 
        "holdings": ["MSFT", "AAPL", "GOOGL", "AMZN", "TSLA"],
        "description": "Tech-heavy individual stock portfolio"
    },
    "mixed": {
        "name": "Mixed Portfolio",
        "holdings": ["SPY", "VTI", "MSFT", "AAPL", "JNJ"],
        "description": "Balanced mix of ETFs and individual stocks"
    }
}

def test_ai_response(portfolio_type: str, portfolio_data: Dict[str, Any]) -> Dict[str, Any]:
    """Test AI response for a specific portfolio type"""
    
    print(f"\n{'='*60}")
    print(f"Testing {portfolio_data['name']}")
    print(f"Holdings: {', '.join(portfolio_data['holdings'])}")
    print(f"{'='*60}")
    
    # Prepare request
    request_data = {
        "holdings": portfolio_data['holdings'],
        "preferences": f"Portfolio Analysis: {portfolio_data['description']}. Client is moderate risk tolerance, 10-year time horizon, seeking growth with some income."
    }
    
    try:
        # Make request to AI service
        response = requests.post(AI_SERVICE_URL, json=request_data, timeout=30)
        
        if response.status_code == 200:
            ai_response = response.json()
            return analyze_ai_response(portfolio_type, ai_response)
        else:
            print(f"❌ Error: HTTP {response.status_code}")
            print(f"Response: {response.text}")
            return {"error": f"HTTP {response.status_code}"}
            
    except requests.exceptions.RequestException as e:
        print(f"❌ Request failed: {e}")
        return {"error": str(e)}

def analyze_ai_response(portfolio_type: str, ai_response: Dict[str, Any]) -> Dict[str, Any]:
    """Analyze AI response for improvements validation"""
    
    analysis = {
        "portfolio_type": portfolio_type,
        "summary": ai_response.get("summary", ""),
        "adjustments_found": {},
        "specificity_score": 0,
        "granularity_score": 0,
        "issues": []
    }
    
    # Extract AI text
    ai_text = ai_response.get("aiGeneratedText", "")
    
    # Check for score adjustments
    if "structuredInsight" in ai_response and ai_response["structuredInsight"]:
        structured = ai_response["structuredInsight"]
        if "scoreAdjustments" in structured:
            adjustments = structured["scoreAdjustments"]
            analysis["adjustments_found"] = adjustments
            
            # Validate granularity
            granularity_score = validate_granularity(portfolio_type, adjustments)
            analysis["granularity_score"] = granularity_score
            
            # Check for specific reasoning
            specificity_score = validate_specificity(ai_text, adjustments)
            analysis["specificity_score"] = specificity_score
    
    # Check for specific percentage references
    percentage_references = count_percentage_references(ai_text)
    analysis["percentage_references"] = percentage_references
    
    # Check for specific holding references
    holding_references = count_holding_references(ai_text)
    analysis["holding_references"] = holding_references
    
    # Validate portfolio type recognition
    portfolio_recognition = validate_portfolio_recognition(portfolio_type, ai_text)
    analysis["portfolio_recognition"] = portfolio_recognition
    
    return analysis

def validate_granularity(portfolio_type: str, adjustments: Dict[str, Any]) -> int:
    """Validate that AI uses appropriate granularity based on portfolio type"""
    
    score = 0
    expected_ranges = {
        "etf_heavy": {"diversification": ["+2%", "-2%", "+5%", "-5%"], "risk": ["+1", "-1"]},
        "individual_stocks": {"diversification": ["+5%", "-5%", "+10%", "-10%"], "risk": ["+1", "-1", "+2", "-2"]},
        "mixed": {"diversification": ["+2%", "-2%", "+5%", "-5%"], "risk": ["+1", "-1"]}
    }
    
    if portfolio_type in expected_ranges:
        expected = expected_ranges[portfolio_type]
        
        for metric, expected_values in expected.items():
            if metric in adjustments:
                adjustment = adjustments[metric].get("adjustment", "")
                if adjustment in expected_values:
                    score += 1
                    print(f"✅ {metric}: {adjustment} (appropriate for {portfolio_type})")
                else:
                    print(f"⚠️  {metric}: {adjustment} (may not be optimal for {portfolio_type})")
    
    return score

def validate_specificity(ai_text: str, adjustments: Dict[str, Any]) -> int:
    """Validate that AI provides specific reasoning with percentages and holdings"""
    
    score = 0
    
    # Check if reasoning contains percentages
    for metric, adjustment_data in adjustments.items():
        reasoning = adjustment_data.get("reasoning", "")
        if any(char.isdigit() for char in reasoning):
            score += 1
            print(f"✅ {metric} reasoning contains specific data")
        else:
            print(f"⚠️  {metric} reasoning lacks specific data")
    
    return score

def count_percentage_references(text: str) -> int:
    """Count percentage references in AI text"""
    import re
    percentage_pattern = r'\d+\.?\d*%'
    matches = re.findall(percentage_pattern, text)
    return len(matches)

def count_holding_references(text: str) -> int:
    """Count specific holding/ticker references in AI text"""
    import re
    # Look for common ticker patterns (3-5 letter uppercase)
    ticker_pattern = r'\b[A-Z]{3,5}\b'
    matches = re.findall(ticker_pattern, text)
    # Filter out common words that might match
    common_words = {'THE', 'AND', 'FOR', 'ARE', 'BUT', 'NOT', 'YOU', 'ALL', 'ANY', 'CAN', 'HAD', 'HER', 'WAS', 'ONE', 'OUR', 'OUT', 'DAY', 'GET', 'HAS', 'HIM', 'HIS', 'HOW', 'MAN', 'NEW', 'NOW', 'OLD', 'SEE', 'TWO', 'WAY', 'WHO', 'BOY', 'DID', 'ITS', 'LET', 'PUT', 'SAY', 'SHE', 'TOO', 'USE'}
    ticker_matches = [match for match in matches if match not in common_words]
    return len(ticker_matches)

def validate_portfolio_recognition(portfolio_type: str, ai_text: str) -> bool:
    """Validate that AI recognizes the portfolio type correctly"""
    
    text_lower = ai_text.lower()
    
    if portfolio_type == "etf_heavy":
        etf_keywords = ["etf", "exchange-traded fund", "diversified", "broad market"]
        return any(keyword in text_lower for keyword in etf_keywords)
    
    elif portfolio_type == "individual_stocks":
        stock_keywords = ["individual stock", "single stock", "company-specific", "stock concentration"]
        return any(keyword in text_lower for keyword in stock_keywords)
    
    elif portfolio_type == "mixed":
        mixed_keywords = ["mixed", "combination", "both etf", "both stock"]
        return any(keyword in text_lower for keyword in mixed_keywords)
    
    return False

def print_analysis_results(results: List[Dict[str, Any]]):
    """Print comprehensive analysis results"""
    
    print(f"\n{'='*80}")
    print("AI IMPROVEMENTS VALIDATION RESULTS")
    print(f"{'='*80}")
    
    total_granularity = 0
    total_specificity = 0
    total_percentages = 0
    total_holdings = 0
    total_recognition = 0
    
    for result in results:
        if "error" in result:
            print(f"\n❌ {result['portfolio_type']}: {result['error']}")
            continue
            
        print(f"\n📊 {result['portfolio_type'].replace('_', ' ').title()}:")
        print(f"   Summary: {result['summary']}")
        print(f"   Granularity Score: {result['granularity_score']}/2")
        print(f"   Specificity Score: {result['specificity_score']}/3")
        print(f"   Percentage References: {result['percentage_references']}")
        print(f"   Holding References: {result['holding_references']}")
        print(f"   Portfolio Recognition: {'✅' if result['portfolio_recognition'] else '❌'}")
        
        if result['adjustments_found']:
            print(f"   Adjustments: {json.dumps(result['adjustments_found'], indent=2)}")
        
        total_granularity += result['granularity_score']
        total_specificity += result['specificity_score']
        total_percentages += result['percentage_references']
        total_holdings += result['holding_references']
        if result['portfolio_recognition']:
            total_recognition += 1
    
    # Overall summary
    print(f"\n{'='*80}")
    print("OVERALL PERFORMANCE SUMMARY")
    print(f"{'='*80}")
    print(f"Average Granularity Score: {total_granularity/len(results):.1f}/2")
    print(f"Average Specificity Score: {total_specificity/len(results):.1f}/3")
    print(f"Total Percentage References: {total_percentages}")
    print(f"Total Holding References: {total_holdings}")
    print(f"Portfolio Recognition Rate: {total_recognition}/{len(results)} ({total_recognition/len(results)*100:.0f}%)")
    
    # Improvement assessment
    print(f"\n🎯 IMPROVEMENT ASSESSMENT:")
    if total_granularity >= len(results) * 1.5:
        print("✅ Granularity: AI is using appropriate adjustment levels")
    else:
        print("⚠️  Granularity: AI may still be using suboptimal adjustment levels")
    
    if total_specificity >= len(results) * 2:
        print("✅ Specificity: AI is providing specific reasoning")
    else:
        print("⚠️  Specificity: AI reasoning could be more specific")
    
    if total_percentages >= len(results) * 3:
        print("✅ Percentage References: AI is referencing specific percentages")
    else:
        print("⚠️  Percentage References: AI could reference more specific percentages")
    
    if total_holdings >= len(results) * 2:
        print("✅ Holding References: AI is referencing specific holdings")
    else:
        print("⚠️  Holding References: AI could reference more specific holdings")

def main():
    """Main test execution"""
    
    print("🧪 AI IMPROVEMENTS VALIDATION TEST")
    print("Testing granular adjustments and specific reasoning requirements")
    
    # Check if AI service is running
    try:
        health_check = requests.get("http://localhost:8000/", timeout=5)
        if health_check.status_code != 200:
            print("❌ AI service not responding properly")
            return
    except:
        print("❌ AI service not available at http://localhost:8000/")
        print("Please start the AI microservice first")
        return
    
    # Run tests for each portfolio type
    results = []
    for portfolio_type, portfolio_data in TEST_PORTFOLIOS.items():
        result = test_ai_response(portfolio_type, portfolio_data)
        results.append(result)
        time.sleep(2)  # Brief pause between requests
    
    # Print comprehensive results
    print_analysis_results(results)
    
    print(f"\n✅ Test completed! Check the results above to validate AI improvements.")

if __name__ == "__main__":
    main() 