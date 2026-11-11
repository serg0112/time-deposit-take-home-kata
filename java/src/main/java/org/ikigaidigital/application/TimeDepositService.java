package org.ikigaidigital.application;

import org.ikigaidigital.TimeDepositCalculator;

import java.util.List;

public class TimeDepositService {
    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    public TimeDepositService(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    public List<DepositDetails> findAll() {
        return repository.findAll();
    }

    public void updateBalances() {
        repository.updateBalances(calculator::updateBalance);
    }
}
