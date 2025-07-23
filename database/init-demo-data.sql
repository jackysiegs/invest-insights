-- Demo Data Initialization for RJ Invest Insights
-- This script creates the ii-Admin advisor and demo clients with portfolios

-- Insert the ii-Admin advisor
-- Password: RayJay1! (BCrypt hash from working system)
INSERT INTO advisor (id, name, email, username, password_hash, created_at) 
VALUES (11, 'Admin User', 'admin@investinsights.com', 'ii-Admin', '$2a$10$CUbulPsoTxQlqMb0s86kFedD4kenS0vJZNWqkeCVLkqdrySGTTQJK', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Insert demo clients
INSERT INTO client (id, name, email, risk_tolerance, investment_goal, time_horizon, advisor_id, created_at) VALUES
(1, 'Sashank Pandem', 'sashank.pandem@email.com', 'Moderate', 'Retirement savings', 25, 11, CURRENT_TIMESTAMP),
(2, 'Jack Siegel', 'jack.siegel@email.com', 'Aggressive', 'Wealth building', 15, 11, CURRENT_TIMESTAMP),
(3, 'Sarah Chen', 'sarah.chen@email.com', 'Conservative', 'Income generation', 10, 11, CURRENT_TIMESTAMP),
(4, 'Michael Rodriguez', 'michael.rodriguez@email.com', 'Moderate', 'College savings', 8, 11, CURRENT_TIMESTAMP),
(5, 'Emily Watson', 'emily.watson@email.com', 'Aggressive', 'Early retirement', 20, 11, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Insert portfolios for each client
INSERT INTO portfolio (id, name, description, client_id, created_at) VALUES
-- Sashank's portfolios (Good examples)
(1, 'Balanced Retirement', 'Well-diversified portfolio for retirement planning', 1, CURRENT_TIMESTAMP),
(2, 'Growth Focus', 'Growth-oriented portfolio with moderate risk', 1, CURRENT_TIMESTAMP),

-- Jack's portfolios (Mixed examples)
(3, 'Tech Growth', 'Technology-heavy growth portfolio', 2, CURRENT_TIMESTAMP),
(4, 'Diversified Growth', 'Well-balanced growth portfolio', 2, CURRENT_TIMESTAMP),

-- Sarah's portfolios (Conservative examples)
(5, 'Conservative Income', 'Bond-heavy income portfolio', 3, CURRENT_TIMESTAMP),
(6, 'Balanced Conservative', 'Conservative with some growth', 3, CURRENT_TIMESTAMP),

-- Michael's portfolios (Problem examples)
(7, 'Single Stock Risk', 'Heavily concentrated in one stock', 4, CURRENT_TIMESTAMP),
(8, 'Sector Concentration', 'Overweight in technology sector', 4, CURRENT_TIMESTAMP),

-- Emily's portfolios (Aggressive examples)
(9, 'Aggressive Growth', 'High-growth aggressive portfolio', 5, CURRENT_TIMESTAMP),
(10, 'International Focus', 'Heavy international exposure', 5, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Insert holdings for each portfolio
-- Portfolio 1: Balanced Retirement (Good example)
INSERT INTO holding (id, ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
(1, 'SPY', 200, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 1),
(2, 'BND', 800, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 1),
(3, 'QQQ', 100, 385.40, 'Technology', 'ETF', 1.25, 0.65, 1),
(4, 'VTI', 150, 245.80, 'Diversified', 'ETF', 0.95, 1.85, 1),
(5, 'VXUS', 300, 55.20, 'International', 'ETF', 0.90, 2.10, 1);

-- Portfolio 2: Growth Focus (Good example)
INSERT INTO holding (id, ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
(6, 'VTI', 300, 245.80, 'Diversified', 'ETF', 0.95, 1.85, 2),
(7, 'VXUS', 200, 55.20, 'International', 'ETF', 0.90, 2.10, 2),
(8, 'QQQ', 150, 385.40, 'Technology', 'ETF', 1.25, 0.65, 2),
(9, 'VYM', 100, 110.30, 'Diversified', 'ETF', 0.85, 3.15, 2),
(10, 'BND', 400, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 2);

-- Portfolio 3: Tech Growth (Problem example - concentration risk)
INSERT INTO holding (id, ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
(11, 'QQQ', 500, 385.40, 'Technology', 'ETF', 1.25, 0.65, 3),
(12, 'AAPL', 200, 175.60, 'Technology', 'Stock', 1.35, 0.50, 3),
(13, 'MSFT', 150, 380.20, 'Technology', 'Stock', 1.15, 0.80, 3),
(14, 'NVDA', 100, 850.80, 'Technology', 'Stock', 1.85, 0.15, 3),
(15, 'TSLA', 50, 240.40, 'Technology', 'Stock', 2.45, 0.00, 3);

-- Portfolio 4: Diversified Growth (Good example)
INSERT INTO holding (id, ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
(16, 'VTI', 400, 245.80, 'Diversified', 'ETF', 0.95, 1.85, 4),
(17, 'VXUS', 300, 55.20, 'International', 'ETF', 0.90, 2.10, 4),
(18, 'QQQ', 100, 385.40, 'Technology', 'ETF', 1.25, 0.65, 4),
(19, 'VNQ', 200, 85.60, 'Real Estate', 'ETF', 0.75, 4.20, 4),
(20, 'GLD', 50, 195.30, 'Commodities', 'ETF', 0.05, 0.00, 4);

-- Portfolio 5: Conservative Income (Conservative example)
INSERT INTO holding (id, ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
(21, 'BND', 1200, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 5),
(22, 'VYM', 300, 110.30, 'Diversified', 'ETF', 0.85, 3.15, 5),
(23, 'VNQ', 150, 85.60, 'Real Estate', 'ETF', 0.75, 4.20, 5),
(24, 'TIP', 200, 105.20, 'Fixed Income', 'ETF', 0.20, 2.80, 5),
(25, 'SPY', 50, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 5);

-- Portfolio 6: Balanced Conservative (Good example)
INSERT INTO holding (id, ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
(26, 'BND', 800, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 6),
(27, 'SPY', 150, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 6),
(28, 'VYM', 200, 110.30, 'Diversified', 'ETF', 0.85, 3.15, 6),
(29, 'VNQ', 100, 85.60, 'Real Estate', 'ETF', 0.75, 4.20, 6),
(30, 'VXUS', 100, 55.20, 'International', 'ETF', 0.90, 2.10, 6);

-- Portfolio 7: Single Stock Risk (Problem example - extreme concentration)
INSERT INTO holding (id, ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
(31, 'AAPL', 1000, 175.60, 'Technology', 'Stock', 1.35, 0.50, 7),
(32, 'SPY', 50, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 7),
(33, 'BND', 100, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 7);

-- Portfolio 8: Sector Concentration (Problem example - tech overweight)
INSERT INTO holding (id, ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
(34, 'QQQ', 400, 385.40, 'Technology', 'ETF', 1.25, 0.65, 8),
(35, 'AAPL', 300, 175.60, 'Technology', 'Stock', 1.35, 0.50, 8),
(36, 'MSFT', 200, 380.20, 'Technology', 'Stock', 1.15, 0.80, 8),
(37, 'NVDA', 100, 850.80, 'Technology', 'Stock', 1.85, 0.15, 8),
(38, 'SPY', 100, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 8);

-- Portfolio 9: Aggressive Growth (Aggressive example)
INSERT INTO holding (id, ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
(39, 'QQQ', 300, 385.40, 'Technology', 'ETF', 1.25, 0.65, 9),
(40, 'VTI', 200, 245.80, 'Diversified', 'ETF', 0.95, 1.85, 9),
(41, 'AAPL', 150, 175.60, 'Technology', 'Stock', 1.35, 0.50, 9),
(42, 'MSFT', 100, 380.20, 'Technology', 'Stock', 1.15, 0.80, 9),
(43, 'VXUS', 150, 55.20, 'International', 'ETF', 0.90, 2.10, 9);

-- Portfolio 10: International Focus (Problem example - currency risk)
INSERT INTO holding (id, ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
(44, 'VXUS', 800, 55.20, 'International', 'ETF', 0.90, 2.10, 10),
(45, 'VWO', 400, 42.80, 'International', 'ETF', 1.15, 2.85, 10),
(46, 'EFA', 300, 75.60, 'International', 'ETF', 0.95, 2.45, 10),
(47, 'SPY', 100, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 10),
(48, 'BND', 100, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 10)
ON CONFLICT (id) DO NOTHING;

-- Insert some sample investment insights for demonstration
INSERT INTO investment_insight (id, summary, ai_generated_text, created_at, portfolio_id) VALUES
(1, 'Tech concentration risk: 62% in single sector', 'Based on your portfolio analysis, there is significant concentration risk in the technology sector...', CURRENT_TIMESTAMP, 3),
(2, 'Well-diversified portfolio with balanced risk', 'Your portfolio shows excellent diversification across asset classes...', CURRENT_TIMESTAMP, 1),
(3, 'Conservative approach suitable for income goals', 'This conservative portfolio aligns well with your income generation goals...', CURRENT_TIMESTAMP, 5),
(4, 'Single stock concentration requires immediate attention', 'Your portfolio has extreme concentration risk with 85% in AAPL...', CURRENT_TIMESTAMP, 7),
(5, 'International overweight may increase currency risk', 'Your portfolio has 65% international exposure which may introduce currency risk...', CURRENT_TIMESTAMP, 10)
ON CONFLICT (id) DO NOTHING; 