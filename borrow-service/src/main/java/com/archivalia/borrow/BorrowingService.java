package com.archivalia.borrow;

import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class BorrowingService {
    private final BorrowingRepository borrowings;
    private final RestClient client;
    private final int loanPeriodDays;

    public BorrowingService(BorrowingRepository borrowings, RestClient.Builder clientBuilder,
                            @Value("${app.loan-period-days}") int loanPeriodDays) {
        if (loanPeriodDays < 1) {
            throw new IllegalArgumentException("LOAN_PERIOD_DAYS must be at least one.");
        }
        this.borrowings = borrowings;
        this.client = clientBuilder.build();
        this.loanPeriodDays = loanPeriodDays;
    }

    public BorrowingResponse borrow(String userId, BorrowRequest request) {
        String borrowingId = UUID.randomUUID().toString();
        BookInfo book = client.post().uri("http://book-service/internal/books/{id}/reserve", request.bookId())
                .body(new ReservationRequest(borrowingId))
                .retrieve().body(BookInfo.class);
        if (book == null) {
            releaseCopy(request.bookId(), borrowingId);
            throw new IllegalStateException("Book service returned an empty reservation response.");
        }
        LocalDate borrowedOn = LocalDate.now();
        Borrowing borrowing;
        try {
            borrowing = borrowings.save(new Borrowing(borrowingId, userId, book.id(), book.title(), book.author(),
                    borrowedOn, borrowedOn.plusDays(loanPeriodDays)));
        } catch (DataAccessException failure) {
            try {
                releaseCopy(book.id(), borrowingId);
            } catch (RestClientException compensationFailure) {
                failure.addSuppressed(compensationFailure);
            }
            throw failure;
        }
        return BorrowingResponse.from(borrowing);
    }

    public List<BorrowingResponse> listForUser(String userId) {
        return borrowings.findByUserIdOrderByBorrowedOnDesc(userId).stream().map(BorrowingResponse::from).toList();
    }

    @Transactional
    public BorrowingResponse returnBook(String userId, String borrowingId) {
        Borrowing borrowing = borrowings.findByIdForUpdate(borrowingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrowing was not found."));
        if (!borrowing.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only return your own borrowings.");
        }
        if ("RETURNED".equals(borrowing.getStatus())) {
            return BorrowingResponse.from(borrowing);
        }

        LocalDate returnedOn = LocalDate.now();
        FineInfo fine = client.post().uri("http://fine-service/internal/fines/assess")
                .body(new FineRequest(borrowing.getId(), userId, borrowing.getDueOn(), returnedOn))
                .retrieve().body(FineInfo.class);
        if (fine == null) {
            throw new IllegalStateException("Fine service returned an empty assessment response.");
        }
        releaseCopy(borrowing.getBookId(), borrowing.getId());
        borrowing.markReturned(returnedOn, fine.amount(), fine.overdueDays());
        return BorrowingResponse.from(borrowings.save(borrowing));
    }

    private void releaseCopy(String bookId, String borrowingId) {
        client.post().uri("http://book-service/internal/books/{id}/release", bookId)
                .body(new ReservationRequest(borrowingId))
                .retrieve().toBodilessEntity();
    }

    public record BorrowRequest(@NotBlank String bookId) { }
    public record ReservationRequest(String borrowingId) { }
    public record BookInfo(String id, String title, String author) { }
    public record FineRequest(String borrowingId, String userId, LocalDate dueOn, LocalDate returnedOn) { }
    public record FineInfo(String id, BigDecimal amount, long overdueDays) { }
    public record BorrowingResponse(String id, String userId, String bookId, String bookTitle, String bookAuthor,
                                    LocalDate borrowedOn, LocalDate dueOn, LocalDate returnedOn, String status,
                                    BigDecimal fineAmount, long overdueDays) {
        static BorrowingResponse from(Borrowing borrowing) {
            return new BorrowingResponse(borrowing.getId(), borrowing.getUserId(), borrowing.getBookId(),
                    borrowing.getBookTitle(), borrowing.getBookAuthor(), borrowing.getBorrowedOn(),
                    borrowing.getDueOn(), borrowing.getReturnedOn(), borrowing.getStatus(),
                    borrowing.getFineAmount(), borrowing.getOverdueDays());
        }
    }
}
