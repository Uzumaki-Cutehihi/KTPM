package com.scar.bookvault.catalog.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldListBooks() throws Exception {
        // Given
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setIsbn("978-1234567890");
        book.setQuantity(10);

        when(bookService.list()).thenReturn(List.of(book));

        // When & Then
        mockMvc.perform(get("/api/catalog/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Book"));

        verify(bookService).list();
    }

    @Test
    void shouldGetBookById() throws Exception {
        // Given
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        when(bookService.get(1L)).thenReturn(book);

        // When & Then
        mockMvc.perform(get("/api/catalog/v1/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Book"));

        verify(bookService).get(1L);
    }

    @Test
    void shouldCreateBook() throws Exception {
        // Given
        Book newBook = new Book();
        newBook.setTitle("New Book");
        newBook.setAuthor("New Author");
        newBook.setIsbn("978-9876543210");
        newBook.setQuantity(5);

        Book savedBook = new Book();
        savedBook.setId(1L);
        savedBook.setTitle("New Book");
        savedBook.setAuthor("New Author");
        savedBook.setIsbn("978-9876543210");
        savedBook.setQuantity(5);

        when(bookService.create(any(Book.class))).thenReturn(savedBook);

        // When & Then
        mockMvc.perform(post("/api/catalog/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBook)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New Book"));

        verify(bookService).create(any(Book.class));
    }

    @Test
    void shouldUpdateBook() throws Exception {
        // Given
        Book updatedBook = new Book();
        updatedBook.setTitle("Updated Book");
        updatedBook.setAuthor("Updated Author");
        updatedBook.setIsbn("978-1111111111");
        updatedBook.setQuantity(20);

        Book savedBook = new Book();
        savedBook.setId(1L);
        savedBook.setTitle("Updated Book");

        when(bookService.update(eq(1L), any(Book.class))).thenReturn(savedBook);

        // When & Then
        mockMvc.perform(put("/api/catalog/v1/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedBook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Book"));

        verify(bookService).update(eq(1L), any(Book.class));
    }

    @Test
    void shouldDeleteBook() throws Exception {
        // Given
        doNothing().when(bookService).delete(1L);

        // When & Then
        mockMvc.perform(delete("/api/catalog/v1/books/1"))
                .andExpect(status().isNoContent());

        verify(bookService).delete(1L);
    }
}

