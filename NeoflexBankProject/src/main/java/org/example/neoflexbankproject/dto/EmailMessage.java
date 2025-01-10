package org.example.neoflexbankproject.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailMessage {
    private String address;
    private Enum theme;               // Замените на конкретный enum, например EmailTheme
    private Long statementId;
    private String text;
}