package com.scar.bookvault.notification.api;

import com.scar.bookvault.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notification/v1")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> sendEmail(@RequestBody Map<String, String> payload) {
        String to = payload.get("to");
        String subject = payload.get("subject");
        String body = payload.get("body");
        
        notificationService.sendEmailNotification(to, subject, body);
        
        return Map.of("status", "sent", "to", to);
    }
}
