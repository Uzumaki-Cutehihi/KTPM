# 📖 Hướng Dẫn Hoạt Động Dự Án BookVault Microservices

## 🎯 Mục Lục

1. [Tổng Quan Kiến Trúc](#1-tổng-quan-kiến-trúc)
2. [Các Kiến Thức Cần Thiết](#2-các-kiến-thức-cần-thiết)
3. [Luồng Hoạt Động Chi Tiết](#3-luồng-hoạt-động-chi-tiết)
4. [Các Service và Chức Năng](#4-các-service-và-chức-năng)
5. [Giao Tiếp Giữa Các Service](#5-giao-tiếp-giữa-các-service)
6. [Bảo Mật và Xác Thực](#6-bảo-mật-và-xác-thực)
7. [Cơ Sở Dữ Liệu](#7-cơ-sở-dữ-liệu)
8. [Infrastructure và Deployment](#8-infrastructure-và-deployment)

---

## 1. Tổng Quan Kiến Trúc

### 1.1. Kiến Trúc Microservices

Dự án BookVault được xây dựng theo **kiến trúc Microservices** với các đặc điểm:

- **8 Services độc lập**: Mỗi service có database riêng, có thể deploy riêng biệt
- **API Gateway**: Điểm vào duy nhất cho tất cả client requests
- **Database per Service**: Mỗi service có database PostgreSQL riêng
- **Event-Driven**: Sử dụng Kafka cho giao tiếp bất đồng bộ
- **Containerized**: Tất cả services chạy trong Docker containers

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENT (Browser/App)                  │
└──────────────────────┬──────────────────────────────────┘
                        │ HTTP/HTTPS
                        ▼
┌─────────────────────────────────────────────────────────┐
│              GATEWAY SERVICE (Port 8080)                  │
│  • JWT Validation                                        │
│  • Request Routing                                       │
│  • Load Balancing                                        │
└──────┬──────────────────────────────────────────────────┘
       │
       ├───► IAM (8082) ──────────► PostgreSQL (IAM DB)
       ├───► Catalog (8081) ──────► PostgreSQL (Catalog DB)
       ├───► Borrowing (8083) ─────► PostgreSQL (Borrowing DB)
       ├───► Search (8084) ────────► Elasticsearch
       ├───► Notification (8085) ──► Kafka ─► Email
       ├───► Media (8086) ─────────► MinIO (S3)
       └───► Admin (8087) ─────────► Aggregates from other services
```

### 1.2. Domain-Driven Design (DDD)

Dự án được tổ chức theo **Domain-Driven Design**:

- **Bounded Context**: Mỗi service đại diện cho một bounded context
- **Database Isolation**: Mỗi context có database riêng
- **API Contracts**: Giao tiếp qua REST API hoặc Events

**Các Domain:**
- **IAM Domain**: Identity & Access Management
- **Catalog Domain**: Book Management
- **Borrowing Domain**: Loan Management
- **Search Domain**: Full-text Search
- **Notification Domain**: Event Notifications
- **Media Domain**: File Storage
- **Admin Domain**: Analytics & Reports

---

## 2. Các Kiến Thức Cần Thiết

### 2.1. Microservices Architecture

**Định nghĩa**: Kiến trúc phần mềm chia ứng dụng thành các service nhỏ, độc lập, giao tiếp qua network.

**Lợi ích:**
- ✅ **Scalability**: Scale từng service độc lập
- ✅ **Technology Diversity**: Dùng công nghệ khác nhau cho từng service
- ✅ **Fault Isolation**: Lỗi một service không ảnh hưởng toàn bộ
- ✅ **Team Autonomy**: Mỗi team phát triển service riêng

**Thách thức:**
- ⚠️ **Distributed System Complexity**: Phức tạp hơn monolithic
- ⚠️ **Network Latency**: Giao tiếp qua network chậm hơn function call
- ⚠️ **Data Consistency**: Khó đảm bảo consistency giữa các service

### 2.2. API Gateway Pattern

**Định nghĩa**: Một điểm vào duy nhất (single entry point) cho tất cả client requests.

**Chức năng:**
- **Routing**: Chuyển request đến service phù hợp
- **Authentication/Authorization**: Validate JWT tokens
- **Load Balancing**: Phân tải requests
- **Rate Limiting**: Giới hạn số requests
- **Request Transformation**: Thêm headers, modify request

**Trong dự án:**
- Gateway sử dụng **Spring Cloud Gateway** (Reactive)
- Routing dựa trên path: `/api/{service}/**`
- JWT validation cho tất cả requests (trừ public endpoints)

### 2.3. JWT (JSON Web Token) Authentication

**JWT là gì?**
- Token-based authentication
- Chứa thông tin user (claims) được mã hóa
- Ký bằng private key (RS256)
- Xác thực bằng public key

**Cấu trúc JWT:**
```
Header.Payload.Signature
```

**Luồng hoạt động:**

1. **User đăng nhập** → IAM Service
   ```
   POST /api/iam/v1/auth/login
   Body: { "username": "user", "password": "pass" }
   ```

2. **IAM tạo JWT** (RS256)
   - Private key: IAM giữ bí mật
   - Public key: Gateway dùng để verify

3. **Client gửi request với JWT**
   ```
   Authorization: Bearer <JWT_TOKEN>
   ```

4. **Gateway verify JWT**
   - Dùng public key để verify signature
   - Check expiration
   - Extract claims (username, role)

5. **Forward request** đến service backend

**RS256 (RSA + SHA-256):**
- Asymmetric algorithm
- Private key: để sign token (chỉ IAM có)
- Public key: để verify token (Gateway có)
- An toàn hơn HS256 (symmetric)

### 2.4. Database per Service Pattern

**Định nghĩa**: Mỗi microservice có database riêng, không chia sẻ database với service khác.

**Lợi ích:**
- ✅ **Data Isolation**: Dữ liệu độc lập, không conflict
- ✅ **Technology Choice**: Mỗi service có thể dùng DB khác nhau
- ✅ **Scalability**: Scale database độc lập
- ✅ **Team Autonomy**: Mỗi team quản lý DB riêng

**Trong dự án:**
- **Catalog Service** → `catalog-postgres` (port 5433)
- **IAM Service** → `iam-postgres` (port 5434)
- **Borrowing Service** → `borrowing-postgres` (port 5435)

### 2.5. Event-Driven Architecture

**Định nghĩa**: Services giao tiếp qua events (messages) thay vì gọi trực tiếp.

**Lợi ích:**
- ✅ **Loose Coupling**: Services không biết nhau trực tiếp
- ✅ **Scalability**: Xử lý bất đồng bộ, không block
- ✅ **Resilience**: Nếu một service down, events được queue

**Trong dự án:**
- **Kafka** làm message broker
- **Borrowing Service** → publish events → **Notification Service** consume
- **Catalog Service** → publish events → **Search Service** index

### 2.6. Docker & Docker Compose

**Docker Container:**
- Package application và dependencies vào container
- Chạy độc lập, không phụ thuộc host OS
- Dễ deploy, scale, rollback

**Docker Compose:**
- Orchestrate nhiều containers
- Define services, networks, volumes
- Start/stop toàn bộ stack với 1 lệnh

**Trong dự án:**
- Mỗi service có `Dockerfile`
- `docker-compose.yml` định nghĩa toàn bộ stack
- Services giao tiếp qua Docker network

### 2.7. Spring Boot & Spring Cloud

**Spring Boot:**
- Framework Java để build microservices
- Auto-configuration, embedded server
- Production-ready features (actuator, metrics)

**Spring Cloud Gateway:**
- API Gateway implementation
- Reactive (non-blocking)
- Route predicates, filters

**Spring Security:**
- Authentication & Authorization
- JWT support với `spring-security-oauth2-jose`

### 2.8. Flyway Database Migrations

**Định nghĩa**: Version control cho database schema.

**Cách hoạt động:**
- SQL scripts trong `db/migration/`
- Naming: `V{version}__{description}.sql`
- Flyway tự động chạy khi app start
- Track migration history trong `flyway_schema_history`

**Ví dụ:**
```
V1__init.sql          # Tạo tables
V2__add_indexes.sql   # Thêm indexes
V3__update_schema.sql # Update schema
```

---

## 3. Luồng Hoạt Động Chi Tiết

### 3.1. Luồng Đăng Ký/Đăng Nhập

```
1. Client → POST /api/iam/v1/auth/register
   │
   ▼
2. Gateway (8080)
   • StripPrefix: /api/iam → Forward to IAM
   │
   ▼
3. IAM Service (8082)
   • Validate input
   • Hash password (BCrypt)
   • Save to PostgreSQL (iam-postgres)
   • Return success
   │
   ▼
4. Client ← 201 Created

---

1. Client → POST /api/iam/v1/auth/login
   │
   ▼
2. Gateway (8080)
   • Route to IAM (public endpoint, no JWT)
   │
   ▼
3. IAM Service (8082)
   • Verify username/password
   • Generate JWT (RS256)
   • Return token
   │
   ▼
4. Client ← JWT Token
   {
     "accessToken": "eyJhbGciOiJSUzI1NiIs...",
     "tokenType": "Bearer"
   }
```

### 3.2. Luồng Tạo Book (Cần Authentication)

```
1. Client → POST /api/catalog/v1/books
   Headers: Authorization: Bearer <JWT>
   Body: { "title": "...", "author": "..." }
   │
   ▼
2. Gateway (8080)
   • Extract JWT từ header
   • Verify JWT với public key
   • Check expiration, signature
   • Extract claims (username, role)
   • Route to Catalog service
   │
   ▼
3. Catalog Service (8081)
   • Validate request body
   • Save to PostgreSQL (catalog-postgres)
   • Return created book
   │
   ▼
4. Client ← 201 Created + Book data
```

### 3.3. Luồng Mượn Sách (Cross-Service Communication)

```
1. Client → POST /api/borrowing/v1/loans
   Headers: Authorization: Bearer <JWT>
   Body: { "bookId": "123", "quantity": 1 }
   │
   ▼
2. Gateway (8080)
   • Verify JWT
   • Route to Borrowing service
   │
   ▼
3. Borrowing Service (8083)
   • Validate request
   • Call Catalog Service (HTTP):
     POST /api/catalog/v1/books/{bookId}/decrease-quantity
     Body: { "quantity": 1 }
   │
   ▼
4. Catalog Service (8081)
   • Check available quantity
   • Decrease quantity in database
   • Return success
   │
   ▼
5. Borrowing Service (8083)
   • Create loan record
   • Save to PostgreSQL (borrowing-postgres)
   • Publish event to Kafka: "loan.created"
   • Return loan data
   │
   ▼
6. Notification Service (8085)
   • Consume "loan.created" event from Kafka
   • Send email to user
   │
   ▼
7. Client ← 201 Created + Loan data
```

### 3.4. Luồng Tìm Kiếm (Elasticsearch)

```
1. Client → GET /api/search/v1/search?q=spring
   │
   ▼
2. Gateway (8080)
   • Route to Search service
   │
   ▼
3. Search Service (8084)
   • Query Elasticsearch
   • Full-text search trên title, author
   • Return results
   │
   ▼
4. Client ← Search results
```

### 3.5. Luồng Upload File (MinIO)

```
1. Client → POST /api/media/v1/files
   Headers: Authorization: Bearer <JWT>
   Body: multipart/form-data (file)
   │
   ▼
2. Gateway (8080)
   • Verify JWT
   • Route to Media service
   │
   ▼
3. Media Service (8086)
   • Upload file to MinIO
   • Store metadata
   • Return file URL
   │
   ▼
4. Client ← File URL
```

---

## 4. Các Service và Chức Năng

### 4.1. Gateway Service (Port 8080)

**Công nghệ:**
- Spring Cloud Gateway (Reactive)
- Spring Security Reactive
- NimbusReactiveJwtDecoder

**Chức năng:**
- **Routing**: Chuyển requests đến service phù hợp
- **JWT Validation**: Verify tokens trước khi forward
- **Request Transformation**: Thêm headers (X-Trace-Id)

**Routes:**
```yaml
/api/iam/**      → IAM Service (8082)
/api/catalog/**  → Catalog Service (8081)
/api/borrowing/** → Borrowing Service (8083)
/api/search/**   → Search Service (8084)
/api/notification/** → Notification Service (8085)
/api/media/**    → Media Service (8086)
/api/admin/**    → Admin Service (8087)
```

**Security:**
- Public endpoints: `/api/iam/**`, `/actuator/**`, `/swagger-ui/**`
- Protected endpoints: Tất cả còn lại cần JWT

### 4.2. IAM Service (Port 8082)

**Công nghệ:**
- Spring Boot 3.3.4
- Spring Security
- JWT (jjwt 0.11.5, RS256)
- PostgreSQL + Flyway
- BCrypt password hashing

**Endpoints:**
- `POST /api/iam/v1/auth/register` - Đăng ký
- `POST /api/iam/v1/auth/login` - Đăng nhập
- `GET /api/iam/v1/auth/public-key` - Lấy public key (cho Gateway)

**Database Schema:**
```sql
users (
  id, username, email, password_hash, role, created_at
)
```

**JWT Claims:**
- `sub`: username
- `role`: USER hoặc ADMIN
- `iss`: bookvault-iam
- `exp`: expiration time

### 4.3. Catalog Service (Port 8081)

**Công nghệ:**
- Spring Boot 3.3.4
- Spring Data JPA
- PostgreSQL + Flyway

**Endpoints:**
- `GET /api/catalog/v1/books` - List books
- `GET /api/catalog/v1/books/{id}` - Get book
- `POST /api/catalog/v1/books` - Create book
- `PUT /api/catalog/v1/books/{id}` - Update book
- `DELETE /api/catalog/v1/books/{id}` - Delete book
- `POST /api/catalog/v1/books/{id}/decrease-quantity` - Decrease (internal)
- `POST /api/catalog/v1/books/{id}/increase-quantity` - Increase (internal)

**Database Schema:**
```sql
books (
  id, title, author, isbn, quantity, created_at, updated_at
)
```

### 4.4. Borrowing Service (Port 8083)

**Công nghệ:**
- Spring Boot 3.3.4
- Spring Data JPA
- RestTemplate (gọi Catalog Service)
- PostgreSQL + Flyway

**Endpoints:**
- `POST /api/borrowing/v1/loans` - Tạo loan
- `GET /api/borrowing/v1/loans/{id}` - Get loan
- `GET /api/borrowing/v1/loans/user/{userId}` - Get user loans
- `POST /api/borrowing/v1/loans/{id}/return` - Trả sách

**Database Schema:**
```sql
loans (
  id, user_id, status, created_at, updated_at
)
loan_items (
  id, loan_id, book_id, quantity, created_at
)
fines (
  id, loan_id, amount, status, created_at, paid_at
)
```

**Workflow:**
1. Tạo loan → Gọi Catalog để giảm quantity
2. Trả sách → Gọi Catalog để tăng quantity
3. Publish events → Kafka (loan.created, loan.overdue)

### 4.5. Search Service (Port 8084)

**Công nghệ:**
- Spring Boot 3.3.4
- Spring Data Elasticsearch
- Elasticsearch 8.11.0

**Endpoints:**
- `GET /api/search/v1/search?q={query}` - Full-text search
- `POST /api/search/v1/index` - Index book (internal)
- `DELETE /api/search/v1/index/{id}` - Remove from index

**Elasticsearch Index:**
```json
{
  "id": "book-id",
  "title": "Spring Boot Guide",
  "author": "John Doe",
  "isbn": "978-1234567890"
}
```

### 4.6. Notification Service (Port 8085)

**Công nghệ:**
- Spring Boot 3.3.4
- Spring Kafka
- Spring Mail (SMTP)
- ObjectMapper (JSON)

**Chức năng:**
- Consume events từ Kafka
- Send email notifications
- WebSocket notifications (future)

**Kafka Topics:**
- `loan.created` - Khi loan được tạo
- `loan.overdue` - Khi loan quá hạn

**Email Templates:**
- Loan confirmation
- Due date reminder
- Overdue notice

### 4.7. Media Service (Port 8086)

**Công nghệ:**
- Spring Boot 3.3.4
- MinIO Client (S3-compatible)

**Endpoints:**
- `POST /api/media/v1/files` - Upload file
- `GET /api/media/v1/files/{id}` - Download file
- `DELETE /api/media/v1/files/{id}` - Delete file

**Storage:**
- MinIO bucket: `bookvault-media`
- File types: images, PDFs, documents

### 4.8. Admin Service (Port 8087)

**Công nghệ:**
- Spring Boot 3.3.4
- RestTemplate (aggregate data)

**Endpoints:**
- `GET /api/admin/v1/stats` - Thống kê tổng quan
- `GET /api/admin/v1/loans` - Tất cả loans
- `GET /api/admin/v1/books` - Tất cả books

**Chức năng:**
- Aggregate data từ Catalog, Borrowing
- Dashboard, reports
- Read-only operations

---

## 5. Giao Tiếp Giữa Các Service

### 5.1. Synchronous Communication (HTTP/REST)

**Sử dụng khi:**
- Cần response ngay lập tức
- Business logic cần kết quả trước khi tiếp tục

**Ví dụ:**
- Borrowing → Catalog (check/decrease quantity)
- Admin → Catalog, Borrowing (aggregate stats)

**Công nghệ:**
- RestTemplate (Spring)
- WebClient (Reactive, future)

### 5.2. Asynchronous Communication (Kafka)

**Sử dụng khi:**
- Không cần response ngay
- Event-driven operations
- Decouple services

**Ví dụ:**
- Borrowing → Notification (loan.created event)
- Catalog → Search (book.indexed event)

**Kafka Setup:**
- Zookeeper: Coordination (port 2181)
- Kafka: Message broker (port 29092)
- Topics: `loan.created`, `loan.overdue`, `book.indexed`

**Producer (Borrowing Service):**
```java
@Autowired
KafkaTemplate<String, String> kafkaTemplate;

kafkaTemplate.send("loan.created", loanJson);
```

**Consumer (Notification Service):**
```java
@KafkaListener(topics = "loan.created")
public void handleLoanCreated(String message) {
    // Process event
}
```

---

## 6. Bảo Mật và Xác Thực

### 6.1. JWT Authentication Flow

**1. Key Generation (IAM Service):**
- IAM tự động generate RSA key pair khi start (nếu không config)
- Private key: Dùng để sign JWT
- Public key: Expose qua endpoint `/api/iam/v1/auth/public-key`

**2. Login Flow:**
```
User → POST /api/iam/v1/auth/login
IAM → Verify credentials
IAM → Generate JWT (RS256)
IAM → Return token
```

**3. Request Flow:**
```
Client → Request + JWT token
Gateway → Verify JWT (public key)
Gateway → Extract claims
Gateway → Forward to service
```

**4. JWT Structure:**
```json
{
  "sub": "username",
  "role": "USER",
  "iss": "bookvault-iam",
  "iat": 1234567890,
  "exp": 1234571490
}
```

### 6.2. Public Key Configuration

**Gateway cần public key để verify JWT:**

1. **Lấy public key từ IAM:**
   ```bash
   curl http://localhost:8082/api/iam/v1/auth/public-key
   ```

2. **Set environment variable:**
   ```yaml
   environment:
     JWT_PUBLIC_KEY_PEM: |
       -----BEGIN PUBLIC KEY-----
       MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...
       -----END PUBLIC KEY-----
   ```

3. **Gateway tự động verify** với public key này

### 6.3. Role-Based Access Control (RBAC)

**Roles:**
- `USER`: End user, có thể mượn sách
- `ADMIN`: Quản trị viên, có thể quản lý books, users

**Trong JWT:**
```json
{
  "role": "USER"  // hoặc "ADMIN"
}
```

**Authorization (future):**
- Gateway có thể check role trong JWT
- Forward chỉ khi có quyền

---

## 7. Cơ Sở Dữ Liệu

### 7.1. Database per Service

**Mỗi service có database riêng:**

| Service | Database | Port | Schema |
|---------|----------|------|--------|
| Catalog | catalog-postgres | 5433 | books |
| IAM | iam-postgres | 5434 | users |
| Borrowing | borrowing-postgres | 5435 | loans, loan_items, fines |

**Lợi ích:**
- Isolation: Dữ liệu độc lập
- Scalability: Scale từng DB riêng
- Technology: Có thể dùng DB khác nhau

### 7.2. Flyway Migrations

**Cấu trúc:**
```
services/{service}/src/main/resources/db/migration/
├── V1__init.sql          # Tạo tables
├── V2__add_indexes.sql   # Thêm indexes
└── V3__update_schema.sql # Update schema
```

**Naming Convention:**
- `V{version}__{description}.sql`
- Version: số nguyên tăng dần
- Description: mô tả migration

**Ví dụ (Catalog Service):**
```sql
-- V1__init.sql
CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn VARCHAR(50) UNIQUE,
    quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
```

**Flyway tự động:**
- Chạy khi app start
- Check version trong `flyway_schema_history`
- Chỉ chạy migrations chưa chạy

### 7.3. JPA/Hibernate

**ORM (Object-Relational Mapping):**
- Map Java objects ↔ Database tables
- Auto-generate queries
- Relationship management

**Ví dụ Entity (Catalog Service):**
```java
@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    private String author;
    private String isbn;
    private Integer quantity;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

---

## 8. Infrastructure và Deployment

### 8.1. Docker Containers

**Mỗi service có Dockerfile:**
```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . /workspace
RUN mvn -q -e -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /workspace/target/*-service-*.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

**Multi-stage build:**
- Build stage: Compile code
- Runtime stage: Chỉ chứa JAR và JRE

### 8.2. Docker Compose

**Orchestration:**
```yaml
services:
  catalog-postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: catalog
    ports:
      - "5433:5432"
  
  catalog:
    build:
      context: ./services/catalog
    depends_on:
      catalog-postgres:
        condition: service_healthy
    ports:
      - "8081:8081"
```

**Features:**
- Service dependencies (`depends_on`)
- Health checks
- Network isolation
- Volume mounts

### 8.3. Kubernetes (Helm Charts)

**Helm Charts:**
```
platform/helm/
├── catalog/
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
│       ├── deployment.yaml
│       └── service.yaml
```

**Deploy:**
```bash
helm install catalog ./platform/helm/catalog -n bookvault
```

### 8.4. External Services

**Elasticsearch:**
- Full-text search engine
- Index books for search
- Port 9200

**MinIO:**
- S3-compatible storage
- File upload/download
- Ports 9000 (API), 9001 (Console)

**Kafka:**
- Message broker
- Event streaming
- Port 29092 (external), 9092 (internal)

**Zookeeper:**
- Coordination service cho Kafka
- Port 2181

---

## 📚 Tài Liệu Tham Khảo

### Kiến Trúc
- [Microservices Patterns](https://microservices.io/patterns/)
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/)
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)

### Công Nghệ
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [JWT.io](https://jwt.io/)
- [Docker Documentation](https://docs.docker.com/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)

### Best Practices
- [12-Factor App](https://12factor.net/)
- [Microservices Best Practices](https://martinfowler.com/articles/microservices.html)

---

## 🎓 Kết Luận

Dự án BookVault Microservices là một hệ thống phức tạp với nhiều công nghệ và pattern. Để hiểu và phát triển dự án:

1. **Nắm vững kiến trúc Microservices**: Service independence, database per service
2. **Hiểu JWT Authentication**: Public/private key, RS256 algorithm
3. **Biết cách services giao tiếp**: HTTP/REST, Kafka events
4. **Thành thạo Docker**: Containerization, Docker Compose
5. **Hiểu Spring Boot**: Auto-configuration, dependency injection
6. **Database migrations**: Flyway, version control cho schema

**Bước tiếp theo:**
- Đọc code từng service
- Chạy dự án local
- Test các API endpoints
- Thêm features mới

---

*Document được tạo tự động từ codebase - Cập nhật: 2024*

