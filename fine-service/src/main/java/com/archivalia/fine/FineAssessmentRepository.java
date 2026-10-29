package com.archivalia.fine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FineAssessmentRepository extends JpaRepository<FineAssessment, String> {
    Optional<FineAssessment> findByBorrowingId(String borrowingId);
    List<FineAssessment> findByUserIdOrderByReturnedOnDesc(String userId);
}
