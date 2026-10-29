package com.archivalia.fine;

import com.archivalia.fine.FineCalculationService.AssessmentRequest;
import com.archivalia.fine.FineCalculationService.FineResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FineCalculationServiceTest {
    @Mock
    private FineAssessmentRepository fines;

    private FineCalculationService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new FineCalculationService(fines, new BigDecimal("1.00"));
    }

    @Test
    void calculatesOneRupeeForEachOverdueDay() {
        FineAssessment saved = new FineAssessment("loan-1", "user-1",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 13), new BigDecimal("3.00"), 3);
        when(fines.findByBorrowingId("loan-1")).thenReturn(Optional.empty());
        when(fines.save(any(FineAssessment.class))).thenReturn(saved);

        FineResponse result = service.assess(new AssessmentRequest("loan-1", "user-1",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 13)));

        assertEquals(3, result.overdueDays());
        assertEquals(new BigDecimal("3.00"), result.amount());
        verify(fines).save(any(FineAssessment.class));
    }

    @Test
    void doesNotChargeWhenReturnedOnTheDueDate() {
        FineAssessment saved = new FineAssessment("loan-2", "user-1",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), new BigDecimal("0.00"), 0);
        when(fines.findByBorrowingId("loan-2")).thenReturn(Optional.empty());
        when(fines.save(any(FineAssessment.class))).thenReturn(saved);

        FineResponse result = service.assess(new AssessmentRequest("loan-2", "user-1",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10)));

        assertEquals(0, result.overdueDays());
        assertEquals(new BigDecimal("0.00"), result.amount());
    }

    @Test
    void rejectsReturnDateBeforeDueDate() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.assess(new AssessmentRequest("loan-3", "user-1",
                        LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 9))));
    }
}
