-- Demo Data Initialization for RJ Invest Insights
-- This script creates the ii-Admin advisor and demo clients with portfolios
-- FIXED: Removed hardcoded IDs to work with auto-increment sequences
-- UPDATED: Calculates portfolio values and uses client_id for insights

-- Insert the ii-Admin advisor (let auto-increment handle ID)
-- Password: RayJay1! (BCrypt hash from working system)
INSERT INTO advisor (name, email, username, password_hash, created_at) 
VALUES ('Admin User', 'admin@investinsights.com', 'ii-Admin', '$2a$10$CUbulPsoTxQlqMb0s86kFedD4kenS0vJZNWqkeCVLkqdrySGTTQJK', CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Insert demo clients (use subquery to get advisor ID)
INSERT INTO client (name, email, risk_tolerance, investment_goal, time_horizon, advisor_id, created_at) VALUES
('Sashank Pandem', 'sashank.pandem@email.com', 'Moderate', 'Retirement savings', 25, 
 (SELECT id FROM advisor WHERE username = 'ii-Admin'), CURRENT_TIMESTAMP),
('Jack Siegel', 'jack.siegel@email.com', 'Aggressive', 'Wealth building', 15, 
 (SELECT id FROM advisor WHERE username = 'ii-Admin'), CURRENT_TIMESTAMP),
('Sarah Chen', 'sarah.chen@email.com', 'Conservative', 'Income generation', 10, 
 (SELECT id FROM advisor WHERE username = 'ii-Admin'), CURRENT_TIMESTAMP),
('Michael Rodriguez', 'michael.rodriguez@email.com', 'Moderate', 'College savings', 8, 
 (SELECT id FROM advisor WHERE username = 'ii-Admin'), CURRENT_TIMESTAMP),
('Emily Watson', 'emily.watson@email.com', 'Aggressive', 'Early retirement', 20, 
 (SELECT id FROM advisor WHERE username = 'ii-Admin'), CURRENT_TIMESTAMP);

-- Insert portfolios for each client (use subqueries for client IDs)
INSERT INTO portfolio (name, description, client_id, account_type, created_at) VALUES
-- Sashank's portfolios (Good examples)
('Balanced Retirement', 'Well-diversified portfolio for retirement planning', 
 (SELECT id FROM client WHERE email = 'sashank.pandem@email.com'), 'Traditional IRA', CURRENT_TIMESTAMP),
('Growth Focus', 'Growth-oriented portfolio with moderate risk', 
 (SELECT id FROM client WHERE email = 'sashank.pandem@email.com'), 'Roth IRA', CURRENT_TIMESTAMP),

-- Jack's portfolios (Mixed examples)
('Tech Growth', 'Technology-heavy growth portfolio', 
 (SELECT id FROM client WHERE email = 'jack.siegel@email.com'), 'Brokerage', CURRENT_TIMESTAMP),
('Diversified Growth', 'Well-balanced growth portfolio', 
 (SELECT id FROM client WHERE email = 'jack.siegel@email.com'), '401k', CURRENT_TIMESTAMP),

-- Sarah's portfolios (Conservative examples)
('Conservative Income', 'Bond-heavy income portfolio', 
 (SELECT id FROM client WHERE email = 'sarah.chen@email.com'), 'Traditional IRA', CURRENT_TIMESTAMP),
('Balanced Conservative', 'Conservative with some growth', 
 (SELECT id FROM client WHERE email = 'sarah.chen@email.com'), 'Brokerage', CURRENT_TIMESTAMP),

-- Michael's portfolios (Problem examples)
('Single Stock Risk', 'Heavily concentrated in one stock', 
 (SELECT id FROM client WHERE email = 'michael.rodriguez@email.com'), '529 Plan', CURRENT_TIMESTAMP),
('Sector Concentration', 'Overweight in technology sector', 
 (SELECT id FROM client WHERE email = 'michael.rodriguez@email.com'), 'Brokerage', CURRENT_TIMESTAMP),

-- Emily's portfolios (Aggressive examples)
('Aggressive Growth', 'High-growth aggressive portfolio', 
 (SELECT id FROM client WHERE email = 'emily.watson@email.com'), 'Roth IRA', CURRENT_TIMESTAMP),
('International Focus', 'Heavy international exposure', 
 (SELECT id FROM client WHERE email = 'emily.watson@email.com'), 'Brokerage', CURRENT_TIMESTAMP);

-- Insert holdings for each portfolio (use subqueries for portfolio IDs)
-- Portfolio: Balanced Retirement (Good example)
INSERT INTO holding (ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
('SPY', 200, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 
 (SELECT id FROM portfolio WHERE name = 'Balanced Retirement' AND client_id = (SELECT id FROM client WHERE email = 'sashank.pandem@email.com'))),
('BND', 800, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 
 (SELECT id FROM portfolio WHERE name = 'Balanced Retirement' AND client_id = (SELECT id FROM client WHERE email = 'sashank.pandem@email.com'))),
('QQQ', 100, 385.40, 'Technology', 'ETF', 1.25, 0.65, 
 (SELECT id FROM portfolio WHERE name = 'Balanced Retirement' AND client_id = (SELECT id FROM client WHERE email = 'sashank.pandem@email.com'))),
('VTI', 150, 245.80, 'Diversified', 'ETF', 0.95, 1.85, 
 (SELECT id FROM portfolio WHERE name = 'Balanced Retirement' AND client_id = (SELECT id FROM client WHERE email = 'sashank.pandem@email.com'))),
('VXUS', 300, 55.20, 'International', 'ETF', 0.90, 2.10, 
 (SELECT id FROM portfolio WHERE name = 'Balanced Retirement' AND client_id = (SELECT id FROM client WHERE email = 'sashank.pandem@email.com')));

-- Portfolio: Growth Focus (Good example)
INSERT INTO holding (ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
('VTI', 300, 245.80, 'Diversified', 'ETF', 0.95, 1.85, 
 (SELECT id FROM portfolio WHERE name = 'Growth Focus' AND client_id = (SELECT id FROM client WHERE email = 'sashank.pandem@email.com'))),
('VXUS', 200, 55.20, 'International', 'ETF', 0.90, 2.10, 
 (SELECT id FROM portfolio WHERE name = 'Growth Focus' AND client_id = (SELECT id FROM client WHERE email = 'sashank.pandem@email.com'))),
('QQQ', 150, 385.40, 'Technology', 'ETF', 1.25, 0.65, 
 (SELECT id FROM portfolio WHERE name = 'Growth Focus' AND client_id = (SELECT id FROM client WHERE email = 'sashank.pandem@email.com'))),
('VYM', 100, 110.30, 'Diversified', 'ETF', 0.85, 3.15, 
 (SELECT id FROM portfolio WHERE name = 'Growth Focus' AND client_id = (SELECT id FROM client WHERE email = 'sashank.pandem@email.com'))),
('BND', 400, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 
 (SELECT id FROM portfolio WHERE name = 'Growth Focus' AND client_id = (SELECT id FROM client WHERE email = 'sashank.pandem@email.com')));

-- Portfolio: Tech Growth (Problem example - concentration risk)
INSERT INTO holding (ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
('QQQ', 500, 385.40, 'Technology', 'ETF', 1.25, 0.65, 
 (SELECT id FROM portfolio WHERE name = 'Tech Growth' AND client_id = (SELECT id FROM client WHERE email = 'jack.siegel@email.com'))),
('AAPL', 200, 175.60, 'Technology', 'Stock', 1.35, 0.50, 
 (SELECT id FROM portfolio WHERE name = 'Tech Growth' AND client_id = (SELECT id FROM client WHERE email = 'jack.siegel@email.com'))),
('MSFT', 150, 380.20, 'Technology', 'Stock', 1.15, 0.80, 
 (SELECT id FROM portfolio WHERE name = 'Tech Growth' AND client_id = (SELECT id FROM client WHERE email = 'jack.siegel@email.com'))),
('NVDA', 100, 850.80, 'Technology', 'Stock', 1.85, 0.15, 
 (SELECT id FROM portfolio WHERE name = 'Tech Growth' AND client_id = (SELECT id FROM client WHERE email = 'jack.siegel@email.com'))),
('TSLA', 50, 240.40, 'Technology', 'Stock', 2.45, 0.00, 
 (SELECT id FROM portfolio WHERE name = 'Tech Growth' AND client_id = (SELECT id FROM client WHERE email = 'jack.siegel@email.com')));

-- Portfolio: Diversified Growth (Good example)
INSERT INTO holding (ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
('VTI', 400, 245.80, 'Diversified', 'ETF', 0.95, 1.85, 
 (SELECT id FROM portfolio WHERE name = 'Diversified Growth' AND client_id = (SELECT id FROM client WHERE email = 'jack.siegel@email.com'))),
('VXUS', 300, 55.20, 'International', 'ETF', 0.90, 2.10, 
 (SELECT id FROM portfolio WHERE name = 'Diversified Growth' AND client_id = (SELECT id FROM client WHERE email = 'jack.siegel@email.com'))),
('QQQ', 100, 385.40, 'Technology', 'ETF', 1.25, 0.65, 
 (SELECT id FROM portfolio WHERE name = 'Diversified Growth' AND client_id = (SELECT id FROM client WHERE email = 'jack.siegel@email.com'))),
('VNQ', 200, 85.60, 'Real Estate', 'ETF', 0.75, 4.20, 
 (SELECT id FROM portfolio WHERE name = 'Diversified Growth' AND client_id = (SELECT id FROM client WHERE email = 'jack.siegel@email.com'))),
('GLD', 50, 195.30, 'Commodities', 'ETF', 0.05, 0.00, 
 (SELECT id FROM portfolio WHERE name = 'Diversified Growth' AND client_id = (SELECT id FROM client WHERE email = 'jack.siegel@email.com')));

-- Portfolio: Conservative Income (Conservative example)
INSERT INTO holding (ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
('BND', 1200, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 
 (SELECT id FROM portfolio WHERE name = 'Conservative Income' AND client_id = (SELECT id FROM client WHERE email = 'sarah.chen@email.com'))),
('VYM', 300, 110.30, 'Diversified', 'ETF', 0.85, 3.15, 
 (SELECT id FROM portfolio WHERE name = 'Conservative Income' AND client_id = (SELECT id FROM client WHERE email = 'sarah.chen@email.com'))),
('VNQ', 150, 85.60, 'Real Estate', 'ETF', 0.75, 4.20, 
 (SELECT id FROM portfolio WHERE name = 'Conservative Income' AND client_id = (SELECT id FROM client WHERE email = 'sarah.chen@email.com'))),
('TIP', 200, 105.20, 'Fixed Income', 'ETF', 0.20, 2.80, 
 (SELECT id FROM portfolio WHERE name = 'Conservative Income' AND client_id = (SELECT id FROM client WHERE email = 'sarah.chen@email.com'))),
('SPY', 50, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 
 (SELECT id FROM portfolio WHERE name = 'Conservative Income' AND client_id = (SELECT id FROM client WHERE email = 'sarah.chen@email.com')));

-- Portfolio: Balanced Conservative (Good example)
INSERT INTO holding (ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
('BND', 800, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 
 (SELECT id FROM portfolio WHERE name = 'Balanced Conservative' AND client_id = (SELECT id FROM client WHERE email = 'sarah.chen@email.com'))),
('SPY', 150, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 
 (SELECT id FROM portfolio WHERE name = 'Balanced Conservative' AND client_id = (SELECT id FROM client WHERE email = 'sarah.chen@email.com'))),
('VYM', 200, 110.30, 'Diversified', 'ETF', 0.85, 3.15, 
 (SELECT id FROM portfolio WHERE name = 'Balanced Conservative' AND client_id = (SELECT id FROM client WHERE email = 'sarah.chen@email.com'))),
('VNQ', 100, 85.60, 'Real Estate', 'ETF', 0.75, 4.20, 
 (SELECT id FROM portfolio WHERE name = 'Balanced Conservative' AND client_id = (SELECT id FROM client WHERE email = 'sarah.chen@email.com'))),
('VXUS', 100, 55.20, 'International', 'ETF', 0.90, 2.10, 
 (SELECT id FROM portfolio WHERE name = 'Balanced Conservative' AND client_id = (SELECT id FROM client WHERE email = 'sarah.chen@email.com')));

-- Portfolio: Single Stock Risk (Problem example - extreme concentration)
INSERT INTO holding (ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
('AAPL', 1000, 175.60, 'Technology', 'Stock', 1.35, 0.50, 
 (SELECT id FROM portfolio WHERE name = 'Single Stock Risk' AND client_id = (SELECT id FROM client WHERE email = 'michael.rodriguez@email.com'))),
('SPY', 50, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 
 (SELECT id FROM portfolio WHERE name = 'Single Stock Risk' AND client_id = (SELECT id FROM client WHERE email = 'michael.rodriguez@email.com'))),
('BND', 100, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 
 (SELECT id FROM portfolio WHERE name = 'Single Stock Risk' AND client_id = (SELECT id FROM client WHERE email = 'michael.rodriguez@email.com')));

-- Portfolio: Sector Concentration (Problem example - tech overweight)
INSERT INTO holding (ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
('QQQ', 400, 385.40, 'Technology', 'ETF', 1.25, 0.65, 
 (SELECT id FROM portfolio WHERE name = 'Sector Concentration' AND client_id = (SELECT id FROM client WHERE email = 'michael.rodriguez@email.com'))),
('AAPL', 300, 175.60, 'Technology', 'Stock', 1.35, 0.50, 
 (SELECT id FROM portfolio WHERE name = 'Sector Concentration' AND client_id = (SELECT id FROM client WHERE email = 'michael.rodriguez@email.com'))),
('MSFT', 200, 380.20, 'Technology', 'Stock', 1.15, 0.80, 
 (SELECT id FROM portfolio WHERE name = 'Sector Concentration' AND client_id = (SELECT id FROM client WHERE email = 'michael.rodriguez@email.com'))),
('NVDA', 100, 850.80, 'Technology', 'Stock', 1.85, 0.15, 
 (SELECT id FROM portfolio WHERE name = 'Sector Concentration' AND client_id = (SELECT id FROM client WHERE email = 'michael.rodriguez@email.com'))),
('SPY', 100, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 
 (SELECT id FROM portfolio WHERE name = 'Sector Concentration' AND client_id = (SELECT id FROM client WHERE email = 'michael.rodriguez@email.com')));

-- Portfolio: Aggressive Growth (Aggressive example)
INSERT INTO holding (ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
('QQQ', 300, 385.40, 'Technology', 'ETF', 1.25, 0.65, 
 (SELECT id FROM portfolio WHERE name = 'Aggressive Growth' AND client_id = (SELECT id FROM client WHERE email = 'emily.watson@email.com'))),
('VTI', 200, 245.80, 'Diversified', 'ETF', 0.95, 1.85, 
 (SELECT id FROM portfolio WHERE name = 'Aggressive Growth' AND client_id = (SELECT id FROM client WHERE email = 'emily.watson@email.com'))),
('AAPL', 150, 175.60, 'Technology', 'Stock', 1.35, 0.50, 
 (SELECT id FROM portfolio WHERE name = 'Aggressive Growth' AND client_id = (SELECT id FROM client WHERE email = 'emily.watson@email.com'))),
('MSFT', 100, 380.20, 'Technology', 'Stock', 1.15, 0.80, 
 (SELECT id FROM portfolio WHERE name = 'Aggressive Growth' AND client_id = (SELECT id FROM client WHERE email = 'emily.watson@email.com'))),
('VXUS', 150, 55.20, 'International', 'ETF', 0.90, 2.10, 
 (SELECT id FROM portfolio WHERE name = 'Aggressive Growth' AND client_id = (SELECT id FROM client WHERE email = 'emily.watson@email.com')));

-- Portfolio: International Focus (Problem example - currency risk)
INSERT INTO holding (ticker, shares, price_per_share, sector, asset_type, beta, dividend_yield, portfolio_id) VALUES
('VXUS', 800, 55.20, 'International', 'ETF', 0.90, 2.10, 
 (SELECT id FROM portfolio WHERE name = 'International Focus' AND client_id = (SELECT id FROM client WHERE email = 'emily.watson@email.com'))),
('VWO', 400, 42.80, 'International', 'ETF', 1.15, 2.85, 
 (SELECT id FROM portfolio WHERE name = 'International Focus' AND client_id = (SELECT id FROM client WHERE email = 'emily.watson@email.com'))),
('EFA', 300, 75.60, 'International', 'ETF', 0.95, 2.45, 
 (SELECT id FROM portfolio WHERE name = 'International Focus' AND client_id = (SELECT id FROM client WHERE email = 'emily.watson@email.com'))),
('SPY', 100, 485.20, 'Diversified', 'ETF', 1.00, 1.45, 
 (SELECT id FROM portfolio WHERE name = 'International Focus' AND client_id = (SELECT id FROM client WHERE email = 'emily.watson@email.com'))),
('BND', 100, 75.40, 'Fixed Income', 'ETF', 0.15, 3.20, 
 (SELECT id FROM portfolio WHERE name = 'International Focus' AND client_id = (SELECT id FROM client WHERE email = 'emily.watson@email.com')));

-- Calculate and update portfolio total values from holdings
UPDATE portfolio SET total_value = (
    SELECT COALESCE(SUM(shares * price_per_share), 0)
    FROM holding 
    WHERE holding.portfolio_id = portfolio.id
); 