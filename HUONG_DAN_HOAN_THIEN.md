# 🚀 Hướng Dẫn Hoàn Thiện Dự Án BookVault

## 📋 Tổng Quan

Tài liệu này hướng dẫn chi tiết cách hoàn thiện các phần còn thiếu trong dự án theo yêu cầu kiến trúc:

1. ✅ **Thêm Unit + Integration Tests** (Đã tạo)
2. 🔄 **Hoàn thiện Event-Driven Design** (Đang làm)
3. 🔄 **Tái cấu trúc theo Layer** (Đang làm)
4. ⏳ **Tách Frontend Pipeline** (Hướng dẫn)

---

## 1. ✅ Unit + Integration Tests

### 1.1. Đã Tạo Tests Cho Catalog Service

**Files đã tạo:**
- ✅ `services/catalog/src/test/java/.../BookServiceTest.java` - Unit tests
- ✅ `services/catalog/src/test/java/.../BookControllerTest.java` - Controller tests
- ✅ `services/catalog/src/test/java/.../BookControllerIntegrationTest.java` - Integration tests

**Dependencies đã thêm:**
- `spring-boot-starter-test`
- `testcontainers` (PostgreSQL container cho integration tests)

### 1.2. Chạy Tests

```bash
# Chạy tất cả tests
cd services/catalog
mvn test

# Chạy chỉ unit tests
mvn test -Dtest=BookServiceTest

# Chạy chỉ integration tests
mvn test -Dtest=BookControllerIntegrationTest
```

### 1.3. Tạo Tests Cho Các Services Khác

**Lặp lại cho IAM Service:**

1. Tạo test structure:
```bash
mkdir -p services/iam/src/test/java/com/scar/bookvault/iam/auth
mkdir -p services/iam/src/test/java/com/scar/bookvault/iam/user
```

2. Tạo `AuthServiceTest.java`:
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private JwtService jwtService;
    
    @InjectMocks
    private AuthService authService;
    
    @Test
    void shouldRegisterUser() {
        // Test implementation
    }
    
    @Test
    void shouldLoginUser() {
        // Test implementation
    }
}
```

3. Tạo `AuthControllerIntegrationTest.java` với Testcontainers

**Lặp lại cho Borrowing Service, Gateway Service, v.v.**

---

## 2. 🔄 Hoàn Thiện Event-Driven Design

### 2.1. Thêm Kafka Producer Cho Catalog Service

**Bước 1: Thêm Dependencies**

```xml
<!-- services/catalog/pom.xml -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

**Bước 2: Tạo Event Publisher Service**

```java
// services/catalog/src/main/java/com/scar/bookvault/catalog/event/BookEventPublisher.java
package com.scar.bookvault.catalog.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scar.bookvault.catalog.book.Book;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public BookEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                             ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    public void publishBookCreated(Book book) {
        try {
            String event = objectMapper.writeValueAsString(Map.of(
                "eventType", "book.created",
                "bookId", book.getId(),
                "title", book.getTitle(),
                "author", book.getAuthor(),
                "isbn", book.getIsbn(),
                "timestamp", System.currentTimeMillis()
            ));
            kafkaTemplate.send("book.created", event);
        } catch (Exception e) {
            // Log error, don't fail the operation
            System.err.println("Failed to publish book.created event: " + e.getMessage());
        }
    }
    
    public void publishBookUpdated(Book book) {
        try {
            String event = objectMapper.writeValueAsString(Map.of(
                "eventType", "book.updated",
                "bookId", book.getId(),
                "title", book.getTitle(),
                "timestamp", System.currentTimeMillis()
            ));
            kafkaTemplate.send("book.updated", event);
        } catch (Exception e) {
            System.err.println("Failed to publish book.updated event: " + e.getMessage());
        }
    }
}
```

**Bước 3: Update BookService**

```java
// services/catalog/src/main/java/com/scar/bookvault/catalog/book/BookService.java
@Service
public class BookService {
    private final BookRepository bookRepository;
    private final BookEventPublisher eventPublisher;
    
    public BookService(BookRepository bookRepository, BookEventPublisher eventPublisher) {
        this.bookRepository = bookRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Transactional
    public Book create(Book book) {
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            throw new IllegalArgumentException("ISBN already exists");
        }
        book.setCreatedAt(java.time.OffsetDateTime.now());
        book.setUpdatedAt(java.time.OffsetDateTime.now());
        Book saved = bookRepository.save(book);
        
        // Publish event
        eventPublisher.publishBookCreated(saved);
        
        return saved;
    }
    
    @Transactional
    public Book update(Long id, Book incoming) {
        Book existing = get(id);
        existing.setTitle(incoming.getTitle());
        existing.setAuthor(incoming.getAuthor());
        existing.setIsbn(incoming.getIsbn());
        existing.setQuantity(incoming.getQuantity());
        existing.setUpdatedAt(java.time.OffsetDateTime.now());
        Book saved = bookRepository.save(existing);
        
        // Publish event
        eventPublisher.publishBookUpdated(saved);
        
        return saved;
    }
}
```

**Bước 4: Cấu hình Kafka**

```yaml
# services/catalog/src/main/resources/application.yml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

### 2.2. Thêm Kafka Producer Cho Borrowing Service

**Bước 1: Thêm Dependencies**

```xml
<!-- services/borrowing/pom.xml -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**Bước 2: Tạo LoanService với Event Publishing**

```java
// services/borrowing/src/main/java/com/scar/bookvault/borrowing/service/LoanService.java
package com.scar.bookvault.borrowing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scar.bookvault.borrowing.domain.Loan;
import com.scar.bookvault.borrowing.domain.LoanRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public LoanService(LoanRepository loanRepository,
                      KafkaTemplate<String, String> kafkaTemplate,
                      ObjectMapper objectMapper) {
        this.loanRepository = loanRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Transactional
    public Loan createLoan(Long userId, Long bookId, Integer quantity) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setBorrowedAt(OffsetDateTime.now());
        loan.setDueAt(OffsetDateTime.now().plusDays(14)); // 14 days loan
        
        Loan saved = loanRepository.save(loan);
        
        // Publish event
        try {
            String event = objectMapper.writeValueAsString(Map.of(
                "eventType", "loan.created",
                "loanId", saved.getId(),
                "userId", userId,
                "bookId", bookId,
                "quantity", quantity,
                "dueAt", saved.getDueAt().toString(),
                "timestamp", System.currentTimeMillis()
            ));
            kafkaTemplate.send("loan.created", event);
        } catch (Exception e) {
            System.err.println("Failed to publish loan.created event: " + e.getMessage());
        }
        
        return saved;
    }
    
    @Transactional
    public void returnLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        
        loan.setReturnedAt(OffsetDateTime.now());
        loanRepository.save(loan);
        
        // Publish event if overdue
        if (loan.getDueAt().isBefore(OffsetDateTime.now())) {
            try {
                String event = objectMapper.writeValueAsString(Map.of(
                    "eventType", "loan.overdue",
                    "loanId", loanId,
                    "userId", loan.getUserId(),
                    "timestamp", System.currentTimeMillis()
                ));
                kafkaTemplate.send("loan.overdue", event);
            } catch (Exception e) {
                System.err.println("Failed to publish loan.overdue event: " + e.getMessage());
            }
        }
    }
}
```

**Bước 3: Update LoanController**

```java
// services/borrowing/src/main/java/com/scar/bookvault/borrowing/api/LoanController.java
@RestController
@RequestMapping("/api/borrowing/v1/loans")
public class LoanController {
    private final LoanService loanService;
    
    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Loan create(@RequestBody CreateLoanRequest request) {
        return loanService.createLoan(
            request.getUserId(),
            request.getBookId(),
            request.getQuantity()
        );
    }
    
    @PostMapping("/{id}/return")
    public void returnLoan(@PathVariable Long id) {
        loanService.returnLoan(id);
    }
}
```

### 2.3. Update Search Service Consumer

```java
// services/search/src/main/java/com/scar/bookvault/search/service/SearchService.java
@Service
public class SearchService {
    private final BookSearchRepository searchRepository;
    
    @KafkaListener(topics = "book.created", groupId = "search-service")
    public void handleBookCreated(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            BookDocument doc = new BookDocument();
            doc.setId(Long.parseLong(event.get("bookId").toString()));
            doc.setTitle((String) event.get("title"));
            doc.setAuthor((String) event.get("author"));
            doc.setIsbn((String) event.get("isbn"));
            searchRepository.save(doc);
        } catch (Exception e) {
            System.err.println("Error indexing book: " + e.getMessage());
        }
    }
    
    @KafkaListener(topics = "book.updated", groupId = "search-service")
    public void handleBookUpdated(String message) {
        // Similar implementation
    }
}
```

---

## 3. 🔄 Tái Cấu Trúc Theo Layer

### 3.1. Cấu Trúc Lý Tưởng

```
services/catalog/src/main/java/com/scar/bookvault/catalog/
├── CatalogServiceApplication.java
├── api/                          # API Layer (Controllers)
│   └── BookController.java
├── service/                      # Service Layer (Business Logic)
│   └── BookService.java
├── domain/                       # Domain Layer (Entities, Repositories)
│   ├── Book.java
│   └── BookRepository.java
├── dto/                          # DTO Layer (Data Transfer Objects)
│   ├── CreateBookRequest.java
│   ├── UpdateBookRequest.java
│   └── BookResponse.java
├── event/                        # Event Layer
│   └── BookEventPublisher.java
└── config/                       # Config Layer
    └── KafkaConfig.java
```

### 3.2. Refactor Catalog Service

**Bước 1: Tạo DTOs**

```java
// services/catalog/src/main/java/com/scar/bookvault/catalog/dto/CreateBookRequest.java
package com.scar.bookvault.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookRequest(
    @NotBlank String title,
    @NotBlank String author,
    @NotBlank String isbn,
    @NotNull Integer quantity
) {}
```

```java
// services/catalog/src/main/java/com/scar/bookvault/catalog/dto/BookResponse.java
package com.scar.bookvault.catalog.dto;

import java.time.OffsetDateTime;

public record BookResponse(
    Long id,
    String title,
    String author,
    String isbn,
    Integer quantity,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static BookResponse from(com.scar.bookvault.catalog.domain.Book book) {
        return new BookResponse(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getIsbn(),
            book.getQuantity(),
            book.getCreatedAt(),
            book.getUpdatedAt()
        );
    }
}
```

**Bước 2: Di chuyển Files**

```bash
# Tạo cấu trúc mới
mkdir -p services/catalog/src/main/java/com/scar/bookvault/catalog/{api,service,domain,dto,event,config}

# Di chuyển files
mv services/catalog/src/main/java/com/scar/bookvault/catalog/book/BookController.java \
   services/catalog/src/main/java/com/scar/bookvault/catalog/api/

mv services/catalog/src/main/java/com/scar/bookvault/catalog/book/BookService.java \
   services/catalog/src/main/java/com/scar/bookvault/catalog/service/

mv services/catalog/src/main/java/com/scar/bookvault/catalog/book/Book.java \
   services/catalog/src/main/java/com/scar/bookvault/catalog/domain/

mv services/catalog/src/main/java/com/scar/bookvault/catalog/book/BookRepository.java \
   services/catalog/src/main/java/com/scar/bookvault/catalog/domain/
```

**Bước 3: Update Package Names**

Cập nhật `package` declarations trong tất cả files.

**Bước 4: Update Imports**

Cập nhật imports trong Controller và Service.

---

## 4. ⏳ Tách Frontend Pipeline

### 4.1. Tạo Frontend Pipeline Riêng

**File: `.github/workflows/build-frontend.yml`**

```yaml
name: Build and Deploy Frontend

on:
  push:
    branches: [ main, develop ]
    paths:
      - 'frontend/**'
      - '.github/workflows/build-frontend.yml'
  pull_request:
    branches: [ main ]
    paths:
      - 'frontend/**'

jobs:
  build-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      
      - name: Install dependencies
        working-directory: ./frontend
        run: npm ci
      
      - name: Run tests
        working-directory: ./frontend
        run: npm test -- --coverage
      
      - name: Build
        working-directory: ./frontend
        run: npm run build
        env:
          REACT_APP_API_URL: ${{ secrets.API_URL }}
      
      - name: Upload build artifacts
        uses: actions/upload-artifact@v4
        with:
          name: frontend-build
          path: frontend/build
          retention-days: 7
      
      - name: Deploy to S3/CDN
        if: github.event_name == 'push' && github.ref == 'refs/heads/main'
        run: |
          aws s3 sync frontend/build s3://${{ secrets.S3_BUCKET }}/frontend
          aws cloudfront create-invalidation \
            --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID }} \
            --paths "/*"
        env:
          AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
```

### 4.2. Tách Frontend Code

**Cấu trúc:**

```
frontend/
├── package.json
├── package-lock.json
├── src/
│   ├── components/
│   ├── pages/
│   ├── services/
│   └── App.js
├── public/
└── build/
```

**package.json:**

```json
{
  "name": "bookvault-frontend",
  "version": "1.0.0",
  "scripts": {
    "start": "react-scripts start",
    "build": "react-scripts build",
    "test": "react-scripts test",
    "eject": "react-scripts eject"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "axios": "^1.6.0"
  }
}
```

---

## 5. 📝 Checklist Hoàn Thiện

### ✅ Tests
- [x] Unit tests cho Catalog Service
- [ ] Unit tests cho IAM Service
- [ ] Unit tests cho Borrowing Service
- [ ] Unit tests cho Gateway Service
- [ ] Integration tests cho tất cả services

### 🔄 Event-Driven
- [ ] Kafka producer trong Catalog Service
- [ ] Kafka producer trong Borrowing Service
- [ ] Kafka consumer trong Search Service
- [ ] Test Kafka events

### 🔄 Layer Structure
- [ ] Refactor Catalog Service
- [ ] Refactor IAM Service
- [ ] Refactor Borrowing Service
- [ ] Tạo DTOs cho tất cả services

### ⏳ Frontend
- [ ] Tạo frontend pipeline
- [ ] Tách frontend code
- [ ] Setup CI/CD cho frontend

---

## 6. 🚀 Hướng Dẫn Chạy

### 6.1. Chạy Tests

```bash
# Chạy tất cả tests
cd services/catalog
mvn test

# Chạy với coverage
mvn test jacoco:report
```

### 6.2. Test Kafka Events

```bash
# Start services
docker compose up -d kafka zookeeper

# Create a book (should publish event)
curl -X POST http://localhost:8080/api/catalog/v1/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","author":"Author","isbn":"123","quantity":10}'

# Check Kafka topic
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic book.created \
  --from-beginning
```

### 6.3. Verify Layer Structure

```bash
# Check package structure
find services/catalog/src/main/java -type f -name "*.java" | sort
```

---

## 7. 📚 Tài Liệu Tham Khảo

- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [Testcontainers](https://www.testcontainers.org/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [GitHub Actions](https://docs.github.com/en/actions)

---

*Document được cập nhật liên tục - Cập nhật: 2024*

