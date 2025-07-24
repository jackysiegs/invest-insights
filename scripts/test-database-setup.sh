#!/bin/bash

echo "🧪 Testing Database Setup for Fresh GitHub Downloads..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Use a different project name to avoid conflicts with main project
export COMPOSE_PROJECT_NAME=test_rj_invest

# Start fresh database with different project name
echo "🗄️  Starting fresh PostgreSQL database (test environment)..."
docker-compose down -v
docker-compose up -d

# Wait for database to be ready
echo "⏳ Waiting for database to be ready..."
sleep 15

# Check if database is ready
if ! docker-compose exec -T postgres pg_isready -U iiAdmin -d invest_insights > /dev/null 2>&1; then
    echo "❌ Database failed to start properly. Check Docker logs:"
    docker-compose logs postgres
    exit 1
fi

echo "✅ Database is ready"

# Test database initialization
echo "🔍 Testing database initialization..."

# Check if admin user exists
ADMIN_EXISTS=$(docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM advisor WHERE username = 'ii-Admin';" | tr -d ' \n')

if [ "$ADMIN_EXISTS" = "1" ]; then
    echo "✅ Admin user (ii-Admin) exists"
else
    echo "❌ Admin user (ii-Admin) not found"
    echo "📋 Current advisors in database:"
    docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -c "SELECT id, name, username, email FROM advisor;"
    exit 1
fi

# Check if clients exist
CLIENT_COUNT=$(docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM client;" | tr -d ' \n')

if [ "$CLIENT_COUNT" -ge 5 ]; then
    echo "✅ Found $CLIENT_COUNT clients"
else
    echo "❌ Expected at least 5 clients, found $CLIENT_COUNT"
    echo "📋 Current clients in database:"
    docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -c "SELECT id, name, email, advisor_id FROM client;"
    exit 1
fi

# Check if portfolios exist
PORTFOLIO_COUNT=$(docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM portfolio;" | tr -d ' \n')

if [ "$PORTFOLIO_COUNT" -ge 10 ]; then
    echo "✅ Found $PORTFOLIO_COUNT portfolios"
else
    echo "❌ Expected at least 10 portfolios, found $PORTFOLIO_COUNT"
    echo "📋 Current portfolios in database:"
    docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -c "SELECT id, name, client_id FROM portfolio;"
    exit 1
fi

# Check if holdings exist
HOLDING_COUNT=$(docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM holding;" | tr -d ' \n')

if [ "$HOLDING_COUNT" -ge 40 ]; then
    echo "✅ Found $HOLDING_COUNT holdings"
else
    echo "❌ Expected at least 40 holdings, found $HOLDING_COUNT"
    echo "📋 Current holdings in database:"
    docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -c "SELECT id, ticker, portfolio_id FROM holding LIMIT 10;"
    exit 1
fi

# Test foreign key relationships
echo "🔗 Testing foreign key relationships..."

# Check if all clients have valid advisor references
INVALID_CLIENTS=$(docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM client c LEFT JOIN advisor a ON c.advisor_id = a.id WHERE a.id IS NULL;" | tr -d ' \n')

if [ "$INVALID_CLIENTS" = "0" ]; then
    echo "✅ All clients have valid advisor references"
else
    echo "❌ Found $INVALID_CLIENTS clients with invalid advisor references"
    exit 1
fi

# Check if all portfolios have valid client references
INVALID_PORTFOLIOS=$(docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM portfolio p LEFT JOIN client c ON p.client_id = c.id WHERE c.id IS NULL;" | tr -d ' \n')

if [ "$INVALID_PORTFOLIOS" = "0" ]; then
    echo "✅ All portfolios have valid client references"
else
    echo "❌ Found $INVALID_PORTFOLIOS portfolios with invalid client references"
    exit 1
fi

# Check if all holdings have valid portfolio references
INVALID_HOLDINGS=$(docker-compose exec -T postgres psql -U iiAdmin -d invest_insights -t -c "SELECT COUNT(*) FROM holding h LEFT JOIN portfolio p ON h.portfolio_id = p.id WHERE p.id IS NULL;" | tr -d ' \n')

if [ "$INVALID_HOLDINGS" = "0" ]; then
    echo "✅ All holdings have valid portfolio references"
else
    echo "❌ Found $INVALID_HOLDINGS holdings with invalid portfolio references"
    exit 1
fi

echo ""
echo "🎉 Database setup test PASSED!"
echo "✅ All data properly initialized"
echo "✅ All foreign key relationships valid"
echo "✅ Ready for fresh GitHub downloads"
echo ""

# Show summary
echo "📊 Database Summary:"
echo "   - Admin users: $ADMIN_EXISTS"
echo "   - Clients: $CLIENT_COUNT"
echo "   - Portfolios: $PORTFOLIO_COUNT"
echo "   - Holdings: $HOLDING_COUNT"
echo ""

echo "🧹 Cleaning up test database..."
docker-compose down -v

# Reset project name
unset COMPOSE_PROJECT_NAME

echo "✅ Test completed successfully!"
echo "🛡️  Your main project data is completely safe!" 