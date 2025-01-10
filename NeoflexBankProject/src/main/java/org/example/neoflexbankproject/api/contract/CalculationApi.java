package org.example.neoflexbankproject.api.contract;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.neoflexbankproject.dto.CreditDto;
import org.example.neoflexbankproject.dto.LoanOfferDto;
import org.example.neoflexbankproject.dto.LoanStatementRequestDto;
import org.example.neoflexbankproject.dto.ScoringDataDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/calculator")
@Tag(name = "Расчёт возможных условий кредита и скоринг данных", description = "API для расчёта условий кредита и выполнения скоринга")
public interface CalculationApi {

    @Operation(summary = "Расчёт возможных условий кредита",
            description = "Принимает данные о заявлении на кредит и возвращает список из 4 предложений по разным комбинациям страховки и зарплатного клиента.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос обработан успешно"),
            @ApiResponse(responseCode = "400", description = "Запрос не прошёл валидацию"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервиса")
    })
    @PostMapping("/offers")
    List<LoanOfferDto> offer(
            @Parameter(description = "Данные заявления на кредит", required = true)
            @RequestBody @Valid LoanStatementRequestDto requestDto
    );

    @Operation(summary = "Расчёт итоговых условий кредита",
            description = "Принимает данные для скоринга и возвращает подробные параметры кредита, включая график платежей.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос обработан успешно"),
            @ApiResponse(responseCode = "400", description = "Запрос не прошёл валидацию"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервиса")
    })
    @PostMapping("/calc")
    CreditDto calc(
            @Parameter(description = "Данные для скоринга", required = true)
            @RequestBody @Valid ScoringDataDto requestDto
    );
}
