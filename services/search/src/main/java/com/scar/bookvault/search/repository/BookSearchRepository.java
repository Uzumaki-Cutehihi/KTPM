package com.scar.bookvault.search.repository;

import com.scar.bookvault.search.domain.BookDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, Long> {
    List<BookDocument> findByTitleContainingOrAuthorContaining(String title, String author);
}

