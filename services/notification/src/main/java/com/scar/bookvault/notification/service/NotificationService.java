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
            
            emailService.sendEmail(
                email,
                "Loan Created Successfully",
                "Your loan has been created successfully. Please return the books by the due date."
            );
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
            
            emailService.sendEmail(
                email,
                "Loan Overdue Reminder",
                "Your loan is overdue. Please return the books as soon as possible to avoid fines."
            );
        } catch (Exception e) {
            System.err.println("Error processing loan.overdue event: " + e.getMessage());
        }
    }

    public void sendEmailNotification(String to, String subject, String body) {
        emailService.sendEmail(to, subject, body);
    }
}

