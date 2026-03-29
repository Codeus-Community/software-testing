package org.codeus.unit_test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private String fromAccountId;
    private String toAccountId;
    private TransactionType type;
    private BigDecimal amount;
    private Currency currency;
    private LocalDateTime timestamp;
    private String description;
    private BigDecimal fee;
}