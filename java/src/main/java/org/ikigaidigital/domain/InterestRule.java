package org.ikigaidigital.domain;

import java.util.function.IntPredicate;

/** Calculates unrounded interest for one monthly update. */
@FunctionalInterface
public interface InterestRule {
    double monthlyInterest(TimeDeposit deposit);

    static InterestRule annualRate(double rate, IntPredicate eligibleDays) {
        // Preserve the working calculator: each monthly accrual uses the quoted rate divided by 12.
        return deposit -> eligibleDays.test(deposit.getDays())
            ? deposit.getBalance() * rate / 12 : 0;
    }
}
