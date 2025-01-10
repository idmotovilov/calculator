package org.example.neoflexbankproject.service;

import org.example.neoflexbankproject.dto.CreditDto;
import org.example.neoflexbankproject.dto.ScoringDataDto;

public interface CreditCalculationService {
    CreditDto calculateCredit(ScoringDataDto scoringDataDto);
}
