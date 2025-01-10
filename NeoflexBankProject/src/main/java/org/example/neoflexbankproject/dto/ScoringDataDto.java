package org.example.neoflexbankproject.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

// Допустим, поля gender, maritalStatus, и другие не указаны чётко,
// Валидацию делаем аналогично, если нужно.

public record ScoringDataDto(
        @NotNull(message = "Сумма кредита не может быть null")
        @DecimalMin(value = "20000", message = "Сумма кредита должна быть не меньше 20000")
        BigDecimal amount,

        @NotNull(message = "Срок кредита не может быть null")
        @Min(value = 6, message = "Срок кредита должен быть не меньше 6 месяцев")
        Integer term,

        @NotNull(message = "Имя не может быть null")
        @Pattern(regexp = "^[A-Za-z]{2,30}$", message = "Имя должно содержать от 2 до 30 латинских букв")
        String firstName,

        @NotNull(message = "Фамилия не может быть null")
        @Pattern(regexp = "^[A-Za-z]{2,30}$", message = "Фамилия должна содержать от 2 до 30 латинских букв")
        String lastName,

        @Pattern(regexp = "^[A-Za-z]{2,30}$", message = "Отчество должно содержать от 2 до 30 латинских букв")
        String middleName,

        @NotNull(message = "Дата рождения не может быть null")
        @Past(message = "Дата рождения должна быть в прошлом")
        LocalDate birthdate,

        @NotNull(message = "Серия паспорта не может быть null")
        @Pattern(regexp = "^\\d{4}$", message = "Серия паспорта должна состоять из 4 цифр")
        String passportSeries,

        @NotNull(message = "Номер паспорта не может быть null")
        @Pattern(regexp = "^\\d{6}$", message = "Номер паспорта должен состоять из 6 цифр")
        String passportNumber,

        // Дополнительные поля, если они есть, также помечаем аннотациями по необходимости:
        Boolean isInsuranceEnabled,
        Boolean isSalaryClient
) {
        @AssertTrue(message = "Клиент должен быть старше 18 лет")
        public boolean isAdult() {
                return birthdate != null && Period.between(birthdate, LocalDate.now()).getYears() >= 18;
        }
}
