package org.example.neoflexbankproject.dto;

import lombok.Builder;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record PaymentScheduleElementDto(
        @NonNull LocalDate paymentDate,
        @NonNull BigDecimal paymentAmount,
        @NonNull BigDecimal interestPayment,
        @NonNull BigDecimal principalPayment,
        @NonNull BigDecimal remainingDebt
) {
}