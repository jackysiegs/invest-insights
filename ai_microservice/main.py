from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from datetime import datetime, timedelta
from openai import OpenAI
from dotenv import load_dotenv
import os
import requests
import json
import re

# Global cache for market news
market_news_cache = {
    'data': None,
    'timestamp': None,
    'cache_duration': timedelta(minutes=30)  # Cache for 30 minutes
}

load_dotenv()
openai_client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
FINNHUB_API_KEY = os.getenv("FINNHUB_API_KEY")

app = FastAPI()

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allow all origins for debugging
    allow_credentials=False,  # Set to False when using allow_origins=["*"]
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["*"],
)

class InsightRequest(BaseModel):
    holdings: list[str]
    preferences: str

class InsightSection(BaseModel):
    title: str
    content: str
    keyMetrics: list[str] = []
    riskLevel: str = "neutral"  # low, medium, high, neutral

class AssetRecommendation(BaseModel):
    ticker: str
    assetName: str
    allocation: str
    category: str
    reasoning: str
    priority: str
    expectedImpact: str

class StructuredInsight(BaseModel):
    mainRecommendations: list[str] = []
    sections: dict[str, InsightSection] = {}
    rawResponse: str = ""
    qualitativeInsights: dict[str, str] = {}  # Store qualitative insights by category
    scoreAdjustments: dict[str, dict[str, str]] = {}  # Store AI score adjustments
    sectionSpecificAnalysis: dict[str, str] = {}  # Store section-specific AI analysis
    assetRecommendations: list[AssetRecommendation] = []  # Store structured asset recommendations

class InvestmentInsight(BaseModel):
    summary: str
    aiGeneratedText: str
    createdAt: str  # Changed to str to match Java's expected format
    structuredInsight: StructuredInsight | None = None
    
    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "summary": "Tech concentration risk: 62% in single sector",
                    "aiGeneratedText": "Based on your portfolio...",
                    "createdAt": "2024-01-01T00:00:00"
                }
            ]
        }
    }

def parse_asset_recommendations(content: str) -> list[AssetRecommendation]:
    """Parse asset recommendations from AI text content"""
    recommendations = []
    
    lines = content.split('\n')
    current_recommendation = None
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
            
        # Check for numbered recommendations (1., 2., 3., etc.)
        # Handle both formats: "1. VYM - Vanguard High Dividend Yield ETF" and "1. [VYM] - Vanguard High Dividend Yield ETF"
        if re.match(r'^\d+\.\s*\[?[A-Z]+\]?\s*-\s*', line):
            # Save previous recommendation if exists
            if current_recommendation:
                recommendations.append(current_recommendation)
            
            # Start new recommendation
            parts = line.split(' - ', 1)
            if len(parts) == 2:
                # Extract ticker, handling both [TICKER] and TICKER formats
                ticker_part = parts[0].split('.', 1)[1].strip()
                ticker = ticker_part.strip('[]')  # Remove brackets if present
                asset_name = parts[1].strip()
                current_recommendation = AssetRecommendation(
                    ticker=ticker,
                    assetName=asset_name,
                    allocation="",
                    category="",
                    reasoning="",
                    priority="",
                    expectedImpact=""
                )
        elif current_recommendation:
            # Parse recommendation details
            if line.lower().startswith('allocation:'):
                current_recommendation.allocation = line.split(':', 1)[1].strip()
            elif line.lower().startswith('category:'):
                current_recommendation.category = line.split(':', 1)[1].strip()
            elif line.lower().startswith('reasoning:'):
                current_recommendation.reasoning = line.split(':', 1)[1].strip()
            elif line.lower().startswith('priority:'):
                current_recommendation.priority = line.split(':', 1)[1].strip()
            elif line.lower().startswith('expected impact:'):
                current_recommendation.expectedImpact = line.split(':', 1)[1].strip()
    
    # Add the last recommendation
    if current_recommendation:
        recommendations.append(current_recommendation)
    
    return recommendations

def get_market_news(limit=5):
    """Fetch recent market news from Finhub API with caching"""
    global market_news_cache
    
    # Check if we have cached data that's still valid
    if (market_news_cache['data'] is not None and 
        market_news_cache['timestamp'] is not None and
        datetime.now() - market_news_cache['timestamp'] < market_news_cache['cache_duration']):
        print("Using cached market news")
        return market_news_cache['data'][:limit]
    
    if not FINNHUB_API_KEY:
        print("Warning: FINNHUB_API_KEY not found in environment variables")
        return []
    
    try:
        url = f"https://finnhub.io/api/v1/news?category=general&token={FINNHUB_API_KEY}"
        response = requests.get(url, timeout=10)  # Add timeout
        if response.status_code == 200:
            news_data = response.json()
            # Return structured news data with headlines, URLs, and original posting time
            news_items = []
            for article in news_data[:limit]:
                # Convert timestamp to datetime if it exists
                original_time = None
                if 'datetime' in article and article['datetime']:
                    try:
                        # Finnhub uses Unix timestamp in seconds
                        original_time = datetime.fromtimestamp(article['datetime'])
                    except (ValueError, TypeError):
                        original_time = None
                
                news_items.append({
                    'headline': article['headline'],
                    'url': article.get('url', 'https://finnhub.io'),  # Use article URL or fallback
                    'summary': article.get('summary', article['headline']),  # Use summary or headline as fallback
                    'originalTime': original_time.isoformat() if original_time else None  # Include original posting time
                })
            
            # Cache the results
            market_news_cache['data'] = news_items
            market_news_cache['timestamp'] = datetime.now()
            
            return news_items
        elif response.status_code == 429:
            print("Finhub API rate limit exceeded")
            return []
        else:
            print(f"Finhub API error: {response.status_code}")
            return []
    except requests.exceptions.Timeout:
        print("Timeout error fetching news from Finhub")
        return []
    except Exception as e:
        print(f"Error fetching news: {e}")
        return []



def parse_qualitative_response(ai_text: str) -> StructuredInsight:
    """Parse the AI response into qualitative insights and sections"""
    
    # Initialize structured insight for qualitative analysis
    structured = StructuredInsight(
        mainRecommendations=[],
        sections={},
        rawResponse=ai_text,
        qualitativeInsights={}
    )
    
    # Debug information removed for stability
    
    # Parse the AI text into sections
    lines = ai_text.split('\n')
    current_section = None
    current_content = []
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
            
        # Check for section headers - handle both exact and partial matches
        if line.upper().startswith(('PORTFOLIO INSIGHTS', 'PORTFOLIO')):
            if current_section and current_content:
                structured.sections[current_section] = InsightSection(
                    title=current_section.replace('_', ' ').title(),
                    content='\n'.join(current_content)
                )
            current_section = 'portfolio_insights'
            current_content = []
        elif line.upper().startswith(('RISK ASSESSMENT', 'RISK')):
            if current_section and current_content:
                structured.sections[current_section] = InsightSection(
                    title=current_section.replace('_', ' ').title(),
                    content='\n'.join(current_content)
                )
            current_section = 'risk_assessment'
            current_content = []
        elif line.upper().startswith(('DIVERSIFICATION ANALYSIS', 'DIVERSIFICATION')):
            if current_section and current_content:
                structured.sections[current_section] = InsightSection(
                    title=current_section.replace('_', ' ').title(),
                    content='\n'.join(current_content)
                )
            current_section = 'diversification_analysis'
            current_content = []
        elif line.upper().startswith(('GOAL ALIGNMENT', 'GOAL')):
            if current_section and current_content:
                structured.sections[current_section] = InsightSection(
                    title=current_section.replace('_', ' ').title(),
                    content='\n'.join(current_content)
                )
            current_section = 'goal_alignment'
            current_content = []
        elif line.upper().startswith(('PERFORMANCE ANALYSIS', 'PERFORMANCE')):
            if current_section and current_content:
                structured.sections[current_section] = InsightSection(
                    title=current_section.replace('_', ' ').title(),
                    content='\n'.join(current_content)
                )
            current_section = 'performance_analysis'
            current_content = []
        elif line.upper().startswith(('MACRO ANALYSIS', 'MACRO')):
            if current_section and current_content:
                structured.sections[current_section] = InsightSection(
                    title=current_section.replace('_', ' ').title(),
                    content='\n'.join(current_content)
                )
            current_section = 'macro_analysis'
            current_content = []
        elif line.upper().startswith(('MARKET CONTEXT', 'MARKET')):
            if current_section and current_content:
                structured.sections[current_section] = InsightSection(
                    title=current_section.replace('_', ' ').title(),
                    content='\n'.join(current_content)
                )
            current_section = 'market_context'
            current_content = []
        elif line.upper().startswith(('ASSET RECOMMENDATIONS', 'ASSET')):
            if current_section and current_content:
                structured.sections[current_section] = InsightSection(
                    title=current_section.replace('_', ' ').title(),
                    content='\n'.join(current_content)
                )
            current_section = 'asset_recommendations'
            current_content = []
        elif line.upper().startswith(('SPECIFIC RECOMMENDATIONS', 'SPECIFIC')):
            if current_section and current_content:
                structured.sections[current_section] = InsightSection(
                    title=current_section.replace('_', ' ').title(),
                    content='\n'.join(current_content)
                )
            current_section = 'specific_recommendations'
            current_content = []
        elif line.upper().startswith(('NEXT STEPS', 'NEXT')):
            if current_section and current_content:
                structured.sections[current_section] = InsightSection(
                    title=current_section.replace('_', ' ').title(),
                    content='\n'.join(current_content)
                )
            current_section = 'next_steps'
            current_content = []
        elif line.upper().startswith(('SUMMARY')):
            # Skip summary section - it's handled separately
            continue
        elif line.upper().startswith(('=== SCORE ADJUSTMENT REQUEST ===', 'SCORE ADJUSTMENT REQUEST', 'RISK SCORE ADJUSTMENT', 'PORTFOLIO TYPE CLASSIFICATION')):
            # Stop parsing when we hit score adjustments or portfolio classification
            break
        else:
            # Skip lines that contain score adjustment patterns
            if any(pattern in line.upper() for pattern in ['SUGGESTED ADJUSTMENT:', 'BRIEF REASONING:', 'RISK SCORE ADJUSTMENT:', 'DIVERSIFICATION ADJUSTMENT:', 'GOAL ALIGNMENT ADJUSTMENT:']):
                continue
            
            # Add content to current section
            if current_section:
                current_content.append(line)
    
    # Add the last section
    if current_section and current_content:
        structured.sections[current_section] = InsightSection(
            title=current_section.replace('_', ' ').title(),
            content='\n'.join(current_content)
        )
    
    # Parse asset recommendations if available
    asset_rec_section = None
    if 'asset_recommendations' in structured.sections:
        asset_rec_section = structured.sections['asset_recommendations']
    elif 'specific_recommendations' in structured.sections:
        asset_rec_section = structured.sections['specific_recommendations']
    
    if asset_rec_section:
        print(f"🔍 Found asset recommendations section, parsing...")
        structured.assetRecommendations = parse_asset_recommendations(
            asset_rec_section.content
        )
        print(f"✅ Parsed {len(structured.assetRecommendations)} asset recommendations")
        for i, rec in enumerate(structured.assetRecommendations):
            print(f"  {i+1}. {rec.ticker} - {rec.assetName}")
    else:
        print(f"⚠️ No asset recommendations section found in structured sections")
        print(f"Available sections: {list(structured.sections.keys())}")
    
    # Extract main recommendations from Next Steps section
    next_steps_section = structured.sections.get('next_steps')
    if next_steps_section:
        # Split next steps into individual items
        step_lines = next_steps_section.content.split('\n')
        for line in step_lines:
            line = line.strip()
            if line and (line.startswith('-') or line.startswith('•') or line.startswith('*')):
                # Remove bullet points and clean up
                clean_step = line.lstrip('-•* ').strip()
                if clean_step:
                    structured.mainRecommendations.append(clean_step)
    
    # Store qualitative insights by category
    for section_name, section in structured.sections.items():
        structured.qualitativeInsights[section_name] = section.content
    
    # Extract section-specific AI analysis content from the parsed sections
    structured.sectionSpecificAnalysis = {}
    for section_key, section in structured.sections.items():
        structured.sectionSpecificAnalysis[section_key] = section.content
    
    # Extract score adjustments if present
    import re
    
    # Look for score adjustment section
    adjustment_patterns = [
        r'SCORE ADJUSTMENT REQUEST:(.*?)(?=\n\n|\Z)',
        r'=== SCORE ADJUSTMENT REQUEST ===(.*?)(?=\n\n|\Z)',
        r'RISK SCORE ADJUSTMENT:(.*?)(?=DIVERSIFICATION ADJUSTMENT:|GOAL ALIGNMENT ADJUSTMENT:|\Z)',
        r'DIVERSIFICATION ADJUSTMENT:(.*?)(?=GOAL ALIGNMENT ADJUSTMENT:|\Z)',
        r'GOAL ALIGNMENT ADJUSTMENT:(.*?)(?=\n\n|\Z)'
    ]
    
    # Extract score adjustments with standardized format
    # The AI now uses a consistent format, so we only need one pattern per adjustment type
    
    # Risk score adjustment pattern (standardized format)
    risk_pattern = r'RISK\s+SCORE\s+ADJUSTMENT:.*?Suggested\s+adjustment:\s*([+-]?\d+).*?Brief\s+reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)'
    
    risk_adjustment_match = re.search(risk_pattern, ai_text, re.DOTALL | re.IGNORECASE)
    if risk_adjustment_match:
        structured.scoreAdjustments['risk'] = {
            'adjustment': risk_adjustment_match.group(1),
            'reasoning': risk_adjustment_match.group(2).strip()
        }
    
    # Diversification adjustment pattern (standardized format)
    div_pattern = r'DIVERSIFICATION\s+ADJUSTMENT:.*?Suggested\s+adjustment:\s*([+-]?\d+%).*?Brief\s+reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)'
    
    div_adjustment_match = re.search(div_pattern, ai_text, re.DOTALL | re.IGNORECASE)
    if div_adjustment_match:
        structured.scoreAdjustments['diversification'] = {
            'adjustment': div_adjustment_match.group(1),
            'reasoning': div_adjustment_match.group(2).strip()
        }
    
    # Goal alignment adjustment pattern (standardized format)
    # Handle both "0" and "0%" formats for consistency
    goal_pattern = r'GOAL\s+ALIGNMENT\s+ADJUSTMENT:.*?Suggested\s+adjustment:\s*([+-]?\d+%?).*?Brief\s+reasoning:\s*(.*?)(?=\n\n|\n[A-Z]|$)'
    
    goal_adjustment_match = re.search(goal_pattern, ai_text, re.DOTALL | re.IGNORECASE)
    if goal_adjustment_match:
        adjustment = goal_adjustment_match.group(1)
        # Ensure adjustment has % symbol for consistency
        if not adjustment.endswith('%'):
            adjustment += '%'
        structured.scoreAdjustments['goalAlignment'] = {
            'adjustment': adjustment,
            'reasoning': goal_adjustment_match.group(2).strip()
        }
    
    # Debug: Log what was extracted
    print(f"📊 Parsed {len(structured.sections)} analysis sections:")
    for section_key, section in structured.sections.items():
        print(f"  - {section_key}: {len(section.content.split())} words")
    
    # Check for missing sections
    expected_sections = ['risk_assessment', 'diversification_analysis', 'goal_alignment', 'performance_analysis', 'macro_analysis', 'market_context', 'next_steps']
    missing_sections = [section for section in expected_sections if section not in structured.sections]
    if missing_sections:
        print(f"⚠️ Missing sections: {missing_sections}")
        print("🔍 Checking AI response for these sections...")
        for section in missing_sections:
            if section.upper().replace('_', ' ') in ai_text.upper():
                print(f"  - {section}: Found in AI response")
            else:
                print(f"  - {section}: NOT found in AI response")
    
    if structured.scoreAdjustments:
        print(f"✅ Successfully extracted {len(structured.scoreAdjustments)} adjustments:")
        for adj_type, adj_data in structured.scoreAdjustments.items():
            print(f"  - {adj_type}: {adj_data['adjustment']}")
    else:
        print("❌ No adjustments extracted - this should not happen with standardized format")
        # Try to find any adjustment patterns in the text
        if 'RISK SCORE ADJUSTMENT:' in ai_text:
            print("  - Risk adjustment section found but not parsed")
        if 'DIVERSIFICATION ADJUSTMENT:' in ai_text:
            print("  - Diversification adjustment section found but not parsed")
        if 'GOAL ALIGNMENT ADJUSTMENT:' in ai_text:
            print("  - Goal alignment adjustment section found but not parsed")
    
    return structured

def get_company_specific_news(tickers, limit=3):
    """Fetch company-specific news for portfolio holdings"""
    if not FINNHUB_API_KEY:
        return []
    
    company_news = []
    try:
        for ticker in tickers[:3]:  # Limit to first 3 tickers to avoid API limits
            url = f"https://finnhub.io/api/v1/company-news?symbol={ticker}&from=2024-01-01&to=2024-12-31&token={FINNHUB_API_KEY}"
            response = requests.get(url, timeout=10)
            if response.status_code == 200:
                news_data = response.json()
                if news_data:
                    # Get the most recent news
                    latest_news = news_data[0]
                    # Convert timestamp to datetime if it exists
                    original_time = None
                    if 'datetime' in latest_news and latest_news['datetime']:
                        try:
                            # Finnhub uses Unix timestamp in seconds
                            original_time = datetime.fromtimestamp(latest_news['datetime'])
                        except (ValueError, TypeError):
                            original_time = None
                    
                    company_news.append({
                        'headline': f"{ticker}: {latest_news['headline']}",
                        'url': latest_news.get('url', f'https://finnhub.io/quote/{ticker}'),  # Use article URL or ticker page
                        'summary': latest_news.get('summary', latest_news['headline']),  # Use summary or headline as fallback
                        'originalTime': original_time.isoformat() if original_time else None  # Include original posting time
                    })
            elif response.status_code == 429:
                print(f"Rate limit exceeded for {ticker}")
                break
            else:
                print(f"Error fetching news for {ticker}: {response.status_code}")
    except requests.exceptions.Timeout:
        print("Timeout error fetching company news")
    except Exception as e:
        print(f"Error fetching company news: {e}")
    
    return company_news

def generate_insight_summary(ai_text: str, holdings: list[str]) -> str:
    """Extract the SUMMARY section from the AI response"""
    
    lines = ai_text.split('\n')
    
    # Look for the SUMMARY section
    for i, line in enumerate(lines):
        line = line.strip()
        if line.upper().startswith('SUMMARY:'):
            # Extract the summary content from the same line after "SUMMARY:"
            summary_content = line[8:].strip()  # Remove "SUMMARY:" prefix
            if summary_content:
                return summary_content[:80]
            # If no content on same line, look at next line
            for j in range(i + 1, min(i + 5, len(lines))):
                summary_line = lines[j].strip()
                if summary_line and not summary_line.upper().startswith(('PORTFOLIO', 'MARKET', 'RISK', 'DIVERSIFICATION', 'GOAL', 'SPECIFIC', 'NEXT', 'SCORE')):
                    return summary_line[:80]
        elif line.upper().startswith('SUMMARY'):
            # Handle case where SUMMARY is on its own line
            for j in range(i + 1, min(i + 5, len(lines))):
                summary_line = lines[j].strip()
                if summary_line and not summary_line.upper().startswith(('PORTFOLIO', 'MARKET', 'RISK', 'DIVERSIFICATION', 'GOAL', 'SPECIFIC', 'NEXT', 'SCORE')):
                    return summary_line[:80]
    
    # Fallback: Look for any meaningful statement in the first few lines
    for line in lines[:10]:
        line = line.strip()
        if line and len(line) <= 80 and line[0].isupper():
            if any(keyword in line.lower() for keyword in ['review', 'consider', 'recommend', 'risk', 'diversification']):
                return line[:80]
    
    # Final fallback
    return f"Portfolio analysis: {len(holdings)} holdings reviewed"

@app.get("/health")
def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "service": "ai-microservice"}

@app.get("/market-news")
def get_market_news_endpoint():
    """Get market news headlines"""
    headlines = get_market_news(limit=3)
    return {"headlines": headlines}

class PortfolioNewsRequest(BaseModel):
    portfolio_id: str
    holdings: list[str]

@app.options("/portfolio-news")
def portfolio_news_options():
    """Handle OPTIONS requests for CORS preflight"""
    return {"message": "OK"}

@app.post("/portfolio-news")
def get_portfolio_news_endpoint(request: PortfolioNewsRequest):
    """Get portfolio-specific news headlines based on holdings"""
    portfolio_id = request.portfolio_id
    holdings = request.holdings
    
    print(f"Getting portfolio-specific news for portfolio {portfolio_id} with holdings: {holdings}")
    
    # Get company-specific news for portfolio holdings
    company_news = get_company_specific_news(holdings, limit=2)
    
    # Get general market news
    general_news = get_market_news(limit=1)
    
    # Combine company-specific and general news
    all_news = []
    
    # Add company-specific news first (most relevant)
    if company_news:
        all_news.extend(company_news)
    
    # Add general market news for context
    if general_news:
        all_news.extend(general_news)
    
    # If we don't have enough news, add some default portfolio-relevant news
    while len(all_news) < 3:
        if len(all_news) == 0:
            all_news.append({
                'headline': 'Portfolio analysis shows continued market opportunities and strategic positioning.',
                'url': 'https://finnhub.io',
                'summary': 'Portfolio analysis shows continued market opportunities and strategic positioning.',
                'originalTime': None
            })
        elif len(all_news) == 1:
            all_news.append({
                'headline': 'Focus on diversification and risk management for optimal portfolio performance.',
                'url': 'https://finnhub.io',
                'summary': 'Focus on diversification and risk management for optimal portfolio performance.',
                'originalTime': None
            })
        else:
            all_news.append({
                'headline': 'Current market conditions support strategic portfolio adjustments and rebalancing.',
                'url': 'https://finnhub.io',
                'summary': 'Current market conditions support strategic portfolio adjustments and rebalancing.',
                'originalTime': None
            })
    
    # Return up to 3 news items
    return {"headlines": all_news[:3], "portfolio_id": portfolio_id}

@app.post("/generate-insight", response_model=InvestmentInsight)
def generate_insight(request: InsightRequest):
    """
    Generate qualitative investment insights and recommendations
    """
    # Fetch market news
    market_headlines = get_market_news(limit=5)
    company_news = get_company_specific_news(request.holdings, limit=3)
    
    # Build news section
    news_section = "=== RECENT MARKET NEWS ===\n"
    if market_headlines:
        news_section += "General: " + "; ".join([h['headline'] for h in market_headlines[:2]]) + "\n"
    if company_news:
        news_section += "Company: " + "; ".join([h['headline'] for h in company_news[:2]]) + "\n"
    
    # Create qualitative analysis prompt with score adjustments
    qualitative_prompt = f"""
{request.preferences}

{news_section}

=== QUALITATIVE ANALYSIS REQUEST ===
Based on the portfolio data above and recent market news, provide comprehensive qualitative insights and recommendations.

1. RISK ASSESSMENT:
   Analyze primary risk factors, concentration risks, volatility, market sensitivity, and specific scenarios.

2. DIVERSIFICATION ANALYSIS:
   Assess sector allocation, missing sectors, geographic exposure, asset class diversification, and recommendations.

3. GOAL ALIGNMENT:
   Evaluate how well the portfolio matches objectives, time horizon, risk tolerance, and income vs. growth goals.

4. PERFORMANCE ANALYSIS:
   Analyze expected performance, benchmark comparison, key drivers, and risk-adjusted return expectations.

5. MACRO ANALYSIS:
   Analyze economic trends, interest rates, inflation, geopolitical factors, and their portfolio impact.

6. MARKET CONTEXT:
   Assess current market conditions, sector trends, news affecting holdings, and market outlook.

7. ASSET RECOMMENDATIONS:
   Provide 3-5 specific asset recommendations with tickers, allocation percentages, and reasoning.

8. NEXT STEPS:
   Outline immediate actions, monitoring priorities, and strategic adjustments.

Format your response with these sections:
- SUMMARY (one line, max 80 characters)
- Risk Assessment
- Diversification Analysis
- Goal Alignment
- Performance Analysis
- Macro Analysis
- Market Context
- Asset Recommendations
- Next Steps

Use exact section headers. Each section should be 200-400 words with specific percentages and actionable insights.

=== ASSET RECOMMENDATIONS FORMAT ===
Provide 3-5 recommendations in this format:

1. [TICKER] - [ASSET NAME]
   Allocation: [X%] of portfolio
   Category: [ETF/Stock/Bond/Mutual Fund/REIT/Commodity/International/etc.]
   Reasoning: [Why this asset is recommended]
   Priority: [High/Medium/Low]
   Expected Impact: [How this will improve the portfolio]
   Expected Impact: [How this will improve the portfolio]

[Continue for 3-5 total recommendations]

ASSET TYPE DIVERSITY REQUIREMENTS:
- Provide a MIX of different asset types, not just ETFs
- Include individual stocks for growth potential and specific sector exposure
- Include bonds for stability and income (especially for conservative clients)
- Include mutual funds for professional management and diversification
- Include REITs for real estate exposure and income
- Include commodities for inflation protection and diversification
- Consider the client's risk tolerance and goals when selecting asset types

ASSET TYPE EXAMPLES:
- ETFs: VTI, SPY, QQQ, VXUS, BND, VYM, VNQ, GLD, SLV
- Individual Stocks: AAPL, MSFT, JNJ, JPM, BRK.B, GOOGL, AMZN, TSLA, NVDA, META
- Bonds: TLT, AGG, BND, LQD, HYG, TIP, IEF
- Mutual Funds: VTSAX, VFIAX, VWELX, VWINX, VWNDX
- REITs: VNQ, O, PLD, AMT, CCI, SPG, EQR
- Commodities: GLD, SLV, DJP, USO, UNG, DBA

PRIORITY ASSIGNMENT GUIDELINES:
- HIGH PRIORITY: Addresses critical portfolio gaps, major diversification issues, or significant goal misalignment
- MEDIUM PRIORITY: Improves portfolio balance, addresses moderate gaps, or enhances existing strengths
- LOW PRIORITY: Nice-to-have additions, minor improvements, or opportunistic suggestions

EXAMPLES:
- HIGH: Adding individual stock (AAPL) for technology growth when portfolio lacks tech exposure
- HIGH: Adding bond fund (BND) when conservative client has 0% fixed income
- HIGH: Adding REIT (VNQ) for real estate exposure when portfolio has no real estate
- MEDIUM: Adding dividend stock (JNJ) for income when income goals are partially met
- MEDIUM: Adding mutual fund (VTSAX) for broad diversification
- LOW: Adding commodity ETF (GLD) for additional diversification when core needs are met

IMPORTANT: 
- Use real ticker symbols from any asset type (ETFs, stocks, bonds, mutual funds, REITs, etc.)
- Provide specific allocation percentages that add up to a reasonable amount (typically 5-20% total for new additions)
- Ensure diversity in asset types recommended (don't recommend only ETFs)
- Consider the client's risk tolerance and investment goals when selecting asset types

=== SCORE ADJUSTMENT REQUEST ===
Based on your qualitative analysis above, provide score adjustments in the EXACT format below:

PORTFOLIO TYPE CLASSIFICATION:
First, classify the portfolio type based on the holdings data:
- ETF-Heavy Portfolio: >50% of holdings are ETFs (SPY, VTI, QQQ, etc.)
- Individual Stock Portfolio: >50% of holdings are individual stocks
- Mixed Portfolio: Balanced mix of ETFs and individual stocks

ADJUSTMENT GUIDELINES BY PORTFOLIO TYPE:

ETF-Heavy Portfolios:
- Risk: Use ±1 (ETFs provide natural diversification)
- Diversification: Use ±2% (ETF concentration is less risky than stock concentration)
- Goal Alignment: Use ±2% (ETFs generally align well with goals)

Individual Stock Portfolios:
- Risk: Use ±1-2 (individual stocks have higher concentration risk)
- Diversification: Use ±5% (stock concentration requires larger adjustments)
- Goal Alignment: Use ±5% (stock selection may not align with goals)

Mixed Portfolios:
- Risk: Use ±1 (balanced approach)
- Diversification: Use ±2-5% (depends on actual composition)
- Goal Alignment: Use ±2-5% (depends on actual composition)

REASONING REQUIREMENTS:
- ALWAYS reference exact percentages: "Technology sector at 55.1%"
- ALWAYS reference specific holdings: "MSFT represents 37.7% of the portfolio"
- Distinguish between ETF concentration (less risky) and stock concentration (riskier)
- Use precise language: "extreme tech concentration (55%)" vs "moderate tech exposure (15%)"
- Reference specific tickers and weights when making adjustments

=== REQUIRED FORMAT - COPY EXACTLY ===
RISK SCORE ADJUSTMENT:
- Suggested adjustment: [Choose from: +2, +1, 0, -1, -2]
- Brief reasoning: [Explain why in 1-2 sentences with specific percentages]

DIVERSIFICATION ADJUSTMENT:
- Suggested adjustment: [Choose from: +10%, +5%, +2%, 0%, -2%, -5%, -10%]
- Brief reasoning: [Explain why in 1-2 sentences with specific percentages]

GOAL ALIGNMENT ADJUSTMENT:
- Suggested adjustment: [Choose from: +10%, +5%, +2%, 0%, -2%, -5%, -10%]
- Brief reasoning: [Explain why in 1-2 sentences with specific percentages]
=== END REQUIRED FORMAT ===

CRITICAL: For Goal Alignment and Diversification adjustments, you MUST include the % symbol (e.g., "0%" not "0"). For Risk adjustments, do NOT include % symbol.

IMPORTANT: You MUST use the exact format above. Do not use bold formatting, bullet points, or any other variations. Follow the template exactly as shown.
"""

    response = openai_client.chat.completions.create(
        model="gpt-4",
        messages=[
            {"role": "system", "content": """You are an expert financial advisor providing comprehensive, detailed qualitative portfolio analysis and investment insights. 

Your role is to:
- Analyze portfolio data and provide thorough, actionable insights
- Identify key risks and opportunities with specific details
- Give comprehensive recommendations with clear rationale
- Consider market context and news with detailed analysis
- Explain what the data means in practical terms with specific examples

CRITICAL REQUIREMENTS:
- ALWAYS reference exact percentages and specific holdings in your analysis
- Distinguish between ETF-heavy portfolios (less risky concentration) and individual stock portfolios (riskier concentration)
- Use precise language: "extreme tech concentration (55%)" vs "moderate tech exposure (15%)"
- Reference specific tickers and weights when making recommendations
- Use appropriate adjustment granularity based on portfolio type
- Provide comprehensive analysis (200-400 words per section)
- Include specific scenarios, examples, and actionable insights

Portfolio Type Recognition:
- ETF-Heavy Portfolios: Use ±2% adjustments for diversification, recognize ETF benefits
- Individual Stock Portfolios: Use ±5% adjustments for diversification, emphasize concentration risks
- Mixed Portfolios: Balanced approach based on actual composition

Focus on:
- COMPREHENSIVE ANALYSIS - Provide detailed insights with specific references
- RISK ASSESSMENT - Identify key risk factors with exact percentages and scenarios
- RECOMMENDATIONS - Provide actionable advice with specific holdings and rationale
- MARKET CONTEXT - Consider current conditions and news impact with detailed analysis
- PORTFOLIO STRATEGY - Assess overall approach with precise, comprehensive analysis

Avoid:
- Quantitative scoring (that's handled by the backend)
- Complex mathematical calculations
- Vague statements without context or percentages
- Generic analysis that doesn't reference specific holdings
- Brief or superficial analysis

Be comprehensive, specific, actionable, and insightful. Help the investor understand their portfolio deeply and make informed decisions."""},
            {"role": "user", "content": qualitative_prompt}
        ],
        temperature=0.3,  # Allow some creativity for insights
        max_tokens=4000  # Allow for comprehensive responses
    )

    message_content = response.choices[0].message.content if response.choices[0].message.content is not None else ""
    ai_text = message_content.strip()

    # Parse the AI response for qualitative insights
    structured_insight = parse_qualitative_response(ai_text)

    # Generate a meaningful summary from the AI response
    summary = generate_insight_summary(ai_text, request.holdings)

    return InvestmentInsight(
        summary=summary,
        aiGeneratedText=ai_text,
        createdAt=datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
        structuredInsight=structured_insight
    )
