#!/bin/bash

echo "🚀 Starting RJ Invest Insights Demo Setup..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if .env file exists
if [ ! -f ai_microservice/.env ]; then
    echo "⚠️  .env file not found. Creating template..."
    cat > ai_microservice/.env << EOF
# API Keys (Required)
OPENAI_API_KEY=your_openai_api_key_here
FINNHUB_API_KEY=your_finnhub_api_key_here

# Optional: Override default settings
# DATABASE_PASSWORD=your_custom_password
# DATABASE_PORT=5432
EOF
    echo "📝 Please edit ai_microservice/.env file with your API keys before continuing."
    echo "   You can get free API keys from:"
    echo "   - OpenAI: https://platform.openai.com/api-keys"
    echo "   - Finnhub: https://finnhub.io/register"
    exit 1
fi

# Check if API keys are set
if grep -q "your_openai_api_key_here" ai_microservice/.env || grep -q "your_finnhub_api_key_here" ai_microservice/.env; then
    echo "❌ Please update ai_microservice/.env file with your actual API keys."
    exit 1
fi

echo "✅ Environment variables configured"

# Start database
echo "🗄️  Starting PostgreSQL database..."
docker-compose up -d

# Wait for database to be ready
echo "⏳ Waiting for database to be ready..."
sleep 10

# Check if database is ready
if ! docker-compose exec -T postgres pg_isready -U iiAdmin -d invest_insights > /dev/null 2>&1; then
    echo "❌ Database failed to start properly. Check Docker logs:"
    docker-compose logs postgres
    exit 1
fi

echo "✅ Database is ready"

# Start backend
echo "🔧 Starting Spring Boot backend..."
cd backend
./mvnw spring-boot:run > ../backend.log 2>&1 &
BACKEND_PID=$!
cd ..

# Wait for backend to start
echo "⏳ Waiting for backend to start..."
sleep 15

# Check if backend is ready
if ! curl -s http://localhost:8080/api/advisors/health > /dev/null 2>&1; then
    echo "❌ Backend failed to start. Check backend.log for details."
    exit 1
fi

echo "✅ Backend is ready"

# Start AI microservice
echo "🤖 Starting AI microservice..."
cd ai_microservice
python main.py > ../ai-service.log 2>&1 &
AI_PID=$!
cd ..

# Wait for AI service to start
echo "⏳ Waiting for AI service to start..."
sleep 10

# Check if AI service is ready
if ! curl -s http://localhost:8000/health > /dev/null 2>&1; then
    echo "❌ AI service failed to start. Check ai-service.log for details."
    exit 1
fi

echo "✅ AI service is ready"

# Start frontend
echo "🎨 Starting Angular frontend..."
cd frontend
npm install > ../frontend-install.log 2>&1
ng serve > ../frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

# Wait for frontend to start
echo "⏳ Waiting for frontend to start..."
sleep 20

echo ""
echo "🎉 RJ Invest Insights Demo is now running!"
echo ""
echo "📱 Access the application:"
echo "   Frontend: http://localhost:4200"
echo "   Backend API: http://localhost:8080"
echo "   AI Service: http://localhost:8000"
echo ""
echo "🔐 Demo Login:"
echo "   Username: ii-Admin"
echo "   Password: RayJay1!"
echo ""
echo "👥 Demo Clients:"
echo "   - Sashank Pandem (Moderate Risk)"
echo "   - Jack Siegel (Aggressive Risk)"
echo "   - Sarah Chen (Conservative Risk)"
echo "   - Michael Rodriguez (Problem Portfolios)"
echo "   - Emily Watson (Aggressive Growth)"
echo ""
echo "🛑 To stop all services, run: ./scripts/stop-demo.sh"
echo "📋 To view logs, check: backend.log, ai-service.log, frontend.log"
echo ""

# Save PIDs for easy cleanup
echo $BACKEND_PID > .backend.pid
echo $AI_PID > .ai-service.pid
echo $FRONTEND_PID > .frontend.pid

echo "Setup complete! 🚀" 