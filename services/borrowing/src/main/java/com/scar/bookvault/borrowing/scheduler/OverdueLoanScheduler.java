package com.scar.bookvault.borrowing.scheduler;

import com.scar.bookvault.borrowing.domain.Loan;
import com.scar.bookvault.borrowing.domain.LoanStatus;
import com.scar.bookvault.borrowing.domain.LoanRepository;
import com.scar.bookvault.borrowing.service.LoanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OverdueLoanScheduler {
    private static final Logger logger = LoggerFactory.getLogger(OverdueLoanScheduler.class);
    
    private final LoanRepository loanRepository;
    private final LoanService loanService;
    
    public OverdueLoanScheduler(LoanRepository loanRepository, LoanService loanService) {
        this.loanRepository = loanRepository;
        this.loanService = loanService;
    }
    
    /**
     * Check for overdue loans every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void checkOverdueLoans() {
        logger.info("Checking for overdue loans...");
        
        LocalDateTime now = LocalDateTime.now();
        List<Loan> overdueLoans = loanRepository.findOverdueLoans(now);
        
        logger.info("Found {} overdue loans", overdueLoans.size());
        
        for (Loan loan : overdueLoans) {
            try {
                // Calculate overdue days
                int overdueDays = (int) java.time.Duration.between(loan.getDueAt(), now).toDays();
                
                // Publish overdue event
                loanService.publishLoanOverdue(loan, overdueDays);
                
                logger.info("Published overdue event for loan ID: {} ({} days overdue)", 
                           loan.getId(), overdueDays);
                           
            } catch (Exception e) {
                logger.error("Failed to process overdue loan ID: {}", loan.getId(), e);
            }
        }
    }
    
    /**
     * Send reminder notifications for loans due in 3 days
     */
    @Scheduled(fixedRate = 86400000) // 24 hours in milliseconds
    public void sendDueDateReminders() {
        logger.info("Sending due date reminders...");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);
        
        List<Loan> loansDueSoon = loanRepository.findLoansDueBetween(now, threeDaysFromNow);
        
        logger.info("Found {} loans due in the next 3 days", loansDueSoon.size());
        
        for (Loan loan : loansDueSoon) {
            try {
                loanService.publishDueDateReminder(loan);
                
                logger.info("Published due date reminder for loan ID: {}", loan.getId());
                
            } catch (Exception e) {
                logger.error("Failed to send reminder for loan ID: {}", loan.getId(), e);
            }
        }
    }
}