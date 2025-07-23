# RJ Invest Insights

A comprehensive financial portfolio analysis application that combines mathematical scoring with AI-generated qualitative insights. Built with Spring Boot, Angular, and OpenAI GPT-4.

## Features

- **Portfolio Analysis**: Mathematical scoring for risk, diversification, and goal alignment
- **AI-Powered Insights**: Qualitative analysis using OpenAI GPT-4 with hybrid scoring approach
- **Real-time Market Data**: Integration with Finnhub API for market news and company-specific updates
- **Modern UI**: Template design for now but intended to be a Glassmorphism design with responsive layout
- **Multi-Client Management**: Advisor dashboard for managing multiple client portfolios
- **Comprehensive Reporting**: Detailed analysis with actionable recommendations

## Architecture

- **Backend**: Java Spring Boot with PostgreSQL
- **Frontend**: Angular 17 with modern UI components
- **AI Service**: FastAPI microservice with OpenAI GPT-4 integration
- **Database**: PostgreSQL with Docker Compose setup
- **APIs**: Finnhub for market data, OpenAI for AI insights

## Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- Docker and Docker Compose
- OpenAI API key
- Finnhub API key

## Quick Setup

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/rj-invest-insights.git
cd rj-invest-insights
```

### 2. Set Up Environment Variables
Create a `.env` file in the `ai_microservice/` directory:
```env
OPENAI_API_KEY=your_openai_api_key_here
FINNHUB_API_KEY=your_finnhub_api_key_here
```

**Note**: The `.env` file is ignored by Git for security. You need to create your own with your actual API keys.

### 3. Start the Database
```bash
docker-compose up -d
```

### 4. Start the Backend
```bash
cd backend
./mvnw spring-boot:run
```

### 5. Start the AI Microservice
```bash
cd ai_microservice
pip install -r requirements.txt
python main.py
```

### 6. Start the Frontend
```bash
cd frontend
npm install
ng serve
```

### 7. Access the Application
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080
- AI Microservice: http://localhost:8000

## Demo Login

The application comes pre-loaded with demo data including:

**Advisor Login:**
- Username: `ii-Admin`
- Password: `RayJay1!`

## Demo Clients & Portfolios

The system includes comprehensive demo data showcasing various portfolio scenarios:

### **Sashank Pandem** (Moderate Risk - Retirement)
- **Balanced Retirement**: Well-diversified portfolio with ETFs and bonds
- **Growth Focus**: Growth-oriented with moderate risk exposure

### **Jack Siegel** (Aggressive Risk - Wealth Building)
- **Tech Growth**: Technology-heavy portfolio (demonstrates concentration risk)
- **Diversified Growth**: Well-balanced growth portfolio

### **Sarah Chen** (Conservative Risk - Income Generation)
- **Conservative Income**: Bond-heavy portfolio for income
- **Balanced Conservative**: Conservative approach with some growth

### **Michael Rodriguez** (Moderate Risk - College Savings)
- **Single Stock Risk**: 85% concentration in AAPL (extreme risk example)
- **Sector Concentration**: Overweight in technology sector

### **Emily Watson** (Aggressive Risk - Early Retirement)
- **Aggressive Growth**: High-growth portfolio with tech exposure
- **International Focus**: Heavy international exposure (currency risk example)

## 📊 Portfolio Analysis Examples

### Good Portfolio Examples
- **Balanced Retirement**: Low risk (4/10), high diversification (85%), good goal alignment (82%)
- **Diversified Growth**: Moderate risk (6/10), excellent diversification (88%), strong goal alignment (85%)

### Problem Portfolio Examples
- **Tech Growth**: High risk (8/10), low diversification (45%), poor goal alignment (62%)
- **Single Stock Risk**: Extreme risk (9/10), very low diversification (25%), poor goal alignment (45%)
- **International Focus**: Moderate risk (6/10), moderate diversification (65%), currency risk concerns

## Configuration

### Database Configuration
The application uses PostgreSQL with the following default settings:
- Database: `invest_insights`
- Username: `iiAdmin`
- Password: `RayJay1!`
- Port: `5432`

### API Configuration
- **OpenAI**: Required for AI insights generation
- **Finnhub**: Required for market news and company data

### Environment Variables
The application uses environment variables for API keys. Create a `.env` file in the `ai_microservice/` directory:

```env
OPENAI_API_KEY=your_openai_api_key_here
FINNHUB_API_KEY=your_finnhub_api_key_here
```

**Security Note**: The `.env` file is automatically ignored by Git to prevent accidental exposure of API keys. Each user must create their own `.env` file with their actual API keys.

## Testing the System

1. **Login as ii-Admin**
2. **Select different clients** to see various portfolio scenarios
3. **Generate AI Insights** for each portfolio to see different analysis types
4. **Compare portfolios** to understand risk and diversification differences
5. **View market news** integration in AI insights

## Project Structure

```
rj-invest-insights/
├── backend/                 # Spring Boot application
├── frontend/               # Angular application
├── ai_microservice/        # FastAPI AI service
├── database/               # Database setup and initialization
├── docs/                   # Project documentation
├── scripts/                # Setup and utility scripts
├── tests/                  # Test files and validation scripts
├── data/                   # Sample data and API examples
├── docker-compose.yml      # Database setup
└── README.md              # This file
```

### Folder Details

- **`backend/`**: Spring Boot REST API with portfolio analysis logic
- **`frontend/`**: Angular 17 application with modern UI
- **`ai_microservice/`**: FastAPI service for AI insights generation
- **`database/`**: SQL scripts for database initialization and cleanup
- **`docs/`**: Technical documentation, improvement notes, and system architecture
- **`scripts/`**: Setup scripts, demo automation, and utility tools
- **`tests/`**: Python test scripts for AI improvements and validation
- **`data/`**: Sample data files and API testing examples

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request
