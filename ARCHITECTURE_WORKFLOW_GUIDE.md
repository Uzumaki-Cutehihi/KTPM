# 📋 Hướng Dẫn Chi Tiết Cách Hoạt Động Các Kiến Trúc BookVault

## 🎯 Mục Tiêu
Tài liệu này mô tả chi tiết cách hoạt động của từng loại kiến trúc trong dự án BookVault, giúp developer hiểu rõ flow và implement đúng cách.

---

## 🏗️ 1. KIẾN TRÚC MICROSERVICES

### **A. Tổng Quan Flow Hoạt Động**

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENT (Browser/Mobile)                     │
└─────────────────────┬─────────────────────────────────────────┘
                      │ HTTP Request
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              API GATEWAY (Port 8080)                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ • JWT Validation                                       │   │
│  │ • Route Mapping: /api/{service}/** → {service}:{port} │   │
│  │ • Load Balancing (round-robin)                        │   │
│  │ • Rate Limiting (configurable)                        │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────┬─────────────────────────────────────────┘
                      │ Routed Request
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                  MICROSERVICES LAYER                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │Catalog   │ │Borrowing │ │Search    │ │IAM       │       │
│  │(8081)    │ │(8083)    │ │(8084)    │ │(8082)    │       │
│  └─────┬────┘ └─────┬────┘ └─────┬────┘ └─────┬────┘       │
│        │            │            │            │              │
│        ▼            ▼            ▼            ▼              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │catalog_db│ │borrowing_│ │elasticsea│ │iam_db    │       │
│  │(5433)    │ │db(5435)  │ │rch(9200) │ │(5434)    │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

### **B. Chi Tiết Flow Request**

#### **Step 1: Client → Gateway**
```http
POST http://localhost:8080/api/catalog/v1/books
Headers:
  Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
  Content-Type: application/json

Body:
{
  "title": "Spring Boot Microservices",
  "author": "John Doe",
  "isbn": "978-1234567890",
  "quantity": 10
}
```

#### **Step 2: Gateway Processing**
```java
// GatewaySecurityConfig.java
public class GatewaySecurityConfig {
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("catalog", r -> r.path("/api/catalog/**")
                .filters(f -> f.stripPrefix(2))
                .uri("http://catalog:8081"))
            .build();
    }
}
```

#### **Step 3: Service Layer Processing**
```java
// BookController.java
@RestController
@RequestMapping("/api/catalog/v1/books")
public class BookController {
    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        // 1. Validation Layer
        validateBook(book);
        
        // 2. Business Logic Layer
        Book saved = bookService.create(book);
        
        // 3. Event Publishing (async)
        eventPublisher.publishBookCreated(saved);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
```

---

## 🔗 2. KIẾN TRÚC EVENT-DRIVEN (KAFKA)

### **A. Event Flow Chi Tiết**

```
┌─────────────────────────────────────────────────────────────────┐
│                    EVENT-DRIVEN ARCHITECTURE                   │
├─────────────────────────────────────────────────────────────────┤
│  PUBLISHER → KAFKA → CONSUMER → ACTION                        │
├─────────────────────────────────────────────────────────────────┤
│  [CATALOG SERVICE]                                             │
│  publishBookCreated()                                         │
│        │                                                        │
│        ▼                                                        │
│  KafkaTemplate.send("book.created", event)                    │
│        │                                                        │
│        ▼                                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              KAFKA BROKER (29092)                      │   │
│  │  Topics:                                                  │   │
│  │  • book.created                                          │   │
│  │  • book.updated                                          │   │
│  │  • book.deleted                                          │   │
│  │  • loan.created                                          │   │
│  │  • loan.returned                                         │   │
│  │  • loan.overdue                                          │   │
│  └─────────────────────┬────────────────────────────────┘   │
│        │                                                        │
│        ▼                                                        │
│  [SEARCH SERVICE]                    [NOTIFICATION SERVICE]   │
│  @KafkaListener                       @KafkaListener           │
│  handleBookCreated()                 handleBookCreated()      │
│        │                                      │                │
│        ▼                                      ▼                │
│  Index to Elasticsearch              Send Email               │
│        │                                      │                │
│        ▼                                      ▼                │
│  books index                         User Email               │
└─────────────────────────────────────────────────────────────────┘
```

### **B. Code Implementation Event Publishing**

#### **Event Publisher (Catalog Service)**
```java
// BookEventPublisher.java
@Service
public class BookEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public void publishBookCreated(Book book) {
        try {
            // 1. Create Event Data
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "book.created");
            event.put("bookId", book.getId());
            event.put("title", book.getTitle());
            event.put("author", book.getAuthor());
            event.put("isbn", book.getIsbn());
            event.put("quantity", book.getQuantity());
            event.put("timestamp", LocalDateTime.now().toString());
            
            // 2. Serialize to JSON
            String eventJson = objectMapper.writeValueAsString(event);
            
            // 3. Send to Kafka
            kafkaTemplate.send("book.created", book.getId().toString(), eventJson);
            
            log.info("Published book.created event for book ID: {}", book.getId());
            
        } catch (Exception e) {
            log.error("Failed to publish book.created event", e);
            // Don't throw - we don't want to fail the main operation
        }
    }
}
```

#### **Event Consumer (Search Service)**
```java
// BookEventConsumer.java
@Component
public class BookEventConsumer {
    
    @KafkaListener(topics = "book.created", groupId = "search-service")
    public void handleBookCreated(String message) {
        try {
            // 1. Parse Event
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            
            // 2. Extract Data
            Long bookId = Long.parseLong(event.get("bookId").toString());
            String title = (String) event.get("title");
            String author = (String) event.get("author");
            
            // 3. Create Document
            BookDocument document = new BookDocument();
            document.setId(bookId);
            document.setTitle(title);
            document.setAuthor(author);
            
            // 4. Index to Elasticsearch
            searchService.indexBook(document);
            
            log.info("Indexed book {} to Elasticsearch", bookId);
            
        } catch (Exception e) {
            log.error("Failed to process book.created event", e);
        }
    }
}
```

### **C. Event Flow Cho Borrowing Service**

```java
// LoanService.java - Saga Pattern Implementation
@Transactional
public Loan createLoan(Long userId, Long bookId, Integer quantity) {
    // 1. Check Book Availability (Synchronous)
    boolean available = checkBookAvailability(bookId, quantity);
    if (!available) {
        throw new IllegalArgumentException("Book not available");
    }
    
    // 2. Create Loan Record
    Loan loan = new Loan(userId, bookId, quantity);
    Loan saved = loanRepository.save(loan);
    
    // 3. Update Book Quantity (Compensating Transaction)
    updateBookQuantity(bookId, -quantity);
    
    // 4. Publish Event (Asynchronous)
    publishLoanCreated(saved);
    
    return saved;
}

private void updateBookQuantity(Long bookId, Integer quantityChange) {
    try {
        // Call Catalog Service via WebClient
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("quantityChange", quantityChange);
        
        webClient.put()
            .uri("http://catalog:8081/api/catalog/v1/books/{id}/quantity", bookId)
            .bodyValue(updateRequest)
            .retrieve()
            .toBodilessEntity()
            .block();
            
    } catch (Exception e) {
        log.error("Failed to update book quantity", e);
        // Don't throw - this is a side effect
    }
}
```

---

## 🔍 3. KIẾN TRÚC ELASTICSEARCH & SEARCH

### **A. Search Architecture Flow**

```
┌─────────────────────────────────────────────────────────────────┐
│                    SEARCH ARCHITECTURE                         │
├─────────────────────────────────────────────────────────────────┤
│  [USER QUERY] → [SEARCH SERVICE] → [ELASTICSEARCH] → [RESULT]│
├─────────────────────────────────────────────────────────────────┤
│  Step 1: Query Processing                                      │
│  GET /api/search/v1/books/search?query=spring                 │
│        │                                                        │
│        ▼                                                        │
│  SearchController.search()                                    │
│        │                                                        │
│        ▼                                                        │
│  SearchService.searchByQuery("spring")                        │
│        │                                                        │
│        ▼                                                        │
│  BookSearchRepository.searchByQuery("spring")                 │
│        │                                                        │
│        ▼                                                        │
│  Elasticsearch Query:                                         │
│  {                                                            │
│    "multi_match": {                                           │
│      "query": "spring",                                      │
│      "fields": ["title^2", "author", "description"]         │
│    }                                                          │
│  }                                                            │
│        │                                                        │
│        ▼                                                        │
│  Return: List<BookDocument>                                   │
└─────────────────────────────────────────────────────────────────┘
```

### **B. Elasticsearch Document Mapping**

```java
// BookDocument.java
@Document(indexName = "books")
public class BookDocument {
    
    @Id
    private Long id;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String author;
    
    @Field(type = FieldType.Text)
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String isbn;
    
    @Field(type = FieldType.Integer)
    private Integer quantity;
    
    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
}
```

### **C. Advanced Search Implementation**

```java
// BookSearchRepository.java
public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, Long> {
    
    // Multi-field search with boosting
    @Query("{\n" +
           "  \"multi_match\": {\n" +
           "    \"query\": \"?0\",\n" +
           "    \"fields\": [\"title^2\", \"author\", \"description\"]\n" +
           "  }\n" +
           "}")
    List<BookDocument> searchByQuery(String query);
    
    // Fuzzy search for typos
    @Query("{\n" +
           "  \"multi_match\": {\n" +
           "    \"query\": \"?0\",\n" +
           "    \"fields\": [\"title\", \"author\"],\n" +
           "    \"fuzziness\": \"AUTO\"\n" +
           "  }\n" +
           "}")
    List<BookDocument> fuzzySearch(String query);
    
    // Filtered search
    List<BookDocument> findByTitleContainingAndAuthorContaining(
        String title, String author);
}
```

---

## 🔐 4. KIẾN TRÚC SECURITY & JWT

### **A. JWT Authentication Flow**

```
┌─────────────────────────────────────────────────────────────────┐
│                    JWT AUTHENTICATION FLOW                     │
├─────────────────────────────────────────────────────────────────┤
│  [CLIENT] → [GATEWAY] → [IAM SERVICE] → [JWT TOKEN]         │
├─────────────────────────────────────────────────────────────────┤
│  Step 1: Login Request                                         │
│  POST /api/iam/v1/auth/login                                  │
│        │                                                        │
│        ▼                                                        │
│  AuthController.login()                                       │
│        │                                                        │
│        ▼                                                        │
│  Validate username/password                                   │
│        │                                                        │
│        ▼                                                        │
│  Generate JWT Token (RS256)                                   │
│        │                                                        │
│        ▼                                                        │
│  Return: { "token": "eyJhbGc..." }                           │
│        │                                                        │
│        ▼                                                        │
│  Store token in client                                        │
│  Include in subsequent requests                               │
└─────────────────────────────────────────────────────────────────┘
```

### **B. JWT Token Structure**

```java
// JwtService.java
@Component
public class JwtService {
    
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("roles", user.getRoles());
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

### **C. Gateway Security Configuration**

```java
// GatewaySecurityConfig.java
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {
    
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            JwtAuthenticationFilter jwtAuthFilter) {
        
        return http
            .csrf().disable()
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/iam/v1/auth/**").permitAll()
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/api/**").authenticated()
                .anyExchange().permitAll()
            )
            .addFilterAt(jwtAuthFilter, SecurityWebFilterChain.class)
            .build();
    }
}
```

---

## 💾 5. KIẾN TRÚC DATABASE-PER-SERVICE

### **A. Database Isolation Pattern**

```
┌─────────────────────────────────────────────────────────────────┐
│              DATABASE-PER-SERVICE ARCHITECTURE                 │
├─────────────────────────────────────────────────────────────────┤
│  [CATALOG SERVICE] → [catalog_postgres:5433]                 │
│        │                                                        │
│        ├─ Book Table                                            │
│        │   ├── id (PK)                                         │
│        │   ├── title                                           │
│        │   ├── author                                          │
│        │   ├── isbn (unique)                                   │
│        │   ├── quantity                                        │
│        │   ├── created_at                                      │
│        │   └── updated_at                                      │
│        │                                                        │
│        └─ Indexes: idx_isbn, idx_author                       │
│                                                               │
│  [BORROWING SERVICE] → [borrowing_postgres:5435]               │
│        │                                                        │
│        ├─ Loan Table                                           │
│        │   ├── id (PK)                                         │
│        │   ├── user_id                                         │
│        │   ├── book_id (FK logical)                           │
│        │   ├── quantity                                        │
│        │   ├── status                                          │
│        │   ├── due_at                                          │
│        │   ├── returned_at                                     │
│        │   └── fine_amount                                     │
│        │                                                        │
│        └─ Indexes: idx_user_id, idx_book_id, idx_status      │
└─────────────────────────────────────────────────────────────────┘
```

### **B. Data Access Layer Implementation**

```java
// BookRepository.java - Catalog Service
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    // Business queries
    boolean existsByIsbn(String isbn);
    
    List<Book> findByAuthorContaining(String author);
    
    @Query("SELECT b FROM Book b WHERE b.quantity > 0")
    List<Book> findAvailableBooks();
    
    @Query("UPDATE Book b SET b.quantity = b.quantity + ?2 WHERE b.id = ?1")
    @Modifying
    void updateQuantity(Long bookId, Integer quantityChange);
}
```

### **C. Cross-Service Data Consistency**

```java
// LoanService.java - Maintaining consistency with Catalog
@Service
public class LoanService {
    
    public boolean checkBookAvailability(Long bookId, Integer requestedQuantity) {
        try {
            // Call Catalog Service via WebClient
            Map<String, Object> bookInfo = webClient.get()
                .uri("http://catalog:8081/api/catalog/v1/books/{id}", bookId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            Integer availableQuantity = (Integer) bookInfo.get("quantity");
            return availableQuantity != null && availableQuantity >= requestedQuantity;
            
        } catch (Exception e) {
            log.error("Failed to check book availability", e);
            return false;
        }
    }
}
```

---

## 🔄 6. KIẾN TRÚC SAGA PATTERN (Distributed Transactions)

### **A. Book Borrowing Saga Flow**

```
┌─────────────────────────────────────────────────────────────────┐
│                    BORROW BOOK SAGA                            │
├─────────────────────────────────────────────────────────────────┤
│  Step 1: Validate and Create Loan                             │
│  ───────────────────────────────────────────────────────────── │
│  [Borrowing Service]                                           │
│  1. Validate user exists (check user service)                │
│  2. Check if user has active loan for this book               │
│  3. Create loan record with PENDING status                    │
│  4. Publish loan.created event                               │
│        │                                                        │
│        ▼                                                        │
│  Step 2: Update Book Quantity (Compensating)                 │
│  ───────────────────────────────────────────────────────────── │
│  [Borrowing Service → Catalog Service]                        │
│  1. Call Catalog API: PUT /books/{id}/quantity               │
│  2. Request body: {"quantityChange": -2}                    │
│  3. If success: Continue                                      │
│  4. If fail: Rollback loan record                           │
│        │                                                        │
│        ▼                                                        │
│  Step 3: Confirm Loan (Finalization)                         │
│  ───────────────────────────────────────────────────────────── │
│  [Borrowing Service]                                           │
│  1. Update loan status to ACTIVE                             │
│  2. Set due date (14 days from now)                          │
│  3. Publish loan.confirmed event                             │
│        │                                                        │
│        ▼                                                        │
│  Step 4: Side Effects (Async)                                │
│  ───────────────────────────────────────────────────────────── │
│  [Notification Service] (via Kafka)                        │
│  1. Receive loan.created event                               │
│  2. Send confirmation email to user                          │
│  3. Send notification to admin                              │
│                                                               │
│  [Search Service] (via Kafka)                                │
│  1. Receive book.updated event (quantity change)             │
│  2. Update Elasticsearch index                              │
└─────────────────────────────────────────────────────────────────┘
```

### **B. Saga Compensation Logic**

```java
// LoanService.java - Compensation handling
@Transactional
public Loan createLoan(Long userId, Long bookId, Integer quantity) {
    try {
        // Step 1: Check availability
        boolean available = checkBookAvailability(bookId, quantity);
        if (!available) {
            throw new IllegalArgumentException("Book not available");
        }
        
        // Step 2: Create loan (local transaction)
        Loan loan = new Loan(userId, bookId, quantity);
        Loan saved = loanRepository.save(loan);
        
        // Step 3: Update book quantity (external service call)
        try {
            updateBookQuantity(bookId, -quantity);
        } catch (Exception e) {
            // Compensation: Rollback loan creation
            loanRepository.delete(saved);
            throw new RuntimeException("Failed to update book quantity", e);
        }
        
        // Step 4: Publish events (async, won't affect main transaction)
        publishLoanCreated(saved);
        
        return saved;
        
    } catch (Exception e) {
        log.error("Loan creation failed, rolling back", e);
        throw e;
    }
}
```

---

## 🚀 7. KIẾN TRÚC DEPLOYMENT & DOCKER

### **A. Container Orchestration Flow**

```
┌─────────────────────────────────────────────────────────────────┐
│                    DOCKER COMPOSE ORCHESTRATION                │
├─────────────────────────────────────────────────────────────────┤
│  docker-compose up -d                                         │
│        │                                                        │
│        ▼                                                        │
│  [HEALTH CHECK CHAIN]                                         │
│  1. PostgreSQL databases (healthcheck)                      │
│  2. Elasticsearch (port 9200)                                 │
│  3. Kafka + Zookeeper (ports 29092, 2181)                    │
│  4. MinIO (ports 9000, 9001)                                │
│        │                                                        │
│        ▼                                                        │
│  [SERVICE STARTUP ORDER]                                      │
│  1. IAM Service (depends on iam-postgres)                    │
│  2. Catalog Service (depends on catalog-postgres)             │
│  3. Borrowing Service (depends on borrowing-postgres + catalog)│
│  4. Search Service (depends on elasticsearch)                  │
│  5. Notification Service (depends on kafka)                   │
│  6. Media Service (depends on minio)                        │
│  7. Admin Service (depends on catalog + borrowing)           │
│  8. Gateway Service (depends on all services)                │
└─────────────────────────────────────────────────────────────────┘
```

### **B. Service Configuration Pattern**

```yaml
# application.yml - Common pattern for all services
spring:
  application:
    name: catalog-service
  
  # Database configuration
  datasource:
    url: jdbc:postgresql://${DB_HOST:catalog-postgres}:${DB_PORT:5432}/${DB_NAME:catalog}
    username: ${DB_USER:catalog}
    password: ${DB_PASSWORD:catalog}
  
  # JPA configuration
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
  # Kafka configuration (for event-driven services)
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: ${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

# Service-specific configuration
server:
  port: 8081

# Health check
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### **C. Docker Build Process**

```dockerfile
# Multi-stage build pattern used in all services
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn -q -e -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 📊 8. KIẾN TRÚC MONITORING & HEALTH CHECKS

### **A. Health Check Implementation**

```java
// CustomHealthIndicator.java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check database connectivity
            Connection connection = dataSource.getConnection();
            connection.close();
            
            return Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("status", "Connected")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### **B. Service Health Endpoints**

```
GET http://localhost:8081/actuator/health

Response:
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 343497121792,
        "threshold": 10485760
      }
    },
    "kafka": {
      "status": "UP",
      "details": {
        "nodes": ["kafka:9092"]
      }
    }
  }
}
```

---

## 🎯 9. BEST PRACTICES & RECOMMENDATIONS

### **A. Error Handling Patterns**

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse(
            "BAD_REQUEST",
            e.getMessage(),
            HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        ErrorResponse error = new ErrorResponse(
            "NOT_FOUND",
            e.getMessage(),
            HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

### **B. Logging Strategy**

```java
// Structured logging with correlation IDs
@Service
public class BookService {
    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    
    @Transactional
    public Book createBook(Book book) {
        String correlationId = MDC.get("correlationId");
        
        log.info("[{}] Creating book: title={}, author={}, isbn={}", 
                 correlationId, book.getTitle(), book.getAuthor(), book.getIsbn());
        
        try {
            Book saved = bookRepository.save(book);
            log.info("[{}] Book created successfully: id={}", correlationId, saved.getId());
            
            // Publish event
            eventPublisher.publishBookCreated(saved);
            log.debug("[{}] Book creation event published", correlationId);
            
            return saved;
            
        } catch (Exception e) {
            log.error("[{}] Failed to create book: {}", correlationId, e.getMessage(), e);
            throw new ServiceException("Failed to create book", e);
        }
    }
}
```

### **C. Performance Optimization**

```java
// Caching implementation
@Service
public class BookService {
    
    @Cacheable(value = "books", key = "#id")
    public Book getBook(Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }
    
    @CacheEvict(value = "books", key = "#book.id")
    public Book updateBook(Book book) {
        // Update logic
        return bookRepository.save(book);
    }
    
    @Cacheable(value = "availableBooks")
    public List<Book> getAvailableBooks() {
        return bookRepository.findAvailableBooks();
    }
}
```

---

## 📚 10. TROUBLESHOOTING GUIDE

### **A. Common Issues & Solutions**

#### **1. Service Discovery Issues**
```bash
# Problem: Service cannot connect to dependencies
# Solution: Check Docker network and service names
docker network ls
docker network inspect bookvault_default
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Test connectivity
docker exec -it ktpm-catalog-1 ping borrowing:8083
```

#### **2. Kafka Connection Issues**
```bash
# Problem: Kafka consumer not connecting
# Solution: Check Kafka topics and consumers
docker exec -it ktpm-kafka-1 kafka-topics --list --bootstrap-server localhost:9092
docker exec -it ktpm-kafka-1 kafka-consumer-groups --list --bootstrap-server localhost:9092

# Check consumer lag
docker exec -it ktpm-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group search-service \
  --describe
```

#### **3. Elasticsearch Index Issues**
```bash
# Problem: Search not returning results
# Solution: Check Elasticsearch index and documents
curl -X GET "localhost:9200/_cat/indices?v"
curl -X GET "localhost:9200/books/_count"
curl -X GET "localhost:9200/books/_mapping"

# Reindex if necessary
curl -X DELETE "localhost:9200/books"
# Restart search service to trigger reindexing
```

#### **4. Database Migration Issues**
```bash
# Problem: Service fails to start due to schema mismatch
# Solution: Check Flyway migrations
docker logs ktpm-catalog-1 | grep -i "flyway"
docker exec -it ktpm-catalog-postgres-1 psql -U catalog -d catalog -c "\dt"

# Check migration history
docker exec -it ktpm-catalog-postgres-1 psql -U catalog -d catalog -c "SELECT * FROM flyway_schema_history"
```

---

## 📖 TÀI LIỆU THAM KHẢO BỔ SUNG

- [Spring Boot Microservices Best Practices](https://spring.io/microservices)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Elasticsearch Java API](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Docker Compose Best Practices](https://docs.docker.com/compose/)
- [Microservices.io - Architecture Patterns](https://microservices.io/patterns/)

---

*Last Updated: $(date)*
*Version: 1.0*