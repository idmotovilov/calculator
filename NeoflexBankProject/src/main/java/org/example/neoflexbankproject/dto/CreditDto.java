package org.example.neoflexbankproject.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

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
