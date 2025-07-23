#!/usr/bin/env python3
"""
Test script to verify Goal Alignment adjustment regex pattern fix
"""

import re

# Test cases with different Goal Alignment formats
test_cases = [
    # Case 1: Correct format with % symbol
    """
GOAL ALIGNMENT ADJUSTMENT:
- Suggested adjustment: 0%
- Brief reasoning: The portfolio's current allocation aligns well with the investor's goals of wealth preservation and income generation.
""",
    
    # Case 2: Missing % symbol (the problematic case)
    """
GOAL ALIGNMENT ADJUSTMENT:
- Suggested adjustment: 0
- Brief reasoning: The portfolio's current allocation aligns well with the investor's goals of wealth preservation and income generation.
""",
    
    # Case 3: Positive adjustment with %
    """
GOAL ALIGNMENT ADJUSTMENT:
- Suggested adjustment: +2%
- Brief reasoning: Minor adjustments to enhance alignment by increasing exposure to defensive sectors.
""",
    
    # Case 4: Negative adjustment with %
    """
GOAL ALIGNMENT ADJUSTMENT:
- Suggested adjustment: -5%
- Brief reasoning: The high tech exposure may not fully align with a moderate risk tolerance.
"""
]

def test_goal_alignment_regex():
    """Test the updated Goal Alignment regex pattern"""
    
    print("🧪 TESTING GOAL ALIGNMENT REGEX PATTERN FIX")
    print("=" * 50)
    
    # Updated regex pattern that handles both "0" and "0%" formats
    goal_pattern = r'GOAL\s+ALIGNMENT\s+ADJUSTMENT:.*?Suggested\s+adjustment:\s*([+-]?\d+%?).*?Brief\s+reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)'
    
    for i, test_case in enumerate(test_cases, 1):
        print(f"\n📊 Test Case {i}:")
        print("-" * 30)
        
        # Find the adjustment in the test case
        match = re.search(goal_pattern, test_case, re.DOTALL | re.IGNORECASE)
        
        if match:
            adjustment = match.group(1)
            reasoning = match.group(2).strip()
            
            # Ensure adjustment has % symbol for consistency
            if not adjustment.endswith('%'):
                adjustment += '%'
            
            print(f"✅ SUCCESS: Adjustment extracted")
            print(f"  - Adjustment: {adjustment}")
            print(f"  - Reasoning: {reasoning}")
        else:
            print("❌ FAILED: No adjustment extracted")
            print(f"  - Test case: {test_case.strip()}")

if __name__ == "__main__":
    test_goal_alignment_regex() 