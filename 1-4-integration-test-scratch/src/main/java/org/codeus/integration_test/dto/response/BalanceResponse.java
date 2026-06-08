package org.codeus.integration_test.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {

    private String userId;
    private String currency;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
}
