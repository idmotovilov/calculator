package org.example.neoflexbankproject.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StatementStatusHistoryDto {
    private Enum status;              // Замените на конкретный enum, например StatementStatus
    private LocalDateTime time;
    private Enum changeType;          // Замените на конкретный enum, например ChangeType
}
