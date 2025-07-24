# Invest Insights
<img width="1675" height="1264" alt="Screenshot 2025-07-24 141547" src="https://github.com/user-attachments/assets/4f32a1f3-e57c-471e-b4b3-9f9f97a4872d" />
<img width="1669" height="1267" alt="Screenshot 2025-07-24 141602" src="https://github.com/user-attachments/assets/1cedad51-4221-4462-8282-c26910e728c5" />
<img width="1668" height="1267" alt="Screenshot 2025-07-24 141615" src="https://github.com/user-attachments/assets/b88c3950-fd4c-4aa9-ad7e-572d391d4106" />

A comprehensive financial portfolio analysis application that combines mathematical scoring with AI-generated qualitative insights. Built with Spring Boot, Angular, and OpenAI GPT-4.

## Features

- **Portfolio Analysis**: Mathematical scoring for risk, diversification, and goal alignment
- **AI-Powered Insights**: Qualitative analysis using OpenAI GPT-4 with hybrid scoring approach
- **Real-time Market Data**: Integration with Finnhub API for market news and company-specific updates
- **Modern UI**: Template design for now but intended to be a Glassmorphism design with responsive layout
- **Multi-Client Management**: Advisor dashboard for managing multiple client portfolios
- **Comprehensive Reporting**: Detailed analysis with actionable recommendations
- **Automatic Setup**: Fresh installations work immediately with demo data and admin user

## Architecture

- **Backend**: Java Spring Boot with PostgreSQL
- **Frontend**: Angular 17 with modern UI components
- **AI Service**: FastAPI microservice with OpenAI GPT-4 integration
- **Database**: PostgreSQL with Docker Compose setup and automatic initialization
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
git clone https://github.com/jackysiegs/invest-insights.git
cd invest-insights
```

### 2. Set Up Environment Variables
Create a `.env` file in the `ai_microservice/` directory:
```env
OPENAI_API_KEY=your_openai_api_key_here
FINNHUB_API_KEY=your_finnhub_api_key_here
```

**Note**: The `.env` file is ignored by Git for security. You need to create your own with your actual API keys.

### 3. Start the Database (Automatic Initialization)
```bash
docker-compose up -d
```

**Important**: The database will automatically:
- Create all necessary tables
- Initialize with demo data including the admin user
- Be ready for immediate use

### 4. Start the Backend
Linux/macOS
```bash
cd backend
./mvnw spring-boot:run
```
Windows
```bash
cd backend
mvnw.cmd spring-boot:run
```

### 5. Start the AI Microservice
```bash
cd ai_microservice
pip install -r requirements.txt
uvicorn main:app --reload
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

## Automated Setup

For the easiest setup experience, use the automated script (may not be fully functional right now recommend manual setup):

```bash
# Make script executable (Linux/Mac)
chmod +x scripts/start-demo.sh

# Run automated setup
./scripts/start-demo.sh
```

This script will:
- ✅ Check Docker is running
- ✅ Validate environment variables
- ✅ Start database with automatic initialization
- ✅ Start all services (backend, AI, frontend)
- ✅ Verify everything is working
- ✅ Provide login credentials

## Troubleshooting

### Database Connection Issues
If you get database connection errors:
1. Ensure Docker is running
2. Run `docker-compose down -v` to reset the database
3. Run `docker-compose up -d` to restart with fresh data

### Maven Wrapper Issues
If `./mvnw` doesn't work:
1. Ensure you're in the `backend` directory
2. Try `mvnw.cmd` on Windows
3. The Maven wrapper files are included in the repository

### Frontend Issues
If the frontend folder appears empty:
1. The frontend is now included as a regular folder (not a submodule)
2. Run `npm install` in the frontend directory
3. Ensure Node.js 18+ is installed

### Login Issues
If login doesn't work:
1. Ensure the database has been properly initialized
2. Check that the admin user exists: `docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -c "SELECT username FROM advisor;"`
3. Verify the database container is running: `docker ps`

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

## Portfolio Analysis Examples

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

**Automatic Initialization**: The database automatically creates tables and demo data on first startup.

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
│   ├── schema.sql         # Database schema creation
│   └── init-demo-data.sql # Demo data initialization
├── docs/                   # Project documentation
├── scripts/                # Setup and utility scripts
├── tests/                  # Test files and validation scripts
├── data/                   # Sample data and API examples
├── docker-compose.yml      # Database setup with automatic initialization
└── README.md              # This file
```

### Folder Details

- **`backend/`**: Spring Boot REST API with portfolio analysis logic
- **`frontend/`**: Angular 17 application with modern UI
- **`ai_microservice/`**: FastAPI service for AI insights generation
- **`database/`**: SQL scripts for database initialization and cleanup
  - **`schema.sql`**: Creates all database tables automatically
  - **`init-demo-data.sql`**: Inserts demo data including admin user
- **`docs/`**: Technical documentation, improvement notes, and system architecture
- **`scripts/`**: Setup scripts, demo automation, and utility tools
- **`tests/`**: Python test scripts for AI improvements and validation
- **`data/`**: Sample data files and API testing examples

## Database Initialization

The system uses an automated database initialization process:

### Automatic Setup
1. **Docker starts PostgreSQL container**
2. **Docker runs initialization scripts**:
   - `01-schema.sql` → Creates all tables with proper relationships
   - `02-init-demo-data.sql` → Inserts admin user and demo data
3. **Database is ready immediately** with tables and data
4. **Spring Boot connects** to existing database seamlessly

### Manual Verification
To verify the database initialization worked:
```bash
# Check if admin user exists
docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -c "SELECT username FROM advisor;"

# Check demo data
docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -c "SELECT COUNT(*) FROM client;"
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Recent Updates

### Database Initialization Fix (Latest)
- **Problem**: Fresh GitHub downloads failed login due to database initialization issues
- **Solution**: Complete rewrite of database initialization system
- **Result**: Fresh installations work immediately with working login
- **Status**: Production ready for GitHub deployment

**Key Improvements**:
- Automatic table creation on container startup
- Proper foreign key relationships
- Admin user created automatically
- No manual database setup required
- JPA compatibility maintained
