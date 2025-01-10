package org.example.bankcalculator.service;

import org.example.bankcalculator.dto.CreditDto;
import org.example.bankcalculator.dto.ScoringDataDto;

public interface CreditCalculationService {
    CreditDto calculateCredit(ScoringDataDto scoringDataDto);
}
