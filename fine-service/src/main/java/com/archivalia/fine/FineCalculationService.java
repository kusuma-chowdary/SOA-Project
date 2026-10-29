package com.archivalia.fine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FineCalculationService {
    private final FineAssessmentRepository fines;
    private final BigDecimal dailyRate;

    public FineCalculationService(FineAssessmentRepository fines,
                                  @Value("${app.fine-per-day}") BigDecimal dailyRate) {
        if (dailyRate.signum() < 0) {
            throw new IllegalArgumentException("FINE_PER_DAY cannot be negative.");
        }
        this.fines = fines;
        this.dailyRate = dailyRate.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public FineResponse assess(AssessmentRequest request) {
        if (request.returnedOn().isBefore(request.dueOn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Return date cannot be before the due date.");
        }
        FineAssessment existing = fines.findByBorrowingId(request.borrowingId()).orElse(null);
        if (existing != null) {
            if (!existing.getUserId().equals(request.userId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This borrowing already has a fine assessment.");
            }
            return FineResponse.from(existing);
        }
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(request.dueOn(), request.returnedOn()));
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(overdueDays)).setScale(2, RoundingMode.HALF_UP);
        FineAssessment assessment = fines.save(new FineAssessment(request.borrowingId(), request.userId(),
                request.dueOn(), request.returnedOn(), amount, overdueDays));
        return FineResponse.from(assessment);
    }

    public List<FineResponse> listForUser(String userId) {
        return fines.findByUserIdOrderByReturnedOnDesc(userId).stream().map(FineResponse::from).toList();
    }

    public record AssessmentRequest(
            @jakarta.validation.constraints.NotBlank String borrowingId,
            @jakarta.validation.constraints.NotBlank String userId,
            @jakarta.validation.constraints.NotNull java.time.LocalDate dueOn,
            @jakarta.validation.constraints.NotNull java.time.LocalDate returnedOn) { }

    public record FineResponse(String id, String borrowingId, String userId, java.time.LocalDate dueOn,
                               java.time.LocalDate returnedOn, BigDecimal amount, long overdueDays) {
        static FineResponse from(FineAssessment assessment) {
            return new FineResponse(assessment.getId(), assessment.getBorrowingId(), assessment.getUserId(),
                    assessment.getDueOn(), assessment.getReturnedOn(), assessment.getAmount(),
                    assessment.getOverdueDays());
        }
    }
}
