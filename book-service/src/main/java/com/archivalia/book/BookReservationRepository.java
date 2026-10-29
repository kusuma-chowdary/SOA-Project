package com.archivalia.book;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookReservationRepository extends JpaRepository<BookReservation, String> {
}
