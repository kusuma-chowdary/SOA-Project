package com.archivalia.book;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BookCatalogService {
    private final BookRepository books;
    private final BookReservationRepository reservations;

    public BookCatalogService(BookRepository books, BookReservationRepository reservations) {
        this.books = books;
        this.reservations = reservations;
    }

    public List<BookResponse> search(String query) {
        List<Book> results = query == null || query.isBlank()
                ? books.findAll(Sort.by(Sort.Direction.ASC, "title"))
                : books.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                        query.trim(), query.trim(), query.trim());
        return results.stream().map(BookResponse::from).toList();
    }

    public BookResponse get(String id) {
        return BookResponse.from(find(id));
    }

    public BookResponse create(BookRequest request) {
        return BookResponse.from(books.save(new Book(request.title().trim(), request.author().trim(),
                request.category().trim(), request.publishedYear(), request.description(), request.coverUrl(),
                request.totalCopies())));
    }

    public BookResponse update(String id, BookRequest request) {
        Book book = find(id);
        try {
            book.update(request.title().trim(), request.author().trim(), request.category().trim(),
                    request.publishedYear(), request.description(), request.coverUrl(), request.totalCopies());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        return BookResponse.from(books.save(book));
    }

    public void delete(String id) {
        Book book = find(id);
        if (book.getBorrowedCopies() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Return all borrowed copies before removing this title.");
        }
        books.delete(book);
    }

    @Transactional
    public BookResponse reserve(String id, String borrowingId) {
        Book book = books.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book was not found."));
        BookReservation existing = reservations.findById(borrowingId).orElse(null);
        if (existing != null) {
            if (!existing.getBookId().equals(id) || !"ACTIVE".equals(existing.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This borrowing cannot reserve another copy.");
            }
            return BookResponse.from(book);
        }
        try {
            book.reserveCopy();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        reservations.save(new BookReservation(borrowingId, id));
        return BookResponse.from(book);
    }

    @Transactional
    public BookResponse release(String id, String borrowingId) {
        Book book = books.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book was not found."));
        BookReservation reservation = reservations.findById(borrowingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Copy reservation was not found."));
        if (!reservation.getBookId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This reservation belongs to a different title.");
        }
        if ("RETURNED".equals(reservation.getStatus())) {
            return BookResponse.from(book);
        }
        try {
            book.releaseCopy();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        reservation.markReturned();
        reservations.save(reservation);
        return BookResponse.from(book);
    }

    private Book find(String id) {
        return books.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book was not found."));
    }

    public record BookRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 180) String title,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 140) String author,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String category,
            @jakarta.validation.constraints.Size(max = 40) String publishedYear,
            @jakarta.validation.constraints.Size(max = 1000) String description,
            @jakarta.validation.constraints.Size(max = 500) String coverUrl,
            @jakarta.validation.constraints.Min(0) int totalCopies) { }

    public record BookResponse(String id, String title, String author, String category, String publishedYear,
                               String description, String coverUrl, int totalCopies, int borrowedCopies,
                               int availableCopies) {
        static BookResponse from(Book book) {
            return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getCategory(),
                    book.getPublishedYear(), book.getDescription(), book.getCoverUrl(), book.getTotalCopies(),
                    book.getBorrowedCopies(), book.getAvailableCopies());
        }
    }

    public record ReservationRequest(@jakarta.validation.constraints.NotBlank String borrowingId) { }
}
