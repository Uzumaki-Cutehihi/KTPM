package com.scar.bookvault.search.api;

import com.scar.bookvault.search.domain.BookDocument;
import com.scar.bookvault.search.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/v1")
public class SearchController {
    
    private final SearchService searchService;
    
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    
    // Tìm kiếm full-text với query parameter
    @GetMapping("/books")
    public ResponseEntity<List<BookDocument>> searchBooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<BookDocument> results = searchService.searchBooks(q);
        return ResponseEntity.ok(results.stream()
                .limit(limit)
                .toList());
    }
    
    // Tìm kiếm theo title
    @GetMapping("/books/title/{title}")
    public ResponseEntity<List<BookDocument>> searchByTitle(@PathVariable String title) {
        List<BookDocument> results = searchService.searchByTitle(title);
        return ResponseEntity.ok(results);
    }
    
    // Tìm kiếm theo author
    @GetMapping("/books/author/{author}")
    public ResponseEntity<List<BookDocument>> searchByAuthor(@PathVariable String author) {
        List<BookDocument> results = searchService.searchByAuthor(author);
        return ResponseEntity.ok(results);
    }
    
    // Tìm kiếm theo ISBN
    @GetMapping("/books/isbn/{isbn}")
    public ResponseEntity<BookDocument> findByIsbn(@PathVariable String isbn) {
        return searchService.findByIsbn(isbn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Tìm kiếm phân trang
    @GetMapping("/books/paged")
    public ResponseEntity<Page<BookDocument>> searchPaged(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<BookDocument> results = searchService.searchWithPagination(query, page, size);
        return ResponseEntity.ok(results);
    }
    
    // Tìm kiếm theo category
    @GetMapping("/books/category/{category}")
    public ResponseEntity<List<BookDocument>> searchByCategory(@PathVariable String category) {
        List<BookDocument> results = searchService.searchByCategory(category);
        return ResponseEntity.ok(results);
    }
    
    // Admin: Index một book mới
    @PostMapping("/admin/books")
    public ResponseEntity<BookDocument> indexBook(@RequestBody BookDocument book) {
        BookDocument indexed = searchService.indexBook(book);
        return ResponseEntity.ok(indexed);
    }
    
    // Admin: Xóa book khỏi index
    @DeleteMapping("/admin/books/{id}")
    public ResponseEntity<Void> deleteBookFromIndex(@PathVariable Long id) {
        searchService.deleteBookFromIndex(id);
        return ResponseEntity.noContent().build();
    }
    
    // Admin: Cập nhật số lượng
    @PutMapping("/admin/books/{id}/quantity")
    public ResponseEntity<Void> updateBookQuantity(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {
        
        Integer newQuantity = request.get("quantity");
        if (newQuantity == null || newQuantity < 0) {
            return ResponseEntity.badRequest().build();
        }
        
        searchService.updateBookQuantity(id, newQuantity);
        return ResponseEntity.ok().build();
    }
    
    // Thống kê
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalBooks = searchService.countBooks();
        List<BookDocument> allBooks = searchService.getAllBooks();
        
        Map<String, Object> stats = Map.of(
                "totalBooks", totalBooks,
                "indexedBooks", allBooks.size()
        );
        
        return ResponseEntity.ok(stats);
    }
}