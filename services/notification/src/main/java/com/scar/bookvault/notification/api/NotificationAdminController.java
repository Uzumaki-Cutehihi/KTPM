package com.scar.bookvault.notification.api;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification/v1/notifications")
public class NotificationAdminController {
    private final MongoTemplate mongoTemplate;

    public NotificationAdminController(MongoTemplate mongoTemplate) { this.mongoTemplate = mongoTemplate; }

    @GetMapping
    public List<Map> list() { return mongoTemplate.findAll(Map.class, "notifications"); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map create(@RequestBody Map doc) { return mongoTemplate.save(doc, "notifications"); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        Map doc = mongoTemplate.findById(id, Map.class, "notifications");
        if (doc != null) mongoTemplate.remove(doc, "notifications");
    }
}

