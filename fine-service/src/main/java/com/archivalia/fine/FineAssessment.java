package com.archivalia.fine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fine_assessments")
public class FineAssessment {
    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @Column(nullable = false, unique = true)
    private String borrowingId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private LocalDate dueOn;

    @Column(nullable = false)
    private LocalDate returnedOn;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private long overdueDays;

    protected FineAssessment() {
    }

    public FineAssessment(String borrowingId, String userId, LocalDate dueOn, LocalDate returnedOn,
                         BigDecimal amount, long overdueDays) {
        this.borrowingId = borrowingId;
        this.userId = userId;
        this.dueOn = dueOn;
        this.returnedOn = returnedOn;
        this.amount = amount;
        this.overdueDays = overdueDays;
    }

    public String getId() { return id; }
    public String getBorrowingId() { return borrowingId; }
    public String getUserId() { return userId; }
    public LocalDate getDueOn() { return dueOn; }
    public LocalDate getReturnedOn() { return returnedOn; }
    public BigDecimal getAmount() { return amount; }
    public long getOverdueDays() { return overdueDays; }
}
