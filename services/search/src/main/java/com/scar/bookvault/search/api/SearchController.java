package com.scar.bookvault.search.api;

import com.scar.bookvault.search.domain.BookDocument;
import com.scar.bookvault.search.repository.BookSearchRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search/v1/books")
public class SearchController {
    private final BookSearchRepository bookSearchRepository;

    public SearchController(BookSearchRepository bookSearchRepository) {
        this.bookSearchRepository = bookSearchRepository;
    }

    @GetMapping
    public List<BookDocument> search(@RequestParam(defaultValue = "") String q) {
        if (q == null || q.trim().isEmpty()) {
            return List.of();
        }
        return bookSearchRepository.findByTitleContainingOrAuthorContaining(q, q);
    }

    @PostMapping
    public BookDocument index(@RequestBody BookDocument book) {
        return bookSearchRepository.save(book);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        bookSearchRepository.deleteById(id);
    }
}
