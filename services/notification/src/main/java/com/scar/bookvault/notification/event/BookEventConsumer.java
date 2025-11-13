package com.scar.bookvault.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scar.bookvault.notification.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BookEventConsumer {
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public BookEventConsumer(EmailService emailService, ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "book.created", groupId = "notification-service")
    public void handleBookCreated(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String title = (String) event.get("title");
            String author = (String) event.get("author");
            
            String subject = "New Book Added to Library";
            String body = String.format(
                "A new book has been added to our library:\n\n" +
                "Title: %s\n" +
                "Author: %s\n\n" +
                "Visit our library to check it out!",
                title, author
            );
            
            // Send notification to admin or broadcast to all users
            emailService.sendEmail("admin@bookvault.com", subject, body);
        } catch (Exception e) {
            System.err.println("Error processing book.created event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "book.updated", groupId = "notification-service")
    public void handleBookUpdated(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String title = (String) event.get("title");
            String author = (String) event.get("author");
            
            String subject = "Book Information Updated";
            String body = String.format(
                "Book information has been updated:\n\n" +
                "Title: %s\n" +
                "Author: %s\n\n" +
                "Please check the updated information in our library.",
                title, author
            );
            
            emailService.sendEmail("admin@bookvault.com", subject, body);
        } catch (Exception e) {
            System.err.println("Error processing book.updated event: " + e.getMessage());
        }
    }
}