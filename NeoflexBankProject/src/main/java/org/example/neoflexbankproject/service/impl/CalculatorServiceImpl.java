package org.example.neoflexbankproject.service.impl;

import io.micrometer.common.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.neoflexbankproject.dto.LoanOfferDto;
import org.example.neoflexbankproject.dto.LoanStatementRequestDto;
import org.example.neoflexbankproject.service.CalculatorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor

public class CalculatorServiceImpl implements CalculatorService {

    private static final double BASE_RATE = 15.0; // Захардкодил значение прямо в коде

    private static final BigDecimal INSURANCE_COST = BigDecimal.valueOf(100000);

    @Override
    public List<LoanOfferDto> salaryCalculation(@NonNull LoanStatementRequestDto requestDto) {
        log.debug("Starting salaryCalculation with requestDto: {}", requestDto);

        List<boolean[]> flags = List.of(
                new boolean[]{false, false},
                new boolean[]{false, true},
                new boolean[]{true, false},
                new boolean[]{true, true}
        );

        UUID statementId = UUID.randomUUID();
        log.debug("Generated statementId: {}", statementId);

        List<LoanOfferDto> offers = flags.stream()
                .map(flag -> createOffer(requestDto, statementId, flag[0], flag[1]))
                .sorted(Comparator.comparing(LoanOfferDto::rate).thenComparing(LoanOfferDto::totalAmount))
                .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), list -> {
                    java.util.Collections.reverse(list);
                    log.debug("Reversed and sorted loan offers: {}", list);
                    return list;
                }));

        log.debug("Final loan offers: {}", offers);
        return offers;
    }

    private LoanOfferDto createOffer(LoanStatementRequestDto r, UUID statementId,
                                     boolean isInsuranceEnabled, boolean isSalaryClient) {
        log.debug("Creating offer with isInsuranceEnabled={}, isSalaryClient={}", isInsuranceEnabled, isSalaryClient);
        double finalRate = BASE_RATE;
        BigDecimal totalAmount = r.amount();

        if (isInsuranceEnabled) {
            totalAmount = totalAmount.add(INSURANCE_COST);
            finalRate -= 3.0;
            log.debug("Insurance enabled: Added {} to totalAmount, decreased rate by 3.0", INSURANCE_COST);
        }

        if (isSalaryClient) {
            finalRate -= 1.0;
            log.debug("Salary client: Decreased rate by 1.0");
        }

        BigDecimal monthlyPayment = calculateMonthlyPayment(totalAmount, r.term(), finalRate);
        log.debug("Calculated monthlyPayment: {}", monthlyPayment);

        LoanOfferDto offer = LoanOfferDto.builder()
                .statementId(statementId)
                .requestedAmount(r.amount())
                .totalAmount(totalAmount)
                .term(r.term())
                .monthlyPayment(monthlyPayment)
                .rate(BigDecimal.valueOf(finalRate))
                .isInsuranceEnabled(isInsuranceEnabled)
                .isSalaryClient(isSalaryClient)
                .build();

        log.debug("Created LoanOfferDto: {}", offer);
        return offer;
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, int term, double finalRate) {
        log.debug("Calculating monthly payment with totalAmount={}, term={}, finalRate={}", totalAmount, term, finalRate);
        double monthlyRate = finalRate / 100 / 12;
        BigDecimal payment;
        if (monthlyRate > 0) {
            BigDecimal r = BigDecimal.valueOf(monthlyRate);
            BigDecimal numerator = totalAmount.multiply(r.add(BigDecimal.ONE).pow(term)).multiply(r);
            BigDecimal denominator = (r.add(BigDecimal.ONE).pow(term)).subtract(BigDecimal.ONE);
            payment = numerator.divide(denominator, 2, RoundingMode.HALF_UP);
            log.debug("Calculated monthly payment (annuity): {}", payment);
        } else {
            payment = totalAmount.divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_UP);
            log.debug("Calculated monthly payment (zero rate): {}", payment);
        }
        return payment;
    }
}
