-- Database Schema for RJ Invest Insights
-- This script creates all necessary tables for the application
-- UPDATED: Matches the actual entity model structure

-- Create advisor table
CREATE TABLE IF NOT EXISTS advisor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- Create client table
CREATE TABLE IF NOT EXISTS client (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    risk_tolerance VARCHAR(50) NOT NULL,
    investment_goal VARCHAR(255) NOT NULL,
    time_horizon INTEGER NOT NULL,
    advisor_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (advisor_id) REFERENCES advisor(id)
);

-- Create portfolio table
CREATE TABLE IF NOT EXISTS portfolio (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    client_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    account_type VARCHAR(255),
    total_value NUMERIC(38,2),
    FOREIGN KEY (client_id) REFERENCES client(id)
);

-- Create holding table
CREATE TABLE IF NOT EXISTS holding (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    shares DECIMAL(10,2) NOT NULL,
    price_per_share DECIMAL(10,2) NOT NULL,
    sector VARCHAR(100),
    asset_type VARCHAR(50),
    beta DECIMAL(5,2),
    dividend_yield DECIMAL(5,2),
    portfolio_id BIGINT NOT NULL,
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);

-- Create investment_insight table (matches entity model)
CREATE TABLE IF NOT EXISTS investment_insight (
    id BIGSERIAL PRIMARY KEY,
    summary VARCHAR(500),
    ai_generated_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    client_id BIGINT,
    portfolio_name VARCHAR(255),
    risk_score INTEGER,
    diversification_score INTEGER,
    goal_alignment INTEGER,
    structured_data TEXT,
    debug_info TEXT,
    FOREIGN KEY (client_id) REFERENCES client(id)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_client_advisor_id ON client(advisor_id);
CREATE INDEX IF NOT EXISTS idx_portfolio_client_id ON portfolio(client_id);
CREATE INDEX IF NOT EXISTS idx_holding_portfolio_id ON holding(portfolio_id);
CREATE INDEX IF NOT EXISTS idx_insight_client_id ON investment_insight(client_id);
CREATE INDEX IF NOT EXISTS idx_advisor_username ON advisor(username);
CREATE INDEX IF NOT EXISTS idx_client_email ON client(email); 