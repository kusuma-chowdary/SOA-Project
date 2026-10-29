package com.archivalia.borrow;

import com.archivalia.borrow.BorrowingService.BorrowRequest;
import com.archivalia.borrow.BorrowingService.BorrowingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/borrowings")
public class BorrowingController {
    private final BorrowingService borrowings;

    public BorrowingController(BorrowingService borrowings) {
        this.borrowings = borrowings;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowingResponse borrow(@RequestHeader("X-Authenticated-User") String userId,
                                    @Valid @RequestBody BorrowRequest request) {
        return borrowings.borrow(userId, request);
    }

    @GetMapping("/mine")
    public List<BorrowingResponse> mine(@RequestHeader("X-Authenticated-User") String userId) {
        return borrowings.listForUser(userId);
    }

    @PostMapping("/{id}/return")
    public BorrowingResponse returnBook(@RequestHeader("X-Authenticated-User") String userId,
                                       @PathVariable String id) {
        return borrowings.returnBook(userId, id);
    }
}
