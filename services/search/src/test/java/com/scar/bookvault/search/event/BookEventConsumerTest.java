package com.scar.bookvault.search.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scar.bookvault.search.domain.BookDocument;
import com.scar.bookvault.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BookEventConsumerTest {
    private SearchService searchService;
    private ObjectMapper objectMapper;
    private BookEventConsumer consumer;

    @BeforeEach
    void setup() {
        searchService = mock(SearchService.class);
        objectMapper = new ObjectMapper();
        consumer = new BookEventConsumer(searchService, objectMapper);
    }

    @Test
    void handleBookCreated_indexesDocument() throws Exception {
        String msg = objectMapper.writeValueAsString(java.util.Map.of(
                "bookId", 1L,
                "title", "Test",
                "author", "Author",
                "isbn", "ISBN",
                "quantity", 3
        ));
        ArgumentCaptor<BookDocument> captor = ArgumentCaptor.forClass(BookDocument.class);

        consumer.handleBookCreated(msg);

        verify(searchService, times(1)).indexBook(captor.capture());
        BookDocument doc = captor.getValue();
        assertEquals(1L, doc.getId());
        assertEquals("Test", doc.getTitle());
    }

    @Test
    void handleBookUpdated_updatesDocument() throws Exception {
        BookDocument existing = new BookDocument();
        existing.setId(2L);
        existing.setTitle("Old");
        when(searchService.searchBooks("2")).thenReturn(List.of(existing));

        String msg = objectMapper.writeValueAsString(java.util.Map.of(
                "bookId", 2L,
                "title", "New"
        ));

        consumer.handleBookUpdated(msg);
        verify(searchService, times(1)).indexBook(Mockito.argThat(doc -> doc.getId().equals(2L) && "New".equals(doc.getTitle())));
    }

    @Test
    void handleBookDeleted_removesDocument() throws Exception {
        String msg = objectMapper.writeValueAsString(java.util.Map.of("bookId", 3L));
        consumer.handleBookDeleted(msg);
        verify(searchService, times(1)).deleteBookFromIndex(3L);
    }
}

