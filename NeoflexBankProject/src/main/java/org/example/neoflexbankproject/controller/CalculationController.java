package org.example.neoflexbankproject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.neoflexbankproject.api.contract.CalculationApi;
import org.example.neoflexbankproject.dto.CreditDto;
import org.example.neoflexbankproject.dto.LoanOfferDto;
import org.example.neoflexbankproject.dto.LoanStatementRequestDto;
import org.example.neoflexbankproject.dto.ScoringDataDto;
import org.example.neoflexbankproject.service.CalculatorService;
import org.example.neoflexbankproject.service.CreditCalculationService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Контроллер расчёта условий кредита.
 */
@Slf4j
@RestController
@RequestMapping("/calculator")
@RequiredArgsConstructor

public class CalculationController implements CalculationApi {

    private final CalculatorService calculatorService;
    private final CreditCalculationService creditCalculationService;

    /**
     * Расчёт возможных условий кредита (4 варианта).
     * POST /calculator/offers
     */
    @PostMapping("/offers")
    @ResponseStatus(HttpStatus.OK)
    public List<LoanOfferDto> offer(@RequestBody @Valid LoanStatementRequestDto requestDto) {
        log.debug("Received request for offer: {}", requestDto);
        return calculatorService.salaryCalculation(requestDto);
    }

    /**
     * Расчёт финальных параметров кредита (аннуитет, ПСК, график).
     * POST /calculator/calc
     */
    @PostMapping("/calc")
    @ResponseStatus(HttpStatus.OK)
    public CreditDto calc(@RequestBody @Valid ScoringDataDto requestDto) {
        log.debug("Received request for calc: {}", requestDto);
        return creditCalculationService.calculateCredit(requestDto);
    }

    /**
     * Обработчик ошибок валидации (Bean Validation).
     * Возвращает структурированный JSON с полем errors при MethodArgumentNotValidException.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, List<String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Собираем все ошибки валидации в список строк
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> (error instanceof FieldError fe)
                        ? (fe.getField() + ": " + fe.getDefaultMessage())
                        : error.getDefaultMessage())
                .collect(Collectors.toList());

        // Создаём карту с ключом "errors"
        Map<String, List<String>> errorResponse = new HashMap<>();
        errorResponse.put("errors", errors);

        log.warn("Validation failed: {}", errors);
        return errorResponse;
    }
}