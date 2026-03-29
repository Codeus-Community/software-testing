package org.codeus.unit_test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private String id;
    private String clientId;
    private AccountType type;
    private Currency currency;
    private BigDecimal balance;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private BigDecimal dailyWithdrawalLimit;
}