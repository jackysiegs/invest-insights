#!/bin/bash

# Test Holdings API endpoints
BASE_URL="http://localhost:8080/api"

echo "=== Testing Holdings API ==="

# 1. Get all holdings
echo "1. Getting all holdings..."
curl -X GET "$BASE_URL/holdings" \
  -H "Content-Type: application/json"

echo -e "\n\n"

# 2. Create a new holding
echo "2. Creating a new holding..."
curl -X POST "$BASE_URL/holdings" \
  -H "Content-Type: application/json" \
  -d '{
    "ticker": "AAPL",
    "shares": 100,
    "pricePerShare": 175.50,
    "sector": "Technology",
    "assetType": "Stock",
    "beta": 1.25,
    "dividendYield": 0.55,
    "portfolioId": 1
  }'

echo -e "\n\n"

# 3. Get holdings by portfolio
echo "3. Getting holdings for portfolio 1..."
curl -X GET "$BASE_URL/portfolios/1/holdings" \
  -H "Content-Type: application/json"

echo -e "\n\n"

# 4. Update a holding
echo "4. Updating holding with ID 1..."
curl -X PUT "$BASE_URL/holdings/1" \
  -H "Content-Type: application/json" \
  -d '{
    "ticker": "AAPL",
    "shares": 150,
    "pricePerShare": 180.00,
    "sector": "Technology",
    "assetType": "Stock",
    "beta": 1.25,
    "dividendYield": 0.55,
    "portfolioId": 1
  }'

echo -e "\n\n"

# 5. Delete a holding
echo "5. Deleting holding with ID 1..."
curl -X DELETE "$BASE_URL/holdings/1" \
  -H "Content-Type: application/json"

echo -e "\n\n=== API Testing Complete ===" 