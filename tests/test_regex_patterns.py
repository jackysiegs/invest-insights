#!/usr/bin/env python3
"""
Test script to validate regex patterns for AI score adjustment extraction
"""

import re
import json

# Test AI responses from the debug output
test_responses = {
    "portfolio_1": """
=== SCORE ADJUSTMENT REQUEST ===
1. Risk Score Adjustment:
   - Suggested adjustment: +1
   - Brief reasoning: The extreme tech concentration (55.1%) warrants a slight risk adjustment due to sector-specific vulnerabilities.

2. Diversification Adjustment:
   - Suggested adjustment: -5%
   - Brief reasoning: The significant tech concentration (55.1%) and lack of diversification in other sectors indicate a need for a moderate diversification adjustment.

3. Goal Alignment Adjustment:
   - Suggested adjustment: -2%
   - Brief reasoning: The high tech exposure may not fully align with a moderate risk tolerance, suggesting a minor adjustment towards a more balanced allocation.
""",
    
    "portfolio_2": """
=== SCORE ADJUSTMENT REQUEST ===
Based on the analysis:
- Risk Score Adjustment: -1
  - The concentration in Diversified ETFs (53.7%) poses moderate risk; a slight adjustment is needed to enhance diversification.
- Diversification Adjustment: -5%
  - The significant concentration in the Diversified sector warrants a moderate adjustment to improve sector-specific diversification.
- Goal Alignment Adjustment: +2%
  - While the portfolio aligns well with income goals, a minor adjustment is recommended to enhance growth potential for retirement savings.
""",
    
    "portfolio_3": """
=== SCORE ADJUSTMENT REQUEST ===
Based on the qualitative analysis:
- Risk Score Adjustment: +1
  - Concentration risks in Real Estate (15.5%) and Healthcare (13.2%) warrant a slight increase in risk score.
- Diversification Adjustment: +5%
  - Lack of exposure to key sectors like Technology and Consumer Discretionary requires a moderate diversification adjustment.
- Goal Alignment Adjustment: +2%
  - Minor adjustments to enhance alignment by increasing exposure to defensive sectors.
""",
    

}

def test_regex_patterns():
    """Test all regex patterns against the actual AI responses"""
    
    print("🧪 TESTING REGEX PATTERNS FOR AI ADJUSTMENT EXTRACTION")
    print("=" * 60)
    
    # Risk score adjustment patterns
    risk_patterns = [
        # Format 1: "RISK SCORE ADJUSTMENT: ... Suggested adjustment: +1 ... Brief reasoning: ..."
        r'RISK\s+SCORE\s+ADJUSTMENT:.*?Suggested\s+adjustment:\s*([+-]?\d+).*?Brief\s+reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 2: "Risk Score Adjustment: +1 ... reasoning: ..."
        r'Risk\s+Score\s+Adjustment:\s*([+-]?\d+).*?reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 3: "- Risk Score Adjustment: +1 ... - [reasoning]"
        r'-\s*Risk\s+Score\s+Adjustment:\s*([+-]?\d+).*?-\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 4: "Risk Score Adjustment: +1 ... [reasoning on next line]"
        r'Risk\s+Score\s+Adjustment:\s*([+-]?\d+).*?\n\s*-\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 5: "RISK SCORE ADJUSTMENT: ... adjustment: +1 ... reasoning: ..."
        r'RISK\s+SCORE\s+ADJUSTMENT:.*?adjustment:\s*([+-]?\d+).*?reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 6: Generic risk pattern
        r'Risk.*?adjustment:\s*([+-]?\d+).*?reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 7: "Risk Score Adjustment: +1" followed by reasoning
        r'Risk\s+Score\s+Adjustment:\s*([+-]?\d+).*?\n\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 8: Bullet point format
        r'-\s*Risk\s+Score\s+Adjustment:\s*([+-]?\d+).*?\n\s*-\s*(.*?)(?=\n\n|\n[A-Z]|$)'
    ]
    
    # Diversification adjustment patterns
    div_patterns = [
        # Format 1: "DIVERSIFICATION ADJUSTMENT: ... Suggested adjustment: +5% ... Brief reasoning: ..."
        r'DIVERSIFICATION\s+ADJUSTMENT:.*?Suggested\s+adjustment:\s*([+-]?\d+%).*?Brief\s+reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 2: "Diversification Adjustment: +5% ... reasoning: ..."
        r'Diversification\s+Adjustment:\s*([+-]?\d+%).*?reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 3: "- Diversification Adjustment: +5% ... - [reasoning]"
        r'-\s*Diversification\s+Adjustment:\s*([+-]?\d+%).*?-\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 4: "Diversification Adjustment: +5% ... [reasoning on next line]"
        r'Diversification\s+Adjustment:\s*([+-]?\d+%).*?\n\s*-\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 5: "DIVERSIFICATION ADJUSTMENT: ... adjustment: +5% ... reasoning: ..."
        r'DIVERSIFICATION\s+ADJUSTMENT:.*?adjustment:\s*([+-]?\d+%).*?reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 6: Generic diversification pattern
        r'Diversification.*?adjustment:\s*([+-]?\d+%).*?reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 7: "Diversification Adjustment: +5%" followed by reasoning
        r'Diversification\s+Adjustment:\s*([+-]?\d+%).*?\n\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 8: Bullet point format
        r'-\s*Diversification\s+Adjustment:\s*([+-]?\d+%).*?\n\s*-\s*(.*?)(?=\n\n|\n[A-Z]|$)'
    ]
    
    # Goal alignment adjustment patterns
    goal_patterns = [
        # Format 1: "GOAL ALIGNMENT ADJUSTMENT: ... Suggested adjustment: +5% ... Brief reasoning: ..."
        r'GOAL\s+ALIGNMENT\s+ADJUSTMENT:.*?Suggested\s+adjustment:\s*([+-]?\d+%).*?Brief\s+reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 2: "Goal Alignment Adjustment: +5% ... reasoning: ..."
        r'Goal\s+Alignment\s+Adjustment:\s*([+-]?\d+%).*?reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 3: "- Goal Alignment Adjustment: +5% ... - [reasoning]"
        r'-\s*Goal\s+Alignment\s+Adjustment:\s*([+-]?\d+%).*?-\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 4: "Goal Alignment Adjustment: +5% ... [reasoning on next line]"
        r'Goal\s+Alignment\s+Adjustment:\s*([+-]?\d+%).*?\n\s*-\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 5: "GOAL ALIGNMENT ADJUSTMENT: ... adjustment: +5% ... reasoning: ..."
        r'GOAL\s+ALIGNMENT\s+ADJUSTMENT:.*?adjustment:\s*([+-]?\d+%).*?reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 6: Generic goal alignment pattern
        r'Goal\s+Alignment.*?adjustment:\s*([+-]?\d+%).*?reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 7: "Goal Alignment Adjustment: +5%" followed by reasoning
        r'Goal\s+Alignment\s+Adjustment:\s*([+-]?\d+%).*?\n\s*(.*?)(?=\n\n|\n[A-Z]|$)',
        
        # Format 8: Bullet point format
        r'-\s*Goal\s+Alignment\s+Adjustment:\s*([+-]?\d+%).*?\n\s*-\s*(.*?)(?=\n\n|\n[A-Z]|$)'
    ]
    
    for portfolio_name, ai_text in test_responses.items():
        print(f"\n📊 Testing {portfolio_name.upper()}:")
        print("-" * 40)
        
        adjustments = {}
        
        # Test risk patterns
        for i, pattern in enumerate(risk_patterns):
            match = re.search(pattern, ai_text, re.DOTALL | re.IGNORECASE)
            if match:
                adjustments['risk'] = {
                    'adjustment': match.group(1),
                    'reasoning': match.group(2).strip(),
                    'pattern_used': f"Risk Pattern {i+1}"
                }
                print(f"✅ Risk adjustment found: {match.group(1)} (Pattern {i+1})")
                break
        
        # Test diversification patterns
        for i, pattern in enumerate(div_patterns):
            match = re.search(pattern, ai_text, re.DOTALL | re.IGNORECASE)
            if match:
                adjustments['diversification'] = {
                    'adjustment': match.group(1),
                    'reasoning': match.group(2).strip(),
                    'pattern_used': f"Diversification Pattern {i+1}"
                }
                print(f"✅ Diversification adjustment found: {match.group(1)} (Pattern {i+1})")
                break
        
        # Test goal alignment patterns
        for i, pattern in enumerate(goal_patterns):
            match = re.search(pattern, ai_text, re.DOTALL | re.IGNORECASE)
            if match:
                adjustments['goalAlignment'] = {
                    'adjustment': match.group(1),
                    'reasoning': match.group(2).strip(),
                    'pattern_used': f"Goal Alignment Pattern {i+1}"
                }
                print(f"✅ Goal alignment adjustment found: {match.group(1)} (Pattern {i+1})")
                break
        
        if adjustments:
            print(f"🎯 All adjustments extracted successfully!")
            print(f"Adjustments: {json.dumps(adjustments, indent=2)}")
        else:
            print("❌ No adjustments extracted!")
            
            # Try fallback patterns
            print("🔍 Trying fallback patterns...")
            
            risk_fallback = re.search(r'-\s*Risk\s+Score\s+Adjustment:\s*([+-]?\d+).*?\n\s*-\s*(.*?)(?=\n|$)', ai_text, re.DOTALL | re.IGNORECASE)
            if risk_fallback:
                print(f"✅ Risk fallback: {risk_fallback.group(1)}")
            
            div_fallback = re.search(r'-\s*Diversification\s+Adjustment:\s*([+-]?\d+%).*?\n\s*-\s*(.*?)(?=\n|$)', ai_text, re.DOTALL | re.IGNORECASE)
            if div_fallback:
                print(f"✅ Diversification fallback: {div_fallback.group(1)}")
            
            goal_fallback = re.search(r'-\s*Goal\s+Alignment\s+Adjustment:\s*([+-]?\d+%).*?\n\s*-\s*(.*?)(?=\n|$)', ai_text, re.DOTALL | re.IGNORECASE)
            if goal_fallback:
                print(f"✅ Goal alignment fallback: {goal_fallback.group(1)}")

def main():
    """Run the regex pattern tests"""
    test_regex_patterns()
    print(f"\n✅ Regex pattern testing completed!")

if __name__ == "__main__":
    main() 