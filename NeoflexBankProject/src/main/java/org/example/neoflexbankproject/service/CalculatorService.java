package org.example.neoflexbankproject.service;

import lombok.NonNull;
import org.example.neoflexbankproject.dto.LoanOfferDto;
import org.example.neoflexbankproject.dto.LoanStatementRequestDto;

import java.util.List;

public interface CalculatorService {
    List<LoanOfferDto> salaryCalculation(@NonNull LoanStatementRequestDto requestDto);
}