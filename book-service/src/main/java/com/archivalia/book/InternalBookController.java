package com.archivalia.book;

import com.archivalia.book.BookCatalogService.BookResponse;
import com.archivalia.book.BookCatalogService.ReservationRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/books")
public class InternalBookController {
    private final BookCatalogService catalog;

    public InternalBookController(BookCatalogService catalog) {
        this.catalog = catalog;
    }

    @PostMapping("/{id}/reserve")
    public BookResponse reserve(@PathVariable String id, @Valid @RequestBody ReservationRequest request) {
        return catalog.reserve(id, request.borrowingId());
    }

    @PostMapping("/{id}/release")
    public BookResponse release(@PathVariable String id, @Valid @RequestBody ReservationRequest request) {
        return catalog.release(id, request.borrowingId());
    }
}
