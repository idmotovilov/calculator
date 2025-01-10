package org.example.bankcalculator.dto;

import lombok.Builder;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CreditDto(
        @NonNull
        BigDecimal amount,
        Integer term,
        @NonNull
        BigDecimal monthlyPayment,
        @NonNull
        BigDecimal rate,
        @NonNull
        BigDecimal psk,
        Boolean isInsuranceEnabled,
        Boolean isSalaryClient,
        List<PaymentScheduleElementDto> paymentSchedule
) {}
