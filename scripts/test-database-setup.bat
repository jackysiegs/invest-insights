@echo off
echo 🧪 Testing Database Setup for Fresh GitHub Downloads...

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not running. Please start Docker and try again.
    exit /b 1
)

REM Use a different project name to avoid conflicts with main project
set COMPOSE_PROJECT_NAME=test_rj_invest

REM Start fresh database with different project name
echo 🗄️  Starting fresh PostgreSQL database (test environment)...
docker-compose down -v
docker-compose up -d

REM Wait for database to be ready
echo ⏳ Waiting for database to be ready...
timeout /t 15 /nobreak >nul

REM Check if database is ready
docker-compose exec -T postgres pg_isready -U iiAdmin -d invest_insights >nul 2>&1
if errorlevel 1 (
    echo ❌ Database failed to start properly. Check Docker logs:
    docker-compose logs postgres
    exit /b 1
)

echo ✅ Database is ready

REM Test database initialization
echo 🔍 Testing database initialization...

REM Check if admin user exists
for /f "tokens=*" %%i in ('docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM advisor WHERE username = 'ii-Admin';"') do set ADMIN_EXISTS=%%i
set ADMIN_EXISTS=%ADMIN_EXISTS: =%

if "%ADMIN_EXISTS%"=="1" (
    echo ✅ Admin user (ii-Admin) exists
) else (
    echo ❌ Admin user (ii-Admin) not found
    echo 📋 Current advisors in database:
    docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -c "SELECT id, name, username, email FROM advisor;"
    exit /b 1
)

REM Check if clients exist
for /f "tokens=*" %%i in ('docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM client;"') do set CLIENT_COUNT=%%i
set CLIENT_COUNT=%CLIENT_COUNT: =%

if %CLIENT_COUNT% geq 5 (
    echo ✅ Found %CLIENT_COUNT% clients
) else (
    echo ❌ Expected at least 5 clients, found %CLIENT_COUNT%
    echo 📋 Current clients in database:
    docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -c "SELECT id, name, email, advisor_id FROM client;"
    exit /b 1
)

REM Check if portfolios exist
for /f "tokens=*" %%i in ('docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM portfolio;"') do set PORTFOLIO_COUNT=%%i
set PORTFOLIO_COUNT=%PORTFOLIO_COUNT: =%

if %PORTFOLIO_COUNT% geq 10 (
    echo ✅ Found %PORTFOLIO_COUNT% portfolios
) else (
    echo ❌ Expected at least 10 portfolios, found %PORTFOLIO_COUNT%
    echo 📋 Current portfolios in database:
    docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -c "SELECT id, name, client_id FROM portfolio;"
    exit /b 1
)

REM Check if holdings exist
for /f "tokens=*" %%i in ('docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM holding;"') do set HOLDING_COUNT=%%i
set HOLDING_COUNT=%HOLDING_COUNT: =%

if %HOLDING_COUNT% geq 40 (
    echo ✅ Found %HOLDING_COUNT% holdings
) else (
    echo ❌ Expected at least 40 holdings, found %HOLDING_COUNT%
    echo 📋 Current holdings in database:
    docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -c "SELECT id, ticker, portfolio_id FROM holding LIMIT 10;"
    exit /b 1
)

REM Test foreign key relationships
echo 🔗 Testing foreign key relationships...

REM Check if all clients have valid advisor references
for /f "tokens=*" %%i in ('docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM client c LEFT JOIN advisor a ON c.advisor_id = a.id WHERE a.id IS NULL;"') do set INVALID_CLIENTS=%%i
set INVALID_CLIENTS=%INVALID_CLIENTS: =%

if "%INVALID_CLIENTS%"=="0" (
    echo ✅ All clients have valid advisor references
) else (
    echo ❌ Found %INVALID_CLIENTS% clients with invalid advisor references
    exit /b 1
)

REM Check if all portfolios have valid client references
for /f "tokens=*" %%i in ('docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM portfolio p LEFT JOIN client c ON p.client_id = c.id WHERE c.id IS NULL;"') do set INVALID_PORTFOLIOS=%%i
set INVALID_PORTFOLIOS=%INVALID_PORTFOLIOS: =%

if "%INVALID_PORTFOLIOS%"=="0" (
    echo ✅ All portfolios have valid client references
) else (
    echo ❌ Found %INVALID_PORTFOLIOS% portfolios with invalid client references
    exit /b 1
)

REM Check if all holdings have valid portfolio references
for /f "tokens=*" %%i in ('docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM holding h LEFT JOIN portfolio p ON h.portfolio_id = p.id WHERE p.id IS NULL;"') do set INVALID_HOLDINGS=%%i
set INVALID_HOLDINGS=%INVALID_HOLDINGS: =%

if "%INVALID_HOLDINGS%"=="0" (
    echo ✅ All holdings have valid portfolio references
) else (
    echo ❌ Found %INVALID_HOLDINGS% holdings with invalid portfolio references
    exit /b 1
)

echo.
echo 🎉 Database setup test PASSED!
echo ✅ All data properly initialized
echo ✅ All foreign key relationships valid
echo ✅ Ready for fresh GitHub downloads
echo.

REM Show summary
echo 📊 Database Summary:
echo    - Admin users: %ADMIN_EXISTS%
echo    - Clients: %CLIENT_COUNT%
echo    - Portfolios: %PORTFOLIO_COUNT%
echo    - Holdings: %HOLDING_COUNT%
echo.

echo 🧹 Cleaning up test database...
docker-compose down -v

REM Reset project name
set COMPOSE_PROJECT_NAME=

echo ✅ Test completed successfully!
echo 🛡️  Your main project data is completely safe! 