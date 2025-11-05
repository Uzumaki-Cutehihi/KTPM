package com.scar.bookvault.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final RestTemplate restTemplate;

    public AdminService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get total books from Catalog Service
            List<?> books = restTemplate.getForObject(
                "http://catalog:8081/api/catalog/v1/books",
                List.class
            );
            stats.put("totalBooks", books != null ? books.size() : 0);
        } catch (Exception e) {
            stats.put("totalBooks", 0);
        }

        try {
            // Get total loans from Borrowing Service
            List<?> loans = restTemplate.getForObject(
                "http://borrowing:8083/api/borrowing/v1/loans",
                List.class
            );
            stats.put("totalLoans", loans != null ? loans.size() : 0);
        } catch (Exception e) {
            stats.put("totalLoans", 0);
        }

        try {
            // Get active loans (borrowed, not returned)
            List<?> activeLoans = restTemplate.getForObject(
                "http://borrowing:8083/api/borrowing/v1/loans?status=BORROWED",
                List.class
            );
            stats.put("activeLoans", activeLoans != null ? activeLoans.size() : 0);
        } catch (Exception e) {
            stats.put("activeLoans", 0);
        }

        // Note: User count would come from IAM service, but we don't have that endpoint yet
        stats.put("totalUsers", "N/A");

        return stats;
    }

    public List<?> getAllLoans() {
        try {
            return restTemplate.getForObject(
                "http://borrowing:8083/api/borrowing/v1/loans",
                List.class
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<?> getAllBooks() {
        try {
            return restTemplate.getForObject(
                "http://catalog:8081/api/catalog/v1/books",
                List.class
            );
        } catch (Exception e) {
            return List.of();
        }
    }
}

