package com.scar.bookvault.search.repository;

import com.scar.bookvault.search.domain.BookDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, Long> {
    
    // Tìm kiếm theo title với fuzzy search
    List<BookDocument> findByTitleContaining(String title);
    
    // Tìm kiếm theo author
    List<BookDocument> findByAuthorContaining(String author);
    
    // Tìm kiếm theo ISBN chính xác
    Optional<BookDocument> findByIsbn(String isbn);
    
    // Tìm kiếm full-text với custom query
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title^2\", \"author\", \"description\"]}}")
    List<BookDocument> searchByQuery(String query);
    
    // Tìm kiếm có phân trang
    Page<BookDocument> findByTitleContainingOrAuthorContaining(String title, String author, Pageable pageable);
    
    // Tìm kiếm theo category
    List<BookDocument> findByCategory(String category);
    
    // Kiểm tra sự tồn tại
    boolean existsByIsbn(String isbn);
}