package com.scar.bookvault.search.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scar.bookvault.search.domain.BookDocument;
import com.scar.bookvault.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BookEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(BookEventConsumer.class);
    
    private final SearchService searchService;
    private final ObjectMapper objectMapper;
    
    public BookEventConsumer(SearchService searchService, ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "book.created", groupId = "search-service")
    public void handleBookCreated(String message) {
        try {
            logger.info("Received book.created event: {}", message);
            
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            
            BookDocument book = new BookDocument();
            book.setId(Long.parseLong(event.get("bookId").toString()));
            book.setTitle((String) event.get("title"));
            book.setAuthor((String) event.get("author"));
            book.setIsbn((String) event.get("isbn"));
            book.setQuantity((Integer) event.get("quantity"));
            
            // Optional fields
            if (event.containsKey("description")) {
                book.setDescription((String) event.get("description"));
            }
            if (event.containsKey("category")) {
                book.setCategory((String) event.get("category"));
            }
            
            searchService.indexBook(book);
            logger.info("Successfully indexed book with ID: {}", book.getId());
            
        } catch (Exception e) {
            logger.error("Failed to process book.created event: {}", message, e);
        }
    }
    
    @KafkaListener(topics = "book.updated", groupId = "search-service")
    public void handleBookUpdated(String message) {
        try {
            logger.info("Received book.updated event: {}", message);
            
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            
            Long bookId = Long.parseLong(event.get("bookId").toString());
            
            // Check if book exists in index
            searchService.searchBooks(bookId.toString()).stream()
                    .filter(book -> book.getId().equals(bookId))
                    .findFirst()
                    .ifPresent(existingBook -> {
                        // Update fields if provided
                        if (event.containsKey("title")) {
                            existingBook.setTitle((String) event.get("title"));
                        }
                        if (event.containsKey("author")) {
                            existingBook.setAuthor((String) event.get("author"));
                        }
                        if (event.containsKey("quantity")) {
                            existingBook.setQuantity((Integer) event.get("quantity"));
                        }
                        if (event.containsKey("description")) {
                            existingBook.setDescription((String) event.get("description"));
                        }
                        if (event.containsKey("category")) {
                            existingBook.setCategory((String) event.get("category"));
                        }
                        
                        searchService.indexBook(existingBook);
                        logger.info("Successfully updated book with ID: {}", bookId);
                    });
            
        } catch (Exception e) {
            logger.error("Failed to process book.updated event: {}", message, e);
        }
    }
    
    @KafkaListener(topics = "book.deleted", groupId = "search-service")
    public void handleBookDeleted(String message) {
        try {
            logger.info("Received book.deleted event: {}", message);
            
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long bookId = Long.parseLong(event.get("bookId").toString());
            
            searchService.deleteBookFromIndex(bookId);
            logger.info("Successfully deleted book with ID: {} from search index", bookId);
            
        } catch (Exception e) {
            logger.error("Failed to process book.deleted event: {}", message, e);
        }
    }
}