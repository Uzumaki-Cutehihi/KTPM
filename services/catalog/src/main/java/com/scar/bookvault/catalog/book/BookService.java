package com.scar.bookvault.catalog.book;

import com.scar.bookvault.catalog.event.BookEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final BookEventPublisher eventPublisher;

    public BookService(BookRepository bookRepository, BookEventPublisher eventPublisher) {
        this.bookRepository = bookRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Book> list() {
        return bookRepository.findAll();
    }

    public Book get(Long id) {
        return bookRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Book create(Book book) {
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            throw new IllegalArgumentException("ISBN already exists");
        }
        book.setCreatedAt(java.time.OffsetDateTime.now());
        book.setUpdatedAt(java.time.OffsetDateTime.now());
        Book saved = bookRepository.save(book);
        
        // Publish event for Search Service
        eventPublisher.publishBookCreated(saved);
        
        return saved;
    }

    @Transactional
    public Book update(Long id, Book incoming) {
        Book existing = get(id);
        existing.setTitle(incoming.getTitle());
        existing.setAuthor(incoming.getAuthor());
        existing.setIsbn(incoming.getIsbn());
        existing.setQuantity(incoming.getQuantity());
        existing.setUpdatedAt(java.time.OffsetDateTime.now());
        Book saved = bookRepository.save(existing);
        
        // Publish event for Search Service
        eventPublisher.publishBookUpdated(saved);
        
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        bookRepository.deleteById(id);
        
        // Publish event for Search Service
        eventPublisher.publishBookDeleted(id);
    }
    
    @Transactional
    public Book updateQuantity(Long id, Integer quantityChange) {
        Book book = get(id);
        book.setQuantity(book.getQuantity() + quantityChange);
        book.setUpdatedAt(java.time.OffsetDateTime.now());
        Book saved = bookRepository.save(book);
        
        // Publish event for Search Service
        eventPublisher.publishBookUpdated(saved);
        
        return saved;
    }
}