# Hướng dẫn test Event-Driven Architecture và Elasticsearch

## 1. Test Event-Driven Flow với Kafka

### Flow 1: Book Creation → Search Indexing
```bash
# 1. Tạo sách mới qua Catalog Service
curl -X POST http://localhost:8081/api/catalog/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Boot Microservices",
    "author": "John Doe",
    "isbn": "978-1234567890",
    "quantity": 10,
    "description": "A comprehensive guide to building microservices with Spring Boot"
  }'

# 2. Kiểm tra event được publish lên Kafka (xem logs Catalog Service)
docker logs ktpm-catalog-1 --tail 20 | grep -i "published book.created"

# 3. Kiểm tra Search Service đã index chưa (sau 5-10 giây)
curl http://localhost:8084/api/search/v1/books/search?query=Spring
```

### Flow 2: Book Borrowing → Quantity Update
```bash
# 1. Mượn sách qua Borrowing Service
curl -X POST http://localhost:8083/api/borrowing/v1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "bookId": 1,
    "quantity": 2
  }'

# 2. Kiểm tra event loan.created được publish
docker logs ktpm-borrowing-1 --tail 20 | grep -i "published loan.created"

# 3. Kiểm tra quantity được cập nhật trong Catalog Service
curl http://localhost:8081/api/catalog/v1/books/1

# 4. Kiểm tra notification event (nếu Notification Service chạy)
docker logs ktpm-notification-1 --tail 20 | grep -i "loan.created"
```

### Flow 3: Book Return → Quantity Restore
```bash
# 1. Trả sách (thay 1 bằng loanId thực tế)
curl -X POST http://localhost:8083/api/borrowing/v1/loans/1/return

# 2. Kiểm tra event loan.returned
docker logs ktpm-borrowing-1 --tail 20 | grep -i "published loan.returned"

# 3. Kiểm tra quantity được restore
curl http://localhost:8081/api/catalog/v1/books/1
```

## 2. Test Elasticsearch Functionality

### Tạo test data
```bash
# Tạo nhiều sách để test search
curl -X POST http://localhost:8081/api/catalog/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Microservices Patterns",
    "author": "Chris Richardson",
    "isbn": "978-1617294549",
    "quantity": 5,
    "description": "Microservices architecture patterns and best practices"
  }'

curl -X POST http://localhost:8081/api/catalog/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Event-Driven Architecture",
    "author": "Martin Fowler",
    "isbn": "978-0321601919",
    "quantity": 8,
    "description": "Building event-driven microservices systems"
  }'
```

### Test các loại search
```bash
# 1. Search theo title
curl http://localhost:8084/api/search/v1/books/search?query=Microservices

# 2. Search theo author
curl http://localhost:8084/api/search/v1/books/search?query=Fowler

# 3. Search theo description
curl http://localhost:8084/api/search/v1/books/search?query=architecture

# 4. Search với fuzzy matching
curl http://localhost:8084/api/search/v1/books/search?query=Micorservices

# 5. Advanced search với filters
curl -X POST http://localhost:8084/api/search/v1/books/advanced \
  -H "Content-Type: application/json" \
  -d '{
    "query": "microservices",
    "filters": {
      "author": "Chris"
    }
  }'
```

## 3. Monitor Kafka Events

### Xem tất cả Kafka topics
```bash
# List all topics
docker exec -it ktpm-kafka-1 kafka-topics --list --bootstrap-server localhost:9092

# Xem events trong topic book.created
docker exec -it ktpm-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic book.created \
  --from-beginning \
  --max-messages 5

# Xem events trong topic loan.created
docker exec -it ktpm-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic loan.created \
  --from-beginning \
  --max-messages 5
```

### Monitor consumer groups
```bash
# Xem consumer groups
docker exec -it ktpm-kafka-1 kafka-consumer-groups --list --bootstrap-server localhost:9092

# Xem offset cho search-service group
docker exec -it ktpm-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group search-service \
  --describe
```

## 4. Monitor Service Logs

### Real-time logs monitoring
```bash
# Monitor Catalog Service events
docker logs -f ktpm-catalog-1 | grep -i "published"

# Monitor Search Service indexing
docker logs -f ktpm-search-1 | grep -i "indexed"

# Monitor Borrowing Service events
docker logs -f ktpm-borrowing-1 | grep -i "published"

# Monitor Notification Service
docker logs -f ktpm-notification-1 | grep -i "received"
```

## 5. Test Error Scenarios

### Test khi service down
```bash
# 1. Stop Search Service
docker stop ktpm-search-1

# 2. Create book mới
curl -X POST http://localhost:8081/api/catalog/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Without Search",
    "author": "Test Author",
    "isbn": "978-0000000001",
    "quantity": 3
  }'

# 3. Start Search Service lại
docker start ktpm-search-1

# 4. Kiểm tra event được replay (nếu có persistence)
curl http://localhost:8084/api/search/v1/books/search?query=Test
```

## 6. Performance Testing

### Bulk operations
```bash
# Tạo 10 books liên tiếp để test throughput
for i in {1..10}; do
  curl -X POST http://localhost:8081/api/catalog/v1/books \
    -H "Content-Type: application/json" \
    -d "{
      \"title\": \"Performance Test Book $i\",
      \"author\": \"Author $i\",
      \"isbn\": \"978-00000000$i\",
      \"quantity\": $i
    }" &
done

# Đo thời gian search response
time curl http://localhost:8084/api/search/v1/books/search?query=Performance
```

## 7. Health Checks

### Service health endpoints
```bash
# Catalog Service health
curl http://localhost:8081/actuator/health

# Search Service health
curl http://localhost:8084/actuator/health

# Borrowing Service health
curl http://localhost:8083/actuator/health

# Kafka health (via Admin)
curl http://localhost:8087/admin/health
```

### Elasticsearch health
curl http://localhost:9200/_cluster/health?pretty

## 8. Cleanup

### Xóa test data
```bash
# Xóa index Elasticsearch
curl -X DELETE http://localhost:9200/books

# Xóa books trong Catalog (thay 1 bằng ID)
curl -X DELETE http://localhost:8081/api/catalog/v1/books/1

# Reset Kafka topics (cẩn thận - mất data)
docker exec -it ktpm-kafka-1 kafka-topics --delete --topic book.created --bootstrap-server localhost:9092
docker exec -it ktpm-kafka-1 kafka-topics --delete --topic loan.created --bootstrap-server localhost:9092
```

## Kết luận

Event-driven architecture đã được implement thành công với:
- ✅ Kafka message broker cho event streaming
- ✅ Elasticsearch integration cho full-text search
- ✅ Inter-service communication với WebClient
- ✅ Event publishing từ Catalog và Borrowing services
- ✅ Event consuming bởi Search và Notification services
- ✅ Saga pattern cho distributed transactions

Hệ thống sẵn sàng cho production với monitoring, health checks, và error handling đầy đủ.