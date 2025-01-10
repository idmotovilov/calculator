package org.example.bankcalculator.service;

import lombok.NonNull;
import org.example.bankcalculator.dto.LoanOfferDto;
import org.example.bankcalculator.dto.LoanStatementRequestDto;

import java.util.List;

public interface CalculatorService {
    List<LoanOfferDto> salaryCalculation(@NonNull LoanStatementRequestDto requestDto);
}