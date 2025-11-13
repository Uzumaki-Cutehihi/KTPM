package com.scar.bookvault.catalog.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class BookControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("test_catalog")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldCreateAndRetrieveBook() throws Exception {
        // Given
        Book newBook = new Book();
        newBook.setTitle("Integration Test Book");
        newBook.setAuthor("Test Author");
        newBook.setIsbn("978-INTEGRATION-TEST");
        newBook.setQuantity(5);

        // When - Create book
        String response = mockMvc.perform(post("/api/catalog/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBook)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Integration Test Book"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Book created = objectMapper.readValue(response, Book.class);

        // Then - Retrieve book
        mockMvc.perform(get("/api/catalog/v1/books/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.title").value("Integration Test Book"));
    }

    @Test
    void shouldUpdateBook() throws Exception {
        // Given - Create a book first
        Book book = new Book();
        book.setTitle("Original Title");
        book.setAuthor("Original Author");
        book.setIsbn("978-UPDATE-TEST");
        book.setQuantity(10);

        String response = mockMvc.perform(post("/api/catalog/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Book created = objectMapper.readValue(response, Book.class);

        // When - Update the book
        Book updated = new Book();
        updated.setTitle("Updated Title");
        updated.setAuthor("Updated Author");
        updated.setIsbn("978-UPDATE-TEST");
        updated.setQuantity(20);

        mockMvc.perform(put("/api/catalog/v1/books/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.quantity").value(20));
    }

    @Test
    void shouldDeleteBook() throws Exception {
        // Given - Create a book first
        Book book = new Book();
        book.setTitle("To Be Deleted");
        book.setAuthor("Author");
        book.setIsbn("978-DELETE-TEST");
        book.setQuantity(1);

        String response = mockMvc.perform(post("/api/catalog/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Book created = objectMapper.readValue(response, Book.class);

        // When - Delete the book
        mockMvc.perform(delete("/api/catalog/v1/books/" + created.getId()))
                .andExpect(status().isNoContent());

        // Then - Verify it's deleted
        mockMvc.perform(get("/api/catalog/v1/books/" + created.getId()))
                .andExpect(status().is5xxServerError()); // Should throw exception
    }
}
