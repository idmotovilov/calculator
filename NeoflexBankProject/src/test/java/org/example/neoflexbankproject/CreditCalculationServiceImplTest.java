package org.example.neoflexbankproject;

import org.example.neoflexbankproject.dto.CreditDto;
import org.example.neoflexbankproject.dto.PaymentScheduleElementDto;
import org.example.neoflexbankproject.dto.ScoringDataDto;
import org.example.neoflexbankproject.service.impl.CreditCalculationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditCalculationServiceImplTest {

    private CreditCalculationServiceImpl service;

    @BeforeEach
    void setUp() {
        // Создаём реальный сервис
        service = new CreditCalculationServiceImpl();
    }

    /**
     * Тест реальной логики: страховка + зарплата => -4% к ставке, +10000 к сумме.
     */
    @Test
    void testCalculateCredit_RealLogic() {
        ScoringDataDto scoringData = new ScoringDataDto(
                BigDecimal.valueOf(50000),
                12,
                "John",
                "Doe",
                "Smith",
                LocalDate.of(1990,1,1),
                "1234",
                "567890",
                true,
                true
        );

        CreditDto credit = service.calculateCredit(scoringData);

        assertNotNull(credit);
        assertEquals("51000", credit.amount().toPlainString());
        assertEquals("11.0", credit.rate().toPlainString());
        assertTrue(credit.monthlyPayment().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(credit.psk().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(12, credit.paymentSchedule().size());
    }

    /**
     * Тест с использованием Spy: проверяем, что внутренние методы вызываются
     * и в правильном порядке (теперь вызов calculateMonthlyPayment будет 1 раз).
     */
    @Test
    void testCalculateCredit_WithSpyAndVerify() {
        // Создаём spy
        CreditCalculationServiceImpl spyService = spy(service);

        ScoringDataDto scoringData = new ScoringDataDto(
                BigDecimal.valueOf(30000),
                6,
                "Alice",
                "Wonderland",
                null,
                LocalDate.of(1985, 3, 15),
                "1111",
                "222222",
                false,
                false
        );

        // Подменяем calculateMonthlyPayment => 9999.99
        when(spyService.calculateMonthlyPayment(any(BigDecimal.class), eq(6), anyDouble()))
                .thenReturn(BigDecimal.valueOf(9999.99));

        // Act
        CreditDto credit = spyService.calculateCredit(scoringData);

        // Assert
        assertNotNull(credit);
        // monthlyPayment => "9999.99"
        assertEquals("9999.99", credit.monthlyPayment().toPlainString());

        // Поскольку generatePaymentSchedule(...)
        // теперь НЕ вызывает повторно calculateMonthlyPayment(...),
        // мы ожидаем, что calculateMonthlyPayment(...) был вызван ровно 1 раз
        verify(spyService, times(1)).calculateMonthlyPayment(any(BigDecimal.class), eq(6), anyDouble());
        // calculatePSK(...) => тоже 1 раз
        verify(spyService, times(1)).calculatePSK(any(BigDecimal.class), eq(6), anyDouble());
        // generatePaymentSchedule(...) => 1 раз
        verify(spyService, times(1)).generatePaymentSchedule(any(BigDecimal.class), eq(6), anyDouble(), eq(BigDecimal.valueOf(9999.99)));

        // Проверка порядка:
        InOrder inOrder = inOrder(spyService);

        // 1) calculateMonthlyPayment
        inOrder.verify(spyService).calculateMonthlyPayment(any(BigDecimal.class), eq(6), anyDouble());
        // 2) calculatePSK
        inOrder.verify(spyService).calculatePSK(any(BigDecimal.class), eq(6), anyDouble());
        // 3) generatePaymentSchedule
        // обратите внимание на четвертый параметр (monthlyPayment)
        inOrder.verify(spyService).generatePaymentSchedule(any(BigDecimal.class), eq(6), anyDouble(), eq(BigDecimal.valueOf(9999.99)));
    }

    /**
     * Сценарий со страховкой, но без зарплатного клиента => -3% к ставке, +10000 к сумме.
     */
    @Test
    void testCalculateCredit_InsuranceNoSalary() {
        ScoringDataDto scoringData = new ScoringDataDto(
                BigDecimal.valueOf(49000),
                10,
                "Bob",
                "Marley",
                "G",
                LocalDate.of(1970,1,1),
                "3333",
                "888888",
                true,
                false
        );

        CreditDto credit = service.calculateCredit(scoringData);

        assertEquals("50000", credit.amount().toPlainString());
        assertEquals("12.0", credit.rate().toPlainString());
        assertTrue(credit.monthlyPayment().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(10, credit.paymentSchedule().size());
    }

    /**
     * Проверяем fallback при term=0 => monthlyPayment=0.00, schedule=empty.
     */
    @Test
    void testCalculateCredit_ZeroOrNegativeRateFallback() {
        ScoringDataDto request = new ScoringDataDto(
                BigDecimal.valueOf(50000),
                0,
                "Sam",
                "Smith",
                null,
                LocalDate.of(1980,10,10),
                "0000",
                "123123",
                true,
                true
        );

        CreditDto credit = service.calculateCredit(request);

        assertNotNull(credit);
        assertEquals("0.00", credit.monthlyPayment().toPlainString());
        assertTrue(credit.paymentSchedule().isEmpty());
    }
}
