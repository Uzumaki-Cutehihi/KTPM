package com.scar.bookvault.catalog.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setIsbn("978-1234567890");
        testBook.setQuantity(10);
    }

    @Test
    void shouldListAllBooks() {
        // Given
        when(bookRepository.findAll()).thenReturn(List.of(testBook));

        // When
        List<Book> result = bookService.list();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
        verify(bookRepository).findAll();
    }

    @Test
    void shouldGetBookById() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // When
        Book result = bookService.get(1L);

        // Then
        assertNotNull(result);
        assertEquals("Test Book", result.getTitle());
        verify(bookRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenBookNotFound() {
        // Given
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(Exception.class, () -> bookService.get(999L));
        verify(bookRepository).findById(999L);
    }

    @Test
    void shouldCreateBook() {
        // Given
        Book newBook = new Book();
        newBook.setTitle("New Book");
        newBook.setAuthor("New Author");
        newBook.setIsbn("978-9876543210");
        newBook.setQuantity(5);

        when(bookRepository.existsByIsbn("978-9876543210")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // When
        Book result = bookService.create(newBook);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(bookRepository).existsByIsbn("978-9876543210");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingBookWithDuplicateIsbn() {
        // Given
        Book newBook = new Book();
        newBook.setIsbn("978-1234567890");

        when(bookRepository.existsByIsbn("978-1234567890")).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> bookService.create(newBook));
        verify(bookRepository).existsByIsbn("978-1234567890");
        verify(bookRepository, never()).save(any());
    }

    @Test
    void shouldUpdateBook() {
        // Given
        Book updatedBook = new Book();
        updatedBook.setTitle("Updated Title");
        updatedBook.setAuthor("Updated Author");
        updatedBook.setIsbn("978-1111111111");
        updatedBook.setQuantity(20);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        Book result = bookService.update(1L, updatedBook);

        // Then
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Author", result.getAuthor());
        assertEquals("978-1111111111", result.getIsbn());
        assertEquals(20, result.getQuantity());
        verify(bookRepository).findById(1L);
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void shouldDeleteBook() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        doNothing().when(bookRepository).deleteById(1L);

        // When
        bookService.delete(1L);

        // Then
        verify(bookRepository).findById(1L);
        verify(bookRepository).deleteById(1L);
    }
}

