package org.ikigaidigital;

import java.util.function.IntPredicate;

/** Calculates unrounded interest for one monthly update. */
@FunctionalInterface
public interface InterestRule {
    double monthlyInterest(TimeDeposit deposit);

    static InterestRule annualRate(double rate, IntPredicate eligibleDays) {
        return deposit -> eligibleDays.test(deposit.getDays())
            ? deposit.getBalance() * rate / 12 : 0;
    }
}
