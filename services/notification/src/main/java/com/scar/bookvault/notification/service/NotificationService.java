package com.scar.bookvault.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationService {
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public NotificationService(EmailService emailService, ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "loan.created", groupId = "notification-service")
    public void handleLoanCreated(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long userId = Long.parseLong(event.get("userId").toString());
            String email = (String) event.getOrDefault("email", "user@example.com");
            String bookTitle = (String) event.getOrDefault("bookTitle", "Unknown Book");
            String dueDate = (String) event.getOrDefault("dueDate", "Unknown");
            
            String subject = "Loan Created Successfully - BookVault";
            String body = String.format(
                "Dear User,\n\n" +
                "Your loan has been created successfully!\n\n" +
                "Book: %s\n" +
                "Due Date: %s\n\n" +
                "Please return the book by the due date to avoid any fines.\n\n" +
                "Thank you for using BookVault!",
                bookTitle, dueDate
            );
            
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            System.err.println("Error processing loan.created event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "loan.overdue", groupId = "notification-service")
    public void handleLoanOverdue(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long userId = Long.parseLong(event.get("userId").toString());
            String email = (String) event.getOrDefault("email", "user@example.com");
            String bookTitle = (String) event.getOrDefault("bookTitle", "Unknown Book");
            Integer overdueDays = Integer.parseInt(event.getOrDefault("overdueDays", "0").toString());
            
            String subject = "Loan Overdue Reminder - BookVault";
            String body = String.format(
                "Dear User,\n\n" +
                "Your loan is overdue by %d days!\n\n" +
                "Book: %s\n\n" +
                "Please return the book as soon as possible to avoid additional fines.\n\n" +
                "If you have already returned the book, please ignore this message.\n\n" +
                "Thank you for using BookVault!",
                overdueDays, bookTitle
            );
            
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            System.err.println("Error processing loan.overdue event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "loan.returned", groupId = "notification-service")
    public void handleLoanReturned(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long userId = Long.parseLong(event.get("userId").toString());
            String email = (String) event.getOrDefault("email", "user@example.com");
            String bookTitle = (String) event.getOrDefault("bookTitle", "Unknown Book");
            
            String subject = "Book Returned Successfully - BookVault";
            String body = String.format(
                "Dear User,\n\n" +
                "Thank you for returning the book!\n\n" +
                "Book: %s\n\n" +
                "Your loan has been completed successfully. We hope you enjoyed reading the book.\n\n" +
                "Feel free to borrow more books from our library.\n\n" +
                "Thank you for using BookVault!",
                bookTitle
            );
            
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            System.err.println("Error processing loan.returned event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "loan.due.reminder", groupId = "notification-service")
    public void handleLoanDueReminder(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long userId = Long.parseLong(event.get("userId").toString());
            String email = (String) event.getOrDefault("email", "user@example.com");
            String bookTitle = (String) event.getOrDefault("bookTitle", "Unknown Book");
            String dueDate = (String) event.getOrDefault("dueDate", "Unknown");
            Integer daysUntilDue = Integer.parseInt(event.getOrDefault("daysUntilDue", "0").toString());
            
            String subject = "Loan Due Reminder - BookVault";
            String body = String.format(
                "Dear User,\n\n" +
                "This is a reminder that your loan is due in %d days.\n\n" +
                "Book: %s\n" +
                "Due Date: %s\n\n" +
                "Please make sure to return the book on time to avoid any fines.\n\n" +
                "If you need more time, please contact the library to extend your loan.\n\n" +
                "Thank you for using BookVault!",
                daysUntilDue, bookTitle, dueDate
            );
            
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            System.err.println("Error processing loan.due.reminder event: " + e.getMessage());
        }
    }

    public void sendEmailNotification(String to, String subject, String body) {
        emailService.sendEmail(to, subject, body);
    }
}