package com.scar.bookvault.catalog.book;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog/v1/books")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> list() { return bookService.list(); }

    @GetMapping("/{id}")
    public Book get(@PathVariable Long id) { return bookService.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Book create(@RequestBody @Valid Book book) { return bookService.create(book); }

    @PutMapping("/{id}")
    public Book update(@PathVariable Long id, @RequestBody @Valid Book book) { return bookService.update(id, book); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { bookService.delete(id); }

    // Endpoint cho Borrowing Service để cập nhật quantity
    @PutMapping("/{id}/quantity")
    public Book updateQuantity(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Integer quantityChange = request.get("quantityChange");
        if (quantityChange == null) {
            throw new IllegalArgumentException("quantityChange is required");
        }
        return bookService.updateQuantity(id, quantityChange);
    }
}