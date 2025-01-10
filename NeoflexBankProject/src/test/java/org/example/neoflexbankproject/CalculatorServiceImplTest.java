package org.example.neoflexbankproject;

import org.example.neoflexbankproject.dto.LoanOfferDto;
import org.example.neoflexbankproject.dto.LoanStatementRequestDto;
import org.example.neoflexbankproject.service.impl.CalculatorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CalculatorServiceImplTest {

    @InjectMocks
    private CalculatorServiceImpl calculatorService;

    @BeforeEach
    void setUp() {
        // Если требуется какая-то общая инициализация перед каждым тестом, делаем здесь
    }

    /**
     * Базовая проверка: метод должен возвращать 4 предложения (для всех комбинаций флагов),
     * и поля в каждом предложении не должны быть null или некорректными.
     */
    @Test
    void testSalaryCalculation_baseCase() {
        // given
        LoanStatementRequestDto requestDto = new LoanStatementRequestDto(
                BigDecimal.valueOf(1_000_000),  // amount >= 20000
                12,                             // term >= 6
                "John",                         // firstName
                "Doe",                          // lastName
                "Jo",                           // middleName (опционально, но здесь указано)
                "john.doe@test.com",            // email
                LocalDate.of(1990, 1, 1),       // birthdate (должен быть в прошлом, старше 18 лет)
                "1234",                         // passportSeries (4 цифры)
                "123456"                        // passportNumber (6 цифр)
        );

        // when
        List<LoanOfferDto> offers = calculatorService.salaryCalculation(requestDto);

        // then
        assertNotNull(offers, "Список предложений не должен быть null");
        assertEquals(4, offers.size(), "Ожидается ровно 4 предложения (по комбинации флагов)");

        for (LoanOfferDto offer : offers) {
            assertNotNull(offer.statementId(), "statementId не должен быть null");
            assertEquals(requestDto.amount(), offer.requestedAmount(),
                    "Сумма кредита в предложении должна совпадать с исходным запросом");
            assertEquals(requestDto.term(), offer.term(),
                    "Срок кредита в предложении должен совпадать с исходным запросом");
            assertNotNull(offer.totalAmount(), "totalAmount не должен быть null");
            assertNotNull(offer.monthlyPayment(), "monthlyPayment не должен быть null");
            assertNotNull(offer.rate(), "rate не должен быть null");
        }
    }

    /**
     * Проверяем, что метод правильно «разворачивает» сортировку:
     * сначала сортировка по rate, затем по totalAmount, а потом список разворачивается.
     */
    @Test
    void testSalaryCalculation_checkSortingReverse() {
        // given
        LoanStatementRequestDto requestDto = new LoanStatementRequestDto(
                BigDecimal.valueOf(50_000),  // Минимум 20000
                6,                           // Минимум 6
                "Mary",
                "Smith",
                "Ma",
                "mary.smith@test.com",
                LocalDate.of(1995, 5, 5),
                "1234",
                "654321"
        );

        // when
        List<LoanOfferDto> offers = calculatorService.salaryCalculation(requestDto);

        // then
        assertNotNull(offers, "Список предложений не должен быть null");
        assertEquals(4, offers.size(), "Должны получить 4 предложения");

        // По логике: .sorted(Comparator.comparing(LoanOfferDto::rate).thenComparing(LoanOfferDto::totalAmount))
        // и затем Collections.reverse() => первое предложение в списке имеет самую большую rate (и totalAmount).
        LoanOfferDto first = offers.get(0);
        LoanOfferDto last = offers.get(offers.size() - 1);

        // Ставка первого >= ставка последнего (после реверса)
        assertTrue(first.rate().compareTo(last.rate()) >= 0,
                "Ставка у первого предложения не должна быть меньше, чем у последнего");
    }

    /**
     * Позитивный сценарий: ищем конкретное предложение (isInsuranceEnabled=false, isSalaryClient=false).
     * Ожидаемая ставка = 15% (BASE_RATE).
     * totalAmount = amount (без страховки).
     */
    @Test
    void testSalaryCalculation_insuranceFalse_salaryFalse() {
        // given
        BigDecimal amount = BigDecimal.valueOf(200_000);
        int term = 12;
        LoanStatementRequestDto requestDto = new LoanStatementRequestDto(
                amount, term,
                "Max", "Power", null,
                "max.power@test.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "654321"
        );

        // when
        List<LoanOfferDto> offers = calculatorService.salaryCalculation(requestDto);

        // then
        // Ищем предложение, где isInsuranceEnabled=false и isSalaryClient=false
        Optional<LoanOfferDto> maybeOffer = offers.stream()
                .filter(o -> Boolean.FALSE.equals(o.isInsuranceEnabled())
                        && Boolean.FALSE.equals(o.isSalaryClient()))
                .findFirst();

        assertTrue(maybeOffer.isPresent(),
                "Не найдено предложение с isInsuranceEnabled=false и isSalaryClient=false");

        LoanOfferDto offer = maybeOffer.get();

        // Ставка должна быть 15
        assertEquals(BigDecimal.valueOf(15.0), offer.rate(),
                "Ставка должна остаться базовой (15.0), если ни страховка, ни зарплатный клиент не активны");

        // totalAmount = исходная сумма
        assertEquals(amount, offer.totalAmount(),
                "totalAmount должна совпадать с запрошенной суммой (страховка не добавляется)");
    }

    /**
     * Позитивный сценарий: ищем предложение (isInsuranceEnabled=true, isSalaryClient=true).
     * Ожидаемая ставка = 15 - 3 - 1 = 11.
     * totalAmount = amount + 100000 (страховка).
     * Проверяем аннуитетный платёж.
     */
    @Test
    void testSalaryCalculation_insuranceTrue_salaryTrue() {
        // given
        BigDecimal amount = BigDecimal.valueOf(300_000);
        int term = 12;
        LoanStatementRequestDto requestDto = new LoanStatementRequestDto(
                amount, term,
                "Alice", "Cooper", "Ali",
                "alice.cooper@test.com",
                LocalDate.of(1980, 1, 1),
                "1234",
                "999999"
        );

        // when
        List<LoanOfferDto> offers = calculatorService.salaryCalculation(requestDto);

        // then
        // Ищем предложение, где isInsuranceEnabled=true и isSalaryClient=true
        Optional<LoanOfferDto> maybeOffer = offers.stream()
                .filter(o -> Boolean.TRUE.equals(o.isInsuranceEnabled())
                        && Boolean.TRUE.equals(o.isSalaryClient()))
                .findFirst();

        assertTrue(maybeOffer.isPresent(),
                "Не найдено предложение с isInsuranceEnabled=true и isSalaryClient=true");

        LoanOfferDto offer = maybeOffer.get();

        // Ставка должна быть 11.0 (15 - 3 - 1)
        assertEquals(BigDecimal.valueOf(11.0), offer.rate(),
                "Ставка должна быть 11.0 при включённой страховке и зарплатном клиенте");

        // totalAmount = исходная сумма + 100_000 (страховка)
        BigDecimal expectedTotalAmount = amount.add(BigDecimal.valueOf(100_000));
        assertEquals(expectedTotalAmount, offer.totalAmount(),
                "totalAmount должна включать страховку в 100 000");

        // Проверим ежемесячный платёж (аннуитетная формула).
        // Для справки: monthlyRate = 11% годовых / 12
        double annualRate = 11.0;
        double monthlyRate = annualRate / 100 / 12;
        double pow = Math.pow(1 + monthlyRate, term);
        double numerator = expectedTotalAmount.doubleValue() * pow * monthlyRate;
        double denominator = pow - 1;
        double annuity = numerator / denominator;

        BigDecimal expectedMonthlyPayment = BigDecimal.valueOf(annuity).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, offer.monthlyPayment().compareTo(expectedMonthlyPayment),
                "Ежемесячный платёж не совпадает с ожидаемым (аннуитетная схема).");
    }
}
