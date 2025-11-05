# 🚀 Start Core Services Only

Nếu gặp lỗi pull images (Elasticsearch, MinIO, Kafka), có thể comment các services optional và chỉ chạy core services.

## Cách 1: Comment trong docker-compose.yml

Trong file `docker-compose.yml`, comment các services sau:

```yaml
# # Elasticsearch - comment nếu không cần search service ngay
# elasticsearch:
#   ...

# # MinIO - comment nếu không cần file storage ngay  
# minio:
#   ...

# # Media service - comment nếu không cần upload files ngay
# media:
#   ...

# # Kafka - comment nếu không cần notifications ngay
# kafka:
#   ...

# # Zookeeper - comment nếu không cần Kafka
# # zookeeper:
#   ...

# # Notification service - comment nếu không cần notifications ngay
# notification:
#   ...

# # Search service - comment nếu không cần Elasticsearch
# search:
#   ...
```

Sau đó:

```bash
docker compose build
docker compose up -d
```

## Cách 2: Dùng minimal compose

```bash
docker compose -f docker-compose.minimal.yml build
docker compose -f docker-compose.minimal.yml up -d
```

## Services Core (sẽ chạy):

✅ **Databases:**
- catalog-postgres
- iam-postgres  
- borrowing-postgres

✅ **Core Services:**
- catalog (quản lý sách)
- iam (authentication)
- borrowing (quản lý mượn sách)
- gateway (API gateway)
- admin (dashboard)

## Services Optional (có thể bỏ qua):

❌ **Search Service** - cần Elasticsearch
❌ **Media Service** - cần MinIO
❌ **Notification Service** - cần Kafka

## Test Core Services

Sau khi start, test các API:

```bash
# Health check
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health

# Register user
curl -X POST http://localhost:8080/api/iam/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# Login
curl -X POST http://localhost:8080/api/iam/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

## Thêm Services Optional sau

Khi đã test được core services, có thể thêm từng service optional:

1. **MinIO + Media Service:**
   ```bash
   # Uncomment minio và media trong docker-compose.yml
   docker compose up -d minio media
   ```

2. **Elasticsearch + Search Service:**
   ```bash
   # Pull Elasticsearch trước
   docker pull docker.elastic.co/elasticsearch/elasticsearch:8.11.0
   # Uncomment elasticsearch và search trong docker-compose.yml
   docker compose up -d elasticsearch search
   ```

3. **Kafka + Notification Service:**
   ```bash
   # Pull Kafka trước
   docker pull apache/kafka:3.6.0
   # Uncomment kafka và notification trong docker-compose.yml
   docker compose up -d kafka notification
   ```

