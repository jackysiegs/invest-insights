#!/bin/bash

echo "🛑 Stopping RJ Invest Insights Demo..."

# Stop frontend
if [ -f .frontend.pid ]; then
    FRONTEND_PID=$(cat .frontend.pid)
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        echo "🎨 Stopping Angular frontend..."
        kill $FRONTEND_PID
        rm .frontend.pid
    fi
fi

# Stop AI service
if [ -f .ai-service.pid ]; then
    AI_PID=$(cat .ai-service.pid)
    if kill -0 $AI_PID 2>/dev/null; then
        echo "🤖 Stopping AI microservice..."
        kill $AI_PID
        rm .ai-service.pid
    fi
fi

# Stop backend
if [ -f .backend.pid ]; then
    BACKEND_PID=$(cat .backend.pid)
    if kill -0 $BACKEND_PID 2>/dev/null; then
        echo "🔧 Stopping Spring Boot backend..."
        kill $BACKEND_PID
        rm .backend.pid
    fi
fi

# Stop database
echo "🗄️  Stopping PostgreSQL database..."
docker-compose down

echo "✅ All services stopped!"
echo ""
echo "💡 To start again, run: ./scripts/start-demo.sh" 