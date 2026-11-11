package org.ikigaidigital.domain;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class TimeDepositCalculator {
    private static final InterestRule NO_INTEREST = deposit -> 0;
    private final Map<String, InterestRule> rules;

    public TimeDepositCalculator() {
        this(Map.of(
            "basic", InterestRule.annualRate(0.01, days -> true),
            "student", InterestRule.annualRate(0.03, days -> days < 366),
            "premium", InterestRule.annualRate(0.05, days -> days > 45)
        ));
    }

    public TimeDepositCalculator(Map<String, InterestRule> rules) {
        this.rules = Map.copyOf(rules);
    }

    public void updateBalance(List<TimeDeposit> deposits) {
        for (TimeDeposit deposit : deposits) {
            double interest = deposit.getDays() > 30
                ? rules.getOrDefault(deposit.getPlanType(), NO_INTEREST).monthlyInterest(deposit) : 0;
            // Keep BigDecimal(double), operation order and balance addition for legacy compatibility.
            double roundedInterest = new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP).doubleValue();
            deposit.setBalance(deposit.getBalance() + roundedInterest);
        }
    }
}
