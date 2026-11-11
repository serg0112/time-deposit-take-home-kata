package org.ikigaidigital.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DepositDetails(int id, String planType, BigDecimal balance, int days,
                             List<Withdrawal> withdrawals) {
    public DepositDetails {
        withdrawals = List.copyOf(withdrawals);
    }

    public record Withdrawal(int id, int timeDepositId, BigDecimal amount, LocalDate date) {}
}
