package com.archivalia.fine;

import com.archivalia.fine.FineCalculationService.AssessmentRequest;
import com.archivalia.fine.FineCalculationService.FineResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FineController {
    private final FineCalculationService fineCalculation;

    public FineController(FineCalculationService fineCalculation) {
        this.fineCalculation = fineCalculation;
    }

    @GetMapping("/api/fines/mine")
    public List<FineResponse> mine(@RequestHeader("X-Authenticated-User") String userId) {
        return fineCalculation.listForUser(userId);
    }

    @PostMapping("/internal/fines/assess")
    @ResponseStatus(HttpStatus.CREATED)
    public FineResponse assess(@Valid @RequestBody AssessmentRequest request) {
        return fineCalculation.assess(request);
    }
}
