package com.scar.bookvault.borrowing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scar.bookvault.borrowing.domain.Loan;
import com.scar.bookvault.borrowing.domain.LoanRepository;
import com.scar.bookvault.borrowing.domain.LoanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LoanService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);
    
    private final LoanRepository loanRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    
    public LoanService(LoanRepository loanRepository, 
                      KafkaTemplate<String, String> kafkaTemplate,
                      ObjectMapper objectMapper,
                      WebClient.Builder webClientBuilder) {
        this.loanRepository = loanRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.build();
    }
    
    @Transactional
    public Loan createLoan(Long userId, Long bookId, Integer quantity) {
        logger.info("Creating loan for userId: {}, bookId: {}, quantity: {}", userId, bookId, quantity);
        
        // Validate quantity
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        // Check if book is available (call Catalog Service)
        boolean bookAvailable = checkBookAvailability(bookId, quantity);
        if (!bookAvailable) {
            throw new IllegalArgumentException("Book not available or insufficient quantity");
        }
        
        // Check if user already has an active loan for this book
        if (loanRepository.existsByUserIdAndBookIdAndStatus(userId, bookId, LoanStatus.ACTIVE)) {
            throw new IllegalArgumentException("User already has an active loan for this book");
        }
        
        // Create new loan
        Loan loan = new Loan(userId, bookId, quantity);
        Loan savedLoan = loanRepository.save(loan);
        
        // Publish loan.created event
        publishLoanCreated(savedLoan);
        
        // Update book quantity in Catalog Service
        updateBookQuantity(bookId, -quantity);
        
        logger.info("Successfully created loan with ID: {}", savedLoan.getId());
        return savedLoan;
    }
    
    @Transactional
    public void returnLoan(Long loanId) {
        logger.info("Returning loan with ID: {}", loanId);
        
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        
        if (loan.isReturned()) {
            throw new IllegalArgumentException("Loan has already been returned");
        }
        
        // Mark as returned and calculate fine if overdue
        loan.markAsReturned();
        loanRepository.save(loan);
        
        // Return book quantity to Catalog Service
        updateBookQuantity(loan.getBookId(), loan.getQuantity());
        
        // Publish events
        publishLoanReturned(loan);
        
        if (loan.getFineAmount() > 0) {
            publishOverdueFine(loan);
        }
        
        logger.info("Successfully returned loan with ID: {}", loanId);
    }
    
    public List<Loan> getUserLoans(Long userId) {
        return loanRepository.findByUserId(userId);
    }
    
    public List<Loan> getUserActiveLoans(Long userId) {
        return loanRepository.findByUserIdAndStatus(userId, LoanStatus.ACTIVE);
    }
    
    public List<Loan> getBookLoans(Long bookId) {
        return loanRepository.findByBookId(bookId);
    }
    
    public List<Loan> getOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDateTime.now());
    }
    
    public Optional<Integer> getBorrowedBookCount(Long bookId) {
        return loanRepository.countBorrowedBooksByBookId(bookId);
    }
    
    // Private methods
    
    private boolean checkBookAvailability(Long bookId, Integer requestedQuantity) {
        try {
            // Call Catalog Service to check book availability
            Map<String, Object> bookInfo = webClient.get()
                    .uri("http://catalog:8081/api/catalog/v1/books/{id}", bookId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (bookInfo == null) {
                return false;
            }
            
            Integer availableQuantity = (Integer) bookInfo.get("quantity");
            return availableQuantity != null && availableQuantity >= requestedQuantity;
            
        } catch (Exception e) {
            logger.error("Failed to check book availability for bookId: {}", bookId, e);
            return false;
        }
    }
    
    private void updateBookQuantity(Long bookId, Integer quantityChange) {
        try {
            // Call Catalog Service to update book quantity
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("quantityChange", quantityChange);
            
            webClient.put()
                    .uri("http://catalog:8081/api/catalog/v1/books/{id}/quantity", bookId)
                    .bodyValue(updateRequest)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            
            logger.info("Updated book quantity for bookId: {} by: {}", bookId, quantityChange);
            
        } catch (Exception e) {
            logger.error("Failed to update book quantity for bookId: {}", bookId, e);
            // Don't throw exception - this is a side effect
        }
    }
    
    private void publishLoanCreated(Loan loan) {
        try {
            // Get book information to include in the event
            Map<String, Object> bookInfo = getBookInfo(loan.getBookId());
            String bookTitle = (String) bookInfo.getOrDefault("title", "Unknown Book");
            
            // Get user email (for now using a placeholder, in real system would call user service)
            String userEmail = "user" + loan.getUserId() + "@bookvault.com";
            
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "loan.created");
            event.put("loanId", loan.getId());
            event.put("userId", loan.getUserId());
            event.put("email", userEmail);
            event.put("bookId", loan.getBookId());
            event.put("bookTitle", bookTitle);
            event.put("quantity", loan.getQuantity());
            event.put("dueDate", loan.getDueAt().toString());
            event.put("timestamp", LocalDateTime.now().toString());
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("loan.created", loan.getId().toString(), eventJson);
            
            logger.info("Published loan.created event for loan ID: {}", loan.getId());
            
        } catch (Exception e) {
            logger.error("Failed to publish loan.created event for loan ID: {}", loan.getId(), e);
        }
    }
    
    private void publishLoanReturned(Loan loan) {
        try {
            // Get book information to include in the event
            Map<String, Object> bookInfo = getBookInfo(loan.getBookId());
            String bookTitle = (String) bookInfo.getOrDefault("title", "Unknown Book");
            
            // Get user email (for now using a placeholder, in real system would call user service)
            String userEmail = "user" + loan.getUserId() + "@bookvault.com";
            
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "loan.returned");
            event.put("loanId", loan.getId());
            event.put("userId", loan.getUserId());
            event.put("email", userEmail);
            event.put("bookId", loan.getBookId());
            event.put("bookTitle", bookTitle);
            event.put("returnedAt", loan.getReturnedAt().toString());
            event.put("fineAmount", loan.getFineAmount());
            event.put("timestamp", LocalDateTime.now().toString());
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("loan.returned", loan.getId().toString(), eventJson);
            
            logger.info("Published loan.returned event for loan ID: {}", loan.getId());
            
        } catch (Exception e) {
            logger.error("Failed to publish loan.returned event for loan ID: {}", loan.getId(), e);
        }
    }
    
    private void publishOverdueFine(Loan loan) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "loan.overdue.fine");
            event.put("loanId", loan.getId());
            event.put("userId", loan.getUserId());
            event.put("fineAmount", loan.getFineAmount());
            event.put("daysOverdue", java.time.temporal.ChronoUnit.DAYS.between(loan.getDueAt(), loan.getReturnedAt()));
            event.put("timestamp", LocalDateTime.now().toString());
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("loan.overdue.fine", loan.getId().toString(), eventJson);
            
            logger.info("Published loan.overdue.fine event for loan ID: {}", loan.getId());
            
        } catch (Exception e) {
            logger.error("Failed to publish loan.overdue.fine event for loan ID: {}", loan.getId(), e);
        }
    }
    
    public void publishLoanOverdue(Loan loan, int overdueDays) {
        try {
            // Get book information to include in the event
            Map<String, Object> bookInfo = getBookInfo(loan.getBookId());
            String bookTitle = (String) bookInfo.getOrDefault("title", "Unknown Book");
            
            // Get user email (for now using a placeholder, in real system would call user service)
            String userEmail = "user" + loan.getUserId() + "@bookvault.com";
            
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "loan.overdue");
            event.put("loanId", loan.getId());
            event.put("userId", loan.getUserId());
            event.put("email", userEmail);
            event.put("bookId", loan.getBookId());
            event.put("bookTitle", bookTitle);
            event.put("overdueDays", overdueDays);
            event.put("timestamp", LocalDateTime.now().toString());
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("loan.overdue", loan.getId().toString(), eventJson);
            
            logger.info("Published loan.overdue event for loan ID: {} ({} days overdue)", loan.getId(), overdueDays);
            
        } catch (Exception e) {
            logger.error("Failed to publish loan.overdue event for loan ID: {}", loan.getId(), e);
        }
    }
    
    public void publishDueDateReminder(Loan loan) {
        try {
            // Get book information to include in the event
            Map<String, Object> bookInfo = getBookInfo(loan.getBookId());
            String bookTitle = (String) bookInfo.getOrDefault("title", "Unknown Book");
            
            // Get user email (for now using a placeholder, in real system would call user service)
            String userEmail = "user" + loan.getUserId() + "@bookvault.com";
            
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "loan.due.reminder");
            event.put("loanId", loan.getId());
            event.put("userId", loan.getUserId());
            event.put("email", userEmail);
            event.put("bookId", loan.getBookId());
            event.put("bookTitle", bookTitle);
            event.put("dueDate", loan.getDueAt().toString());
            event.put("daysUntilDue", java.time.Duration.between(LocalDateTime.now(), loan.getDueAt()).toDaysPart());
            event.put("timestamp", LocalDateTime.now().toString());
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("loan.due.reminder", loan.getId().toString(), eventJson);
            
            logger.info("Published loan.due.reminder event for loan ID: {}", loan.getId());
            
        } catch (Exception e) {
            logger.error("Failed to publish loan.due.reminder event for loan ID: {}", loan.getId(), e);
        }
    }
    
    private Map<String, Object> getBookInfo(Long bookId) {
        try {
            // Call Catalog Service to get book information
            Map<String, Object> bookInfo = webClient.get()
                    .uri("http://catalog:8081/api/catalog/v1/books/{id}", bookId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            return bookInfo != null ? bookInfo : new HashMap<>();
            
        } catch (Exception e) {
            logger.error("Failed to get book info for bookId: {}", bookId, e);
            return new HashMap<>();
        }
    }
}