# 🚀 BookVault Microservices - Quick Start

## ⚡ Cách Chạy Nhanh (3 Phương Pháp)

### 🎯 Cách 1: Dùng Script (NHANH NHẤT)

**Windows:**
```bash
build-and-start.bat
```

**Linux/Mac:**
```bash
chmod +x build-and-start.sh
./build-and-start.sh
```

### 🎯 Cách 2: Docker Compose (Đầy đủ services)

```bash
# Build và start tất cả services
docker compose build
docker compose up -d

# Xem logs
docker compose logs -f

# Stop
docker compose down
```

### 🎯 Cách 3: Chỉ Core Services (NHANH - bỏ Elasticsearch, MinIO, Kafka)

```bash
# Chạy chỉ core services (catalog, iam, borrowing, gateway, admin)
docker compose -f docker-compose.minimal.yml build
docker compose -f docker-compose.minimal.yml up -d
```

## 📋 Prerequisites

- Docker & Docker Compose
- JDK 21 (nếu build local)
- Maven 3.9+ (nếu build local)

## ✅ Kiểm Tra Services Đã Chạy

```bash
# Xem status
docker compose ps

# Health checks
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # Catalog
curl http://localhost:8082/actuator/health  # IAM
curl http://localhost:8083/actuator/health  # Borrowing
```

## 🧪 Test API

**Register user**:
```bash
curl -X POST http://localhost:8080/api/iam/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Login**:
```bash
curl -X POST http://localhost:8080/api/iam/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**Get public key** (để config Gateway):
```bash
curl http://localhost:8082/api/iam/v1/auth/public-key
```

**Create book** (với JWT token):
```bash
curl -X POST http://localhost:8080/api/catalog/v1/books \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "title": "Spring Boot Guide",
    "author": "John Doe",
    "isbn": "978-1234567890",
    "quantity": 10
  }'
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| Gateway | 8080 | API Gateway, JWT validation |
| Catalog | 8081 | Book management |
| IAM | 8082 | Authentication & Authorization |

## Database

- **Catalog DB**: `catalog-postgres:5432` (localhost:5433)
- **IAM DB**: `iam-postgres:5432` (localhost:5434)

## Message Broker

- **Kafka**: `localhost:29092`
- **Zookeeper**: `localhost:2181`

## Documentation

- **Swagger UI**: 
  - Gateway: http://localhost:8080/swagger-ui
  - Catalog: http://localhost:8081/swagger-ui
  - IAM: http://localhost:8082/swagger-ui

- **Chi tiết**: Xem `REFACTOR_REPORT.md`

## Troubleshooting

### Services không start
```bash
# Check logs
docker compose logs catalog
docker compose logs iam
docker compose logs gateway

# Check database health
docker compose ps
```

### Database connection issues
```bash
# Verify postgres is running
docker compose ps | grep postgres

# Check database
docker compose exec catalog-postgres psql -U catalog -d catalog
```

### JWT validation không hoạt động
1. Lấy public key từ IAM: `curl http://localhost:8082/api/iam/v1/auth/public-key`
2. Set env `JWT_PUBLIC_KEY_PEM_CONTENT` trong docker-compose.yml
3. Restart gateway: `docker compose restart gateway`

## Build Locally

```bash
# Build Catalog
cd services/catalog
mvn clean package
docker build -t catalog-service:latest .

# Build IAM
cd ../iam
mvn clean package
docker build -t iam-service:latest .

# Build Gateway
cd ../gateway
mvn clean package
docker build -t gateway-service:latest .
```

## Deploy to Kubernetes

```bash
# Install Helm charts
helm install catalog ./platform/helm/catalog -n bookvault --create-namespace
helm install iam ./platform/helm/iam -n bookvault
helm install gateway ./platform/helm/gateway -n bookvault

# Check status
kubectl get pods -n bookvault
kubectl get services -n bookvault
```

## Next Steps

1. Implement Borrowing Service
2. Add Elasticsearch for Search Service
3. Implement Notification Service
4. Add Media Service (S3/MinIO)
5. Create Admin Dashboard Service

Xem `REFACTOR_REPORT.md` để biết chi tiết đầy đủ.

