package org.ikigaidigital.application;

import lombok.RequiredArgsConstructor;
import org.ikigaidigital.adapter.persistence.TimeDepositRepository;
import org.ikigaidigital.adapter.persistence.data.TimeDepositEntity;
import org.ikigaidigital.adapter.persistence.data.WithdrawalEntity;
import org.ikigaidigital.adapter.rest.response.DepositDetails;
import org.ikigaidigital.domain.TimeDeposit;
import org.ikigaidigital.domain.TimeDepositCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeDepositService {
    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    public List<DepositDetails> findAll() {
        return repository.findAllWithWithdrawals().stream().map(this::toDetails).toList();
    }

    @Transactional
    public void updateBalances() {
        // One call is one accrual. Days stay unchanged; balances already reflect historical withdrawals.
        List<TimeDepositEntity> entities = repository.findAllForUpdate();
        List<TimeDeposit> deposits = entities.stream().map(this::toTimeDeposit).toList();
        calculator.updateBalance(deposits);
        // Mapping preserves order, and the calculator changes balances without reordering the batch.
        // BigDecimal.valueOf keeps the shortest decimal for the legacy double result (the calculator
        // deliberately uses new BigDecimal(double) for rounding). The locked entities are managed, so
        // dirty checking persists these balances when the transaction commits; no explicit save is needed.
        for (int index = 0; index < entities.size(); index++) {
            entities.get(index).setBalance(BigDecimal.valueOf(deposits.get(index).getBalance()));
        }
    }

    private TimeDeposit toTimeDeposit(TimeDepositEntity entity) {
        return new TimeDeposit(entity.getId(), entity.getPlanType(),
            entity.getBalance().doubleValue(), entity.getDays());
    }

    private DepositDetails toDetails(TimeDepositEntity entity) {
        List<DepositDetails.Withdrawal> withdrawals = entity.getWithdrawals().stream()
            .map(this::toWithdrawal).toList();
        return new DepositDetails(entity.getId(), entity.getPlanType(), entity.getBalance(),
            entity.getDays(), withdrawals);
    }

    private DepositDetails.Withdrawal toWithdrawal(WithdrawalEntity entity) {
        return new DepositDetails.Withdrawal(entity.getId(), entity.getTimeDeposit().getId(),
            entity.getAmount(), entity.getDate());
    }
}
