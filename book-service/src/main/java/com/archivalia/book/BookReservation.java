package com.archivalia.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "book_reservations")
public class BookReservation {
    @Id
    private String id;

    @Column(nullable = false)
    private String bookId;

    @Column(nullable = false)
    private String status;

    protected BookReservation() {
    }

    public BookReservation(String id, String bookId) {
        this.id = id;
        this.bookId = bookId;
        this.status = "ACTIVE";
    }

    public String getBookId() { return bookId; }
    public String getStatus() { return status; }
    public void markReturned() { this.status = "RETURNED"; }
}
