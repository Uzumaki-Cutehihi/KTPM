# 🔧 Troubleshooting Guide

## Lỗi "unexpected end of JSON input"

Lỗi này thường xảy ra khi:
1. **Network issues**: Kết nối internet không ổn định
2. **Docker registry issues**: Registry bị lỗi hoặc quá tải
3. **Image tags không tồn tại**: Tag đã bị xóa hoặc không còn tồn tại

### Giải pháp:

#### 1. Pull images trước khi start:

```bash
# Pull từng image một để kiểm tra
docker pull postgres:16
docker pull bitnami/zookeeper:3.9
docker pull bitnami/kafka:3.6
docker pull docker.elastic.co/elasticsearch/elasticsearch:8.11.0
docker pull minio/minio:RELEASE.2023-12-09T18-10-45Z

# Sau đó mới start
docker compose up -d
```

#### 2. Nếu vẫn lỗi với MinIO, thử alternatives:

**Option A: Dùng MinIO tag khác**
```yaml
image: minio/minio:latest
```

**Option B: Comment MinIO và Media service tạm thời**
- Comment `minio:` và `media:` trong docker-compose.yml
- Các service khác vẫn chạy được

#### 3. Nếu lỗi với Kafka/Zookeeper:

**Option A: Dùng Apache Kafka official**
```yaml
kafka:
  image: apache/kafka:latest
  # ... config khác
```

**Option B: Dùng Confluent**
```yaml
kafka:
  image: confluentinc/cp-kafka:latest
  # ... config khác
```

#### 4. Nếu lỗi với Elasticsearch:

**Option A: Dùng version khác**
```yaml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.10.0
```

**Option B: Dùng OpenSearch**
```yaml
elasticsearch:
  image: opensearchproject/opensearch:latest
```

## Lỗi Build Images

### 1. Build từng service một:

```bash
# Build catalog
docker compose build catalog

# Build iam
docker compose build iam

# ... tương tự
```

### 2. Nếu build fail, check logs:

```bash
docker compose build --progress=plain catalog 2>&1 | tee build.log
```

### 3. Build với no-cache:

```bash
docker compose build --no-cache catalog
```

## Lỗi Network

### 1. Tạo network trước:

```bash
docker network create bookvault
```

### 2. Kiểm tra network:

```bash
docker network ls
docker network inspect bookvault
```

## Lỗi Port Conflicts

### 1. Kiểm tra ports đang dùng:

```bash
# Windows
netstat -ano | findstr :8080

# Linux/Mac
lsof -i :8080
```

### 2. Đổi ports trong docker-compose.yml:

```yaml
ports:
  - "8080:8080"  # Đổi số bên trái
```

## Lỗi Database Connection

### 1. Kiểm tra database đã start:

```bash
docker compose ps | grep postgres
```

### 2. Check database logs:

```bash
docker compose logs catalog-postgres
```

### 3. Connect vào database:

```bash
docker compose exec catalog-postgres psql -U catalog -d catalog
```

## Lỗi Memory/Resource

### 1. Giảm memory cho Elasticsearch:

```yaml
environment:
  - "ES_JAVA_OPTS=-Xms256m -Xmx256m"
```

### 2. Tắt services không cần thiết:

Comment các services không dùng đến trong docker-compose.yml

## Quick Fix - Start từng service

Nếu gặp nhiều lỗi, start từng nhóm:

### Nhóm 1: Databases
```bash
docker compose up -d catalog-postgres iam-postgres borrowing-postgres
```

### Nhóm 2: Core Services
```bash
docker compose up -d catalog iam gateway
```

### Nhóm 3: Supporting Services
```bash
docker compose up -d borrowing search admin
```

### Nhóm 4: Optional Services (có thể skip)
```bash
docker compose up -d elasticsearch minio media notification kafka zookeeper
```

## Reset Everything

Nếu muốn reset hoàn toàn:

```bash
# Stop và xóa tất cả
docker compose down -v

# Xóa images
docker rmi $(docker images -q bookvault-*)

# Xóa volumes
docker volume prune -f

# Build lại từ đầu
docker compose build
docker compose up -d
```

## Test từng service

```bash
# Test Catalog
curl http://localhost:8081/actuator/health

# Test IAM
curl http://localhost:8082/actuator/health

# Test Gateway
curl http://localhost:8080/actuator/health
```

## Contact Support

Nếu vẫn gặp lỗi, gửi:
1. Logs: `docker compose logs > logs.txt`
2. Docker version: `docker --version`
3. Docker Compose version: `docker compose version`
4. OS và version

