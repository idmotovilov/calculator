package org.example.neoflexbankproject;

import com.fasterxml.jackson.databind.ObjectMapper;


import org.example.neoflexbankproject.controller.CalculationController;
import org.example.neoflexbankproject.dto.*;
import org.example.neoflexbankproject.service.CalculatorService;
import org.example.neoflexbankproject.service.CreditCalculationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(SpringExtension.class)
@WebMvcTest(CalculationController.class)
class CalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalculatorService calculatorService;

    @MockBean
    private CreditCalculationService creditCalculationService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Сценарий: невалидные поля => ожидаем 400 BAD_REQUEST.
     */




    @Test
    @DisplayName("offer(): невалидные поля => 400 BAD_REQUEST")
   void testOfferValidationError() throws Exception {
        // Делаем заведомо "кривой" LoanStatementRequestDto
        LoanStatementRequestDto invalidRequest = new LoanStatementRequestDto(
                BigDecimal.valueOf(10_000), // <20000 => DecimalMin нарушена
                3,                          // <6 => Min нарушена
                "J",                        // Имя <2 символов => Pattern
                "D",                        // Фамилия <2 символов => Pattern
                "!",                        // Отчество не вписывается в [A-Za-z]{2,30}
                "bad_email_format",         // Неверный email
                LocalDate.now(),           // не в прошлом => @Past + не >=18
                "12",                       // Серия !=4 цифр
                "12345"                     // Номер !=6 цифр
        );

        mockMvc.perform(post("/calculator/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()) // ожидаем 400
                .andExpect(content().string(containsString("Сумма кредита должна быть не меньше 20000")))
                .andExpect(content().string(containsString("Срок кредита должен быть не меньше 6 месяцев")))
                .andExpect(content().string(containsString("Имя должно содержать от 2 до 30 латинских букв")))
                .andExpect(content().string(containsString("Фамилия должна содержать от 2 до 30 латинских букв")))
                .andExpect(content().string(containsString("Отчество должно содержать от 2 до 30 латинских букв")))
                .andExpect(content().string(containsString("Неверный формат email")))
                .andExpect(content().string(containsString("Дата рождения должна быть в прошлом")))
                .andExpect(content().string(containsString("Серия паспорта должна состоять из 4 цифр")))
                .andExpect(content().string(containsString("Номер паспорта должен состоять из 6 циф")))
                .andExpect(content().string(containsString("Клиент должен быть старше 18 лет")));
    }

    /**
     * Сценарий: валидные поля => ожидаем 200 OK.
     */
    @Test
    @DisplayName("offer(): валидные поля => 200 OK")
    void testOfferSuccess() throws Exception {
        // Мокаем, чтобы сервис вернул список из 1 LoanOfferDto
        LoanOfferDto fakeOffer = LoanOfferDto.builder()
                .requestedAmount(BigDecimal.valueOf(500_000))
                .totalAmount(BigDecimal.valueOf(600_000))
                .term(12)
                .monthlyPayment(BigDecimal.valueOf(50_000))
                .rate(BigDecimal.valueOf(15.0))
                .isInsuranceEnabled(false)
                .isSalaryClient(false)
                .build();
        Mockito.when(calculatorService.salaryCalculation(any(LoanStatementRequestDto.class)))
                .thenReturn(List.of(fakeOffer));

        // Абсолютно валидный LoanStatementRequestDto
        LoanStatementRequestDto validRequest = new LoanStatementRequestDto(
                BigDecimal.valueOf(500_000),
                12,
                "John",
                "Doe",
                "Johann",
                "john.doe@test.com",
                LocalDate.of(1985, 1, 1),
                "1234",
                "123456"
        );

        mockMvc.perform(post("/calculator/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Проверяем, что список содержит 1 элемент
                .andExpect(jsonPath("$.length()").value(1))
                // Проверяем содержимое первого элемента
                .andExpect(jsonPath("$[0].requestedAmount").value(500000))
                .andExpect(jsonPath("$[0].totalAmount").value(600000))
                .andExpect(jsonPath("$[0].rate").value(15.0));
    }
}
