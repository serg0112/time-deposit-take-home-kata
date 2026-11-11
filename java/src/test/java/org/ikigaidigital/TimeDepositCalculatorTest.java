package org.ikigaidigital;

import org.ikigaidigital.domain.TimeDeposit;
import org.ikigaidigital.domain.TimeDepositCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDepositCalculatorTest {
    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @ParameterizedTest(name = "{0} at day {1} has balance {2}")
    @CsvSource({
        "basic,29,1200.00", "basic,30,1200.00", "basic,31,1201.00", "basic,366,1201.00",
        "student,29,1200.00", "student,30,1200.00", "student,31,1203.00",
        "student,364,1203.00", "student,365,1203.00", "student,366,1200.00", "student,367,1200.00",
        "premium,29,1200.00", "premium,30,1200.00", "premium,31,1200.00",
        "premium,44,1200.00", "premium,45,1200.00", "premium,46,1205.00", "premium,366,1205.00",
        "unknown,100,1200.00", "Basic,100,1200.00"
    })
    void appliesInterestOnlyWithinThePlanWindow(String plan, int days, double expectedBalance) {
        TimeDeposit deposit = new TimeDeposit(7, plan, 1200.0, days);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(expectedBalance);
        assertThat(deposit.getId()).isEqualTo(7);
        assertThat(deposit.getPlanType()).isEqualTo(plan);
        assertThat(deposit.getDays()).isEqualTo(days);
    }

    @ParameterizedTest(name = "balance {0} rounds to {1}")
    @CsvSource({"1000,1000.83", "6,6.01", "18,18.01", "30,30.02", "0,0"})
    void preservesRoundingOfTheOriginalDoubleBasedCalculation(double balance, double expectedBalance) {
        // 18 * .01 / 12 is slightly below .015 as a double. Preserve that legacy result.
        TimeDeposit deposit = new TimeDeposit(1, "basic", balance, 31);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(expectedBalance);
    }

    @Test
    void calculatesEachDepositIndependentlyInTheSameBatch() {
        List<TimeDeposit> deposits = List.of(
            new TimeDeposit(1, "student", 1200.0, 365),
            new TimeDeposit(2, "student", 1200.0, 366),
            new TimeDeposit(3, "premium", 2400.0, 46),
            new TimeDeposit(4, "unknown", 1200.0, 100),
            new TimeDeposit(5, "basic", 1200.0, 30),
            new TimeDeposit(6, "basic", 600.0, 31)
        );

        calculator.updateBalance(deposits);

        assertThat(deposits).extracting(TimeDeposit::getBalance)
            .containsExactly(1203.0, 1200.0, 2410.0, 1200.0, 1200.0, 600.5);
    }

    @Test
    void eachInvocationAppliesAnotherMonthOfInterestWithoutAdvancingDays() {
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1000.0, 31);

        calculator.updateBalance(List.of(deposit));
        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(1001.6600000000001);
        assertThat(deposit.getDays()).isEqualTo(31);
    }

    @Test
    void roundsTheInterestRatherThanTheExistingBalance() {
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1000.123, 31);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(1000.123 + 0.83);
    }

    @Test
    void acceptsCustomRulesWithoutChangingTheCalculator() {
        TimeDepositCalculator customCalculator = new TimeDepositCalculator(
            Map.of("loyalty", deposit -> deposit.getBalance() >= 1000 ? 7.125 : 1.0));
        TimeDeposit eligible = new TimeDeposit(1, "loyalty", 1200.0, 31);
        TimeDeposit initialPeriod = new TimeDeposit(2, "loyalty", 1200.0, 30);

        customCalculator.updateBalance(List.of(eligible, initialPeriod));

        assertThat(eligible.getBalance()).isEqualTo(1207.13);
        assertThat(initialPeriod.getBalance()).isEqualTo(1200.0);
    }

    @Test
    void acceptsAnEmptyBatch() {
        calculator.updateBalance(List.of());
    }
}
