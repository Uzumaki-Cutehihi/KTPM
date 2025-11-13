package com.scar.bookvault.borrowing.api;

import com.scar.bookvault.borrowing.domain.Loan;
import com.scar.bookvault.borrowing.domain.LoanStatus;
import com.scar.bookvault.borrowing.dto.CreateLoanRequest;
import com.scar.bookvault.borrowing.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrowing/v1")
public class LoanController {
    
    private final LoanService loanService;
    
    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }
    
    // Tạo loan mới (mượn sách)
    @PostMapping("/loans")
    public ResponseEntity<Loan> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        try {
            Loan loan = loanService.createLoan(request.getUserId(), request.getBookId(), request.getQuantity());
            return ResponseEntity.status(HttpStatus.CREATED).body(loan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Trả sách
    @PostMapping("/loans/{id}/return")
    public ResponseEntity<Void> returnLoan(@PathVariable Long id) {
        try {
            loanService.returnLoan(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Lấy danh sách loans của user
    @GetMapping("/users/{userId}/loans")
    public ResponseEntity<List<Loan>> getUserLoans(@PathVariable Long userId) {
        List<Loan> loans = loanService.getUserLoans(userId);
        return ResponseEntity.ok(loans);
    }
    
    // Lấy danh sách active loans của user
    @GetMapping("/users/{userId}/loans/active")
    public ResponseEntity<List<Loan>> getUserActiveLoans(@PathVariable Long userId) {
        List<Loan> loans = loanService.getUserActiveLoans(userId);
        return ResponseEntity.ok(loans);
    }
    
    // Lấy danh sách loans của book
    @GetMapping("/books/{bookId}/loans")
    public ResponseEntity<List<Loan>> getBookLoans(@PathVariable Long bookId) {
        List<Loan> loans = loanService.getBookLoans(bookId);
        return ResponseEntity.ok(loans);
    }
    
    // Lấy danh sách overdue loans
    @GetMapping("/loans/overdue")
    public ResponseEntity<List<Loan>> getOverdueLoans() {
        List<Loan> loans = loanService.getOverdueLoans();
        return ResponseEntity.ok(loans);
    }
    
    // Lấy số lượng sách đang được mượn của một book
    @GetMapping("/books/{bookId}/borrowed-count")
    public ResponseEntity<Map<String, Integer>> getBorrowedBookCount(@PathVariable Long bookId) {
        return loanService.getBorrowedBookCount(bookId)
                .map(count -> ResponseEntity.ok(Map.of("borrowedCount", count)))
                .orElse(ResponseEntity.ok(Map.of("borrowedCount", 0)));
    }
    
    // Lấy thông tin một loan cụ thể
    @GetMapping("/loans/{id}")
    public ResponseEntity<Loan> getLoan(@PathVariable Long id) {
        return loanService.getUserLoans(null).stream()
                .filter(loan -> loan.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Thống kê
    @GetMapping("/loans/stats")
    public ResponseEntity<Map<String, Object>> getLoanStats() {
        // This would require additional service methods
        // For now, return basic structure
        Map<String, Object> stats = Map.of(
                "totalLoans", 0,
                "activeLoans", 0,
                "overdueLoans", 0,
                "returnedLoans", 0
        );
        
        return ResponseEntity.ok(stats);
    }
}