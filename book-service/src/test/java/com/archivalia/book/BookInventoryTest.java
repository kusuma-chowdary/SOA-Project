package com.archivalia.book;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookInventoryTest {
    @Test
    void reservesAndReleasesCopies() {
        Book book = new Book("A title", "An author", "Research", "2024", null, null, 2);

        book.reserveCopy();
        assertEquals(1, book.getAvailableCopies());
        book.releaseCopy();
        assertEquals(2, book.getAvailableCopies());
    }

    @Test
    void doesNotReserveMoreThanAvailableCopies() {
        Book book = new Book("A title", "An author", "Research", "2024", null, null, 1);
        book.reserveCopy();

        assertThrows(IllegalStateException.class, book::reserveCopy);
    }

    @Test
    void doesNotReleaseWhenNoCopiesAreBorrowed() {
        Book book = new Book("A title", "An author", "Research", "2024", null, null, 1);

        assertThrows(IllegalStateException.class, book::releaseCopy);
    }
}
