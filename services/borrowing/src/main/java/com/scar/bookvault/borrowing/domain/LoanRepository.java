package com.scar.bookvault.borrowing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    
    // Tìm loans theo user ID
    List<Loan> findByUserId(Long userId);
    
    // Tìm loans theo book ID
    List<Loan> findByBookId(Long bookId);
    
    // Tìm active loans theo user ID
    List<Loan> findByUserIdAndStatus(Long userId, LoanStatus status);
    
    // Tìm active loans theo book ID
    List<Loan> findByBookIdAndStatus(Long bookId, LoanStatus status);
    
    // Tìm overdue loans
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueAt < :now")
    List<Loan> findOverdueLoans(@Param("now") LocalDateTime now);
    
    // Đếm số lượng book đang được mượn
    @Query("SELECT SUM(l.quantity) FROM Loan l WHERE l.bookId = :bookId AND l.status = 'ACTIVE'")
    Optional<Integer> countBorrowedBooksByBookId(@Param("bookId") Long bookId);
    
    // Tìm loans trong khoảng thời gian
    List<Loan> findByBorrowedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Tìm loans theo status
    List<Loan> findByStatus(LoanStatus status);
    
    // Tìm loans sắp đến hạn trong khoảng thời gian
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueAt BETWEEN :startDate AND :endDate")
    List<Loan> findLoansDueBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Kiểm tra user có đang mượn book nào đó không
    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, LoanStatus status);
}