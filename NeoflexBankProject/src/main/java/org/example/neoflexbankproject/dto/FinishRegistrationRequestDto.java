package org.example.neoflexbankproject.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter

public class FinishRegistrationRequestDto {
    private Enum gender;              // Замените на конкретный enum, например Gender
    private Enum maritalStatus;       // Замените на конкретный enum, например MaritalStatus
    private Integer dependentAmount;
    private LocalDate passportIssueDate;
    private String passportIssueBranch;
    private EmploymentDto employment;
    private String accountNumber;
}