package com.scar.bookvault.catalog.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scar.bookvault.catalog.book.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class BookEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(BookEventPublisher.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public BookEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    public void publishBookCreated(Book book) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "book.created");
            event.put("bookId", book.getId());
            event.put("title", book.getTitle());
            event.put("author", book.getAuthor());
            event.put("isbn", book.getIsbn());
            event.put("quantity", book.getQuantity());
            event.put("timestamp", LocalDateTime.now().toString());
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("book.created", book.getId().toString(), eventJson);
            
            logger.info("Published book.created event for book ID: {}", book.getId());
            
        } catch (Exception e) {
            logger.error("Failed to publish book.created event for book ID: {}", book.getId(), e);
            // Don't throw exception - we don't want to fail the main operation
        }
    }
    
    public void publishBookUpdated(Book book) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "book.updated");
            event.put("bookId", book.getId());
            event.put("title", book.getTitle());
            event.put("author", book.getAuthor());
            event.put("isbn", book.getIsbn());
            event.put("quantity", book.getQuantity());
            event.put("timestamp", LocalDateTime.now().toString());
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("book.updated", book.getId().toString(), eventJson);
            
            logger.info("Published book.updated event for book ID: {}", book.getId());
            
        } catch (Exception e) {
            logger.error("Failed to publish book.updated event for book ID: {}", book.getId(), e);
        }
    }
    
    public void publishBookDeleted(Long bookId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "book.deleted");
            event.put("bookId", bookId);
            event.put("timestamp", LocalDateTime.now().toString());
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("book.deleted", bookId.toString(), eventJson);
            
            logger.info("Published book.deleted event for book ID: {}", bookId);
            
        } catch (Exception e) {
            logger.error("Failed to publish book.deleted event for book ID: {}", bookId, e);
        }
    }
}