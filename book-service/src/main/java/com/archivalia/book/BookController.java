package com.archivalia.book;

import com.archivalia.book.BookCatalogService.BookRequest;
import com.archivalia.book.BookCatalogService.BookResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookCatalogService catalog;

    public BookController(BookCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<BookResponse> search(@RequestParam(required = false) String q) {
        return catalog.search(q);
    }

    @GetMapping("/{id}")
    public BookResponse get(@PathVariable String id) {
        return catalog.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request) {
        return catalog.create(request);
    }

    @PutMapping("/{id}")
    public BookResponse update(@PathVariable String id, @Valid @RequestBody BookRequest request) {
        return catalog.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        catalog.delete(id);
    }
}
