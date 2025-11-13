# Hướng dẫn chạy và test BookVault với Docker

## Bước 1: Chuẩn bị môi trường

1. **Kiểm tra Docker Desktop**
   ```bash
   docker --version
   docker info
   ```

2. **Đảm bảo Docker Desktop đang chạy**
   - Mở Docker Desktop trên Windows
   - Đợi đến khi Docker daemon khởi động xong

## Bước 2: Build tất cả services

```bash
docker-compose build
```

**Thời gian build**: Khoảng 5-10 phút tùy theo tốc độ mạng và máy tính.

## Bước 3: Khởi động toàn bộ hệ thống

```bash
docker-compose up -d
```

## Bước 4: Kiểm tra trạng thái services

```bash
# Xem trạng thái tất cả containers
docker-compose ps

# Xem logs của từng service
docker-compose logs catalog
docker-compose logs borrowing
docker-compose logs search
docker-compose logs notification
```

## Bước 5: Test Event-Driven Flow

### 5.1 Test Book Creation và Elasticsearch Indexing

```bash
# 1. Tạo một book mới (lưu ý thay đổi title/author nếu cần)
curl -X POST http://localhost:8081/api/catalog/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "isbn": "978-0132350884",
    "description": "A Handbook of Agile Software Craftsmanship",
    "quantity": 10,
    "category": "Programming"
  }'

# 2. Kiểm tra book được tạo thành công
curl http://localhost:8081/api/catalog/v1/books

# 3. Search book trong Elasticsearch (chờ 5-10 giây để indexing)
curl "http://localhost:8084/api/search/v1/books?query=Clean%20Code"

# 4. Kiểm tra logs để xem events được xử lý
docker-compose logs search | grep -i "book.*created"
docker-compose logs notification | grep -i "book.*created"
```

### 5.2 Test Loan Creation và Notifications

```bash
# 1. Tạo một loan mới (thay đổi userId và bookId tùy theo dữ liệu)
curl -X POST http://localhost:8083/api/borrowing/v1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "bookId": 1,
    "quantity": 1,
    "dueDays": 14
  }'

# 2. Kiểm tra loan được tạo
curl http://localhost:8083/api/borrowing/v1/loans

# 3. Kiểm tra logs notification
docker-compose logs notification | grep -i "loan.*created"
```

### 5.3 Test Book Return

```bash
# 1. Return loan (thay đổi loanId tùy theo dữ liệu)
curl -X PUT http://localhost:8083/api/borrowing/v1/loans/1/return

# 2. Kiểm tra logs
docker-compose logs notification | grep -i "loan.*returned"
```

### 5.4 Test Overdue Notifications (Manual Test)

```bash
# Tạo một loan với due date trong quá khứ
curl -X POST http://localhost:8083/api/borrowing/v1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "bookId": 1,
    "quantity": 1,
    "dueDays": -5
  }'

# Chờ 1 giờ để scheduler chạy, hoặc restart borrowing service để trigger ngay
docker-compose restart borrowing

# Kiểm tra logs
docker-compose logs notification | grep -i "overdue"
```

## Bước 6: Kiểm tra Elasticsearch

```bash
# Truy cập Elasticsearch trực tiếp
curl http://localhost:9200/books/_search?pretty

# Hoặc dùng Search API
curl "http://localhost:8084/api/search/v1/books?query=programming"
```

## Bước 7: Kiểm tra Kafka Topics

```bash
# Vào container Kafka
docker exec -it bookvault-kafka-1 bash

# List topics
kafka-topics.sh --list --bootstrap-server localhost:9092

# Xem messages trong topic (thoát bằng Ctrl+C)
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic book.created --from-beginning
```

## Bước 8: Dừng hệ thống

```bash
# Dừng tất cả services
docker-compose down

# Dừng và xóa volumes (cẩn thận - mất dữ liệu)
docker-compose down -v
```

## Troubleshooting

### Service không khởi động được
```bash
# Kiểm tra logs chi tiết
docker-compose logs [service-name]

# Restart service cụ thể
docker-compose restart [service-name]
```

### Database connection failed
```bash
# Kiểm tra database containers
docker-compose logs catalog-postgres
docker-compose logs borrowing-postgres
```

### Kafka connection issues
```bash
# Kiểm tra Kafka container
docker-compose logs kafka
```

### Elasticsearch không hoạt động
```bash
# Kiểm tra Elasticsearch container
docker-compose logs elasticsearch
```

## Test Script tự động

Tạo file `test-bookvault.sh`:

```bash
#!/bin/bash

echo "=== Testing BookVault Event-Driven Architecture ==="

# Test 1: Create Book
echo "1. Creating test book..."
BOOK_RESPONSE=$(curl -s -X POST http://localhost:8081/api/catalog/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Book",
    "author": "Test Author",
    "isbn": "123-4567890123",
    "description": "Test Description",
    "quantity": 5,
    "category": "Test"
  }')

echo "Book created: $BOOK_RESPONSE"

# Test 2: Search Book (chờ indexing)
echo "2. Searching for book..."
sleep 5
SEARCH_RESPONSE=$(curl -s "http://localhost:8084/api/search/v1/books?query=Test%20Book")
echo "Search result: $SEARCH_RESPONSE"

# Test 3: Create Loan
echo "3. Creating loan..."
LOAN_RESPONSE=$(curl -s -X POST http://localhost:8083/api/borrowing/v1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "bookId": 1,
    "quantity": 1,
    "dueDays": 14
  }')

echo "Loan created: $LOAN_RESPONSE"

echo "=== Test completed! Check logs for event processing ==="
```

Chạy script:
```bash
chmod +x test-bookvault.sh
./test-bookvault.sh
```

## Kết quả mong đợi

Sau khi chạy test, bạn sẽ thấy:

1. **Book Creation**: Book được tạo trong Catalog Service
2. **Event Publishing**: Event được publish đến Kafka
3. **Search Indexing**: Book được index trong Elasticsearch
4. **Notification**: Email notification được gửi (nếu SMTP được cấu hình)
5. **Loan Processing**: Loan được tạo và events được xử lý

Kiểm tra logs để xác nhận các events được xử lý đúng cách!