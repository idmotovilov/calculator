package org.example.neoflexbankproject.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EmploymentDto {
    private Enum employmentStatus;    // Замените на конкретный enum, например EmploymentStatus
    private String employerINN;
    private BigDecimal salary;
    private Enum position;            // Замените на конкретный enum, например Position
    private Integer workExperienceTotal;
    private Integer workExperienceCurrent;
}
