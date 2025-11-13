package com.scar.bookvault.search.service;

import com.scar.bookvault.search.domain.BookDocument;
import com.scar.bookvault.search.repository.BookSearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SearchService {
    
    private final BookSearchRepository bookSearchRepository;
    
    public SearchService(BookSearchRepository bookSearchRepository) {
        this.bookSearchRepository = bookSearchRepository;
    }
    
    // Tìm kiếm full-text với query string
    public List<BookDocument> searchBooks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return bookSearchRepository.searchByQuery(query);
    }
    
    // Tìm kiếm theo title
    public List<BookDocument> searchByTitle(String title) {
        return bookSearchRepository.findByTitleContaining(title);
    }
    
    // Tìm kiếm theo author
    public List<BookDocument> searchByAuthor(String author) {
        return bookSearchRepository.findByAuthorContaining(author);
    }
    
    // Tìm kiếm theo ISBN
    public Optional<BookDocument> findByIsbn(String isbn) {
        return bookSearchRepository.findByIsbn(isbn);
    }
    
    // Tìm kiếm phân trang
    public Page<BookDocument> searchWithPagination(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookSearchRepository.findByTitleContainingOrAuthorContaining(query, query, pageable);
    }
    
    // Tìm kiếm theo category
    public List<BookDocument> searchByCategory(String category) {
        return bookSearchRepository.findByCategory(category);
    }
    
    // Index một book mới hoặc cập nhật
    public BookDocument indexBook(BookDocument book) {
        return bookSearchRepository.save(book);
    }
    
    // Xóa book khỏi index
    public void deleteBookFromIndex(Long bookId) {
        bookSearchRepository.deleteById(bookId);
    }
    
    // Cập nhật số lượng sách
    public void updateBookQuantity(Long bookId, Integer newQuantity) {
        Optional<BookDocument> optionalBook = bookSearchRepository.findById(bookId);
        if (optionalBook.isPresent()) {
            BookDocument book = optionalBook.get();
            book.setQuantity(newQuantity);
            book.setUpdatedAt(java.time.LocalDateTime.now());
            bookSearchRepository.save(book);
        }
    }
    
    // Lấy tất cả books (cho admin)
    public List<BookDocument> getAllBooks() {
        return (List<BookDocument>) bookSearchRepository.findAll();
    }
    
    // Đếm tổng số books
    public long countBooks() {
        return bookSearchRepository.count();
    }
}