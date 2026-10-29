package com.archivalia.borrow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "borrowings")
public class Borrowing {
    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String bookId;

    @Column(nullable = false)
    private String bookTitle;

    @Column(nullable = false)
    private String bookAuthor;

    @Column(nullable = false)
    private LocalDate borrowedOn;

    @Column(nullable = false)
    private LocalDate dueOn;

    private LocalDate returnedOn;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fineAmount;

    @Column(nullable = false)
    private long overdueDays;

    @Version
    private long version;

    protected Borrowing() {
    }

    public Borrowing(String id, String userId, String bookId, String bookTitle, String bookAuthor,
                     LocalDate borrowedOn, LocalDate dueOn) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
        this.borrowedOn = borrowedOn;
        this.dueOn = dueOn;
        this.status = "ACTIVE";
        this.fineAmount = BigDecimal.ZERO.setScale(2);
        this.overdueDays = 0;
    }

    public void markReturned(LocalDate returnedOn, BigDecimal fineAmount, long overdueDays) {
        this.returnedOn = returnedOn;
        this.fineAmount = fineAmount;
        this.overdueDays = overdueDays;
        this.status = "RETURNED";
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getBookAuthor() { return bookAuthor; }
    public LocalDate getBorrowedOn() { return borrowedOn; }
    public LocalDate getDueOn() { return dueOn; }
    public LocalDate getReturnedOn() { return returnedOn; }
    public String getStatus() { return status; }
    public BigDecimal getFineAmount() { return fineAmount; }
    public long getOverdueDays() { return overdueDays; }
}
