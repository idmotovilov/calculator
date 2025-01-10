package org.example.neoflexbankproject.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.neoflexbankproject.dto.CreditDto;
import org.example.neoflexbankproject.dto.PaymentScheduleElementDto;
import org.example.neoflexbankproject.dto.ScoringDataDto;
import org.example.neoflexbankproject.service.CreditCalculationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для расчёта кредита по аннуитетной схеме.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCalculationServiceImpl implements CreditCalculationService {

    private static final double DEFAULT_BASE_RATE = 15.0;
    private final double baseRate = DEFAULT_BASE_RATE;

    private static final BigDecimal INSURANCE_COST = BigDecimal.valueOf(1000);

    @Override
    public CreditDto calculateCredit(ScoringDataDto scoringDataDto) {
        log.debug("Starting calculateCredit with scoringDataDto: {}", scoringDataDto);

        // Подстраховка от null
        BigDecimal totalAmount = scoringDataDto.amount();
        if (totalAmount == null) {
            log.warn("ScoringDataDto.amount is null => fallback => 0.00");
            totalAmount = BigDecimal.ZERO;
        }

        double finalRate = baseRate;

        // Страховка
        if (Boolean.TRUE.equals(scoringDataDto.isInsuranceEnabled())) {
            totalAmount = totalAmount.add(INSURANCE_COST);
            finalRate -= 3.0;
            log.debug("Insurance => +{} => totalAmount={}, finalRate={}", INSURANCE_COST, totalAmount, finalRate);
        }

        // Зарплатный клиент
        if (Boolean.TRUE.equals(scoringDataDto.isSalaryClient())) {
            finalRate -= 1.0;
            log.debug("Salary client => finalRate={}", finalRate);
        }

        // 1) Расчёт ежемесячного платежа
        BigDecimal monthlyPayment = calculateMonthlyPayment(totalAmount, scoringDataDto.term(), finalRate);

        // 2) Расчёт ПСК (упрощённая формула)
        BigDecimal psk = calculatePSK(totalAmount, scoringDataDto.term(), finalRate);

        // 3) Генерация графика платежей (теперь мы передаём уже рассчитанный monthlyPayment!)
        List<PaymentScheduleElementDto> schedule = generatePaymentSchedule(totalAmount,
                scoringDataDto.term(),
                finalRate,
                monthlyPayment);

        // Итоговый результат
        CreditDto credit = CreditDto.builder()
                .amount(totalAmount)
                .term(scoringDataDto.term())
                .monthlyPayment(monthlyPayment)
                .rate(BigDecimal.valueOf(finalRate))
                .psk(psk)
                .isInsuranceEnabled(scoringDataDto.isInsuranceEnabled())
                .isSalaryClient(scoringDataDto.isSalaryClient())
                .paymentSchedule(schedule)
                .build();

        log.debug("Created CreditDto => {}", credit);
        return credit;
    }

    /**
     * Аннуитетная формула:
     * P = S × ( i + i / ((1 + i)^n - 1) ),
     * где:
     *   S=totalAmount,
     *   i=(finalRate / 100 / 12),
     *   n=term
     * Fallback при term<=0 => 0.00
     */
    public BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, int term, double finalRate) {
        log.debug("calculateMonthlyPayment => totalAmount={}, term={}, finalRate={}", totalAmount, term, finalRate);

        // Fallback, если term <= 0
        if (term <= 0) {
            log.warn("term <= 0 => monthlyPayment=0.00");
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        double monthlyRateDouble = finalRate / 100.0 / 12.0;
        BigDecimal i = BigDecimal.valueOf(monthlyRateDouble);

        if (i.compareTo(BigDecimal.ZERO) <= 0) {
            // Если ставка <=0, делим сумму на срок
            BigDecimal zeroRatePayment = totalAmount.divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_UP);
            log.debug("Rate<=0 => zeroRatePayment => {}", zeroRatePayment);
            return zeroRatePayment;
        }

        // (1 + i)^n
        BigDecimal powTerm = i.add(BigDecimal.ONE).pow(term);
        // (1 + i)^n - 1
        BigDecimal denominator = powTerm.subtract(BigDecimal.ONE);

        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("denominator=0 => fallback => totalAmount/term");
            return totalAmount.divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_UP);
        }

        // i / ((1 + i)^n - 1)
        BigDecimal iOverDenominator = i.divide(denominator, 8, RoundingMode.HALF_UP);

        // аннуитетная часть
        BigDecimal annuityPart = i.add(iOverDenominator);

        BigDecimal monthlyPayment = totalAmount.multiply(annuityPart).setScale(2, RoundingMode.HALF_UP);
        log.debug("monthlyPayment => {}", monthlyPayment);
        return monthlyPayment;
    }

    /**
     * Упрощенная формула PSK => finalRate * 1.2
     */
    public BigDecimal calculatePSK(BigDecimal totalAmount, int term, double finalRate) {
        log.debug("calculatePSK => totalAmount={}, term={}, finalRate={}", totalAmount, term, finalRate);
        BigDecimal psk = BigDecimal.valueOf(finalRate).multiply(BigDecimal.valueOf(1.2))
                .setScale(2, RoundingMode.HALF_UP);
        log.debug("psk => {}", psk);
        return psk;
    }

    /**
     * Генерация графика платежей на основе monthlyPayment,
     * чтобы не вызывать повторно calculateMonthlyPayment(...) внутри.
     */
    public List<PaymentScheduleElementDto> generatePaymentSchedule(BigDecimal totalAmount,
                                                                   int term,
                                                                   double finalRate,
                                                                   BigDecimal monthlyPayment) {
        log.debug("generatePaymentSchedule => totalAmount={}, term={}, finalRate={}, monthlyPayment={}",
                totalAmount, term, finalRate, monthlyPayment);

        List<PaymentScheduleElementDto> schedule = new ArrayList<>();

        // Если monthlyPayment=0 или term<=0 => пустой график
        if (monthlyPayment.compareTo(BigDecimal.ZERO) == 0 || term <= 0) {
            log.warn("monthlyPayment=0 or term<=0 => empty schedule");
            return schedule;
        }

        double monthlyRateDouble = finalRate / 100.0 / 12.0;
        BigDecimal monthlyRate = BigDecimal.valueOf(monthlyRateDouble);

        BigDecimal debtRemaining = totalAmount;
        for (int i = 1; i <= term; i++) {
            BigDecimal interestPayment = debtRemaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPayment = monthlyPayment.subtract(interestPayment).max(BigDecimal.ZERO);
            debtRemaining = debtRemaining.subtract(principalPayment).max(BigDecimal.ZERO);

            PaymentScheduleElementDto element = PaymentScheduleElementDto.builder()
                    .paymentDate(LocalDate.now().plusMonths(i))
                    .paymentAmount(monthlyPayment)
                    .interestPayment(interestPayment)
                    .principalPayment(principalPayment)
                    .remainingDebt(debtRemaining)
                    .build();

            schedule.add(element);
            log.debug("Added PaymentScheduleElementDto => {}", element);
        }
        return schedule;
    }
}
