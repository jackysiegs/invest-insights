#!/bin/bash

echo "🧹 Investment Insights Cleanup & Test Data Generation"
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080"

echo -e "${BLUE}1. Checking current insights...${NC}"
CURRENT_INSIGHTS=$(curl -s "$BASE_URL/api/insights")
echo "Current insights count: $(echo $CURRENT_INSIGHTS | jq '. | length')"

echo -e "${YELLOW}2. Deleting all insights...${NC}"
DELETE_RESPONSE=$(curl -s -X DELETE "$BASE_URL/api/insights/all")
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ All insights deleted successfully${NC}"
else
    echo -e "${RED}✗ Failed to delete insights${NC}"
    exit 1
fi

echo -e "${BLUE}3. Verifying deletion...${NC}"
VERIFY_INSIGHTS=$(curl -s "$BASE_URL/api/insights")
INSIGHT_COUNT=$(echo $VERIFY_INSIGHTS | jq '. | length')
if [ "$INSIGHT_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✓ Verification successful - no insights remaining${NC}"
else
    echo -e "${RED}✗ Verification failed - $INSIGHT_COUNT insights still exist${NC}"
    exit 1
fi

echo -e "${YELLOW}4. Generating test insights...${NC}"

# Test Insight 1 - Conservative Portfolio
echo -e "${BLUE}   Generating conservative portfolio insight...${NC}"
curl -s -X POST "$BASE_URL/api/insights/generate-portfolio/1?clientId=1" \
  -H "Content-Type: application/json" \
  -d '{"preferences": "Provide a comprehensive analysis of this conservative portfolio focusing on risk management and income generation. Consider the client'\''s conservative risk tolerance and need for stable returns."}'

# Test Insight 2 - Growth Portfolio  
echo -e "${BLUE}   Generating growth portfolio insight...${NC}"
curl -s -X POST "$BASE_URL/api/insights/generate-portfolio/2?clientId=1" \
  -H "Content-Type: application/json" \
  -d '{"preferences": "Analyze this growth portfolio with focus on capital appreciation and long-term growth potential. Consider the client'\''s growth objectives and higher risk tolerance."}'

# Test Insight 3 - Balanced Portfolio
echo -e "${BLUE}   Generating balanced portfolio insight...${NC}"
curl -s -X POST "$BASE_URL/api/insights/generate-portfolio/3?clientId=1" \
  -H "Content-Type: application/json" \
  -d '{"preferences": "Provide a balanced portfolio analysis considering both growth and income objectives. Focus on diversification and moderate risk management."}'

echo -e "${BLUE}5. Verifying new insights...${NC}"
FINAL_INSIGHTS=$(curl -s "$BASE_URL/api/insights")
FINAL_COUNT=$(echo $FINAL_INSIGHTS | jq '. | length')
echo -e "${GREEN}✓ Generated $FINAL_COUNT new insights${NC}"

echo -e "${GREEN}🎉 Cleanup and test data generation complete!${NC}"
echo -e "${BLUE}You can now test the frontend with fresh insights.${NC}" 