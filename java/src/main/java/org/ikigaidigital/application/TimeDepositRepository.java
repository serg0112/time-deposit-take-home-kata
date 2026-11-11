package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;

import java.util.List;
import java.util.function.Consumer;

public interface TimeDepositRepository {
    List<DepositDetails> findAll();

    /** Load, update and persist all balances atomically, excluding concurrent balance updates. */
    void updateBalances(Consumer<List<TimeDeposit>> update);
}
