#!/bin/bash

echo "🚀 Starting BookVault Microservices..."
echo ""

# Check if Docker Desktop is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop first."
    echo "   Opening Docker Desktop..."
    open -a Docker
    echo "   Waiting for Docker to start (30 seconds)..."
    sleep 30
    
    # Check again
    if ! docker info > /dev/null 2>&1; then
        echo "❌ Docker still not running. Please start Docker Desktop manually."
        exit 1
    fi
fi

echo "✅ Docker is running"
echo ""

# Check if docker-compose.yml exists
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ docker-compose.yml not found. Please run this script from project root."
    exit 1
fi

# Build and start services
echo "📦 Building and starting services..."
echo "   This may take 5-10 minutes on first run..."
echo ""

docker compose up -d --build

if [ $? -ne 0 ]; then
    echo "❌ Failed to start services. Check logs with: docker compose logs"
    exit 1
fi

echo ""
echo "⏳ Waiting for services to be ready (30 seconds)..."
sleep 30

echo ""
echo "🏥 Checking service health..."
echo ""

# Health checks
check_health() {
    local service=$1
    local url=$2
    local response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
    
    if [ "$response" = "200" ]; then
        echo "   ✅ $service is healthy"
        return 0
    else
        echo "   ⚠️  $service is not ready yet (HTTP $response)"
        return 1
    fi
}

check_health "Gateway" "http://localhost:8080/actuator/health"
check_health "Catalog" "http://localhost:8081/actuator/health"
check_health "IAM" "http://localhost:8082/actuator/health"

echo ""
echo "📊 Service Status:"
docker compose ps

echo ""
echo "✅ Services started!"
echo ""
echo "🌐 Access points:"
echo "   Gateway:     http://localhost:8080"
echo "   Catalog:     http://localhost:8081"
echo "   IAM:         http://localhost:8082"
echo "   Borrowing:   http://localhost:8083"
echo "   Search:      http://localhost:8084"
echo "   Notification: http://localhost:8085"
echo "   Media:       http://localhost:8086"
echo "   Admin:       http://localhost:8087"
echo ""
echo "📚 Swagger UI:"
echo "   Catalog:     http://localhost:8081/swagger-ui"
echo "   IAM:         http://localhost:8082/swagger-ui"
echo ""
echo "📝 Next steps:"
echo "   1. Register a user:"
echo "      curl -X POST http://localhost:8080/api/iam/v1/auth/register \\"
echo "        -H 'Content-Type: application/json' \\"
echo "        -d '{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password123\"}'"
echo ""
echo "   2. View logs:"
echo "      docker compose logs -f gateway"
echo ""
echo "   3. Stop services:"
echo "      docker compose stop"
echo ""

