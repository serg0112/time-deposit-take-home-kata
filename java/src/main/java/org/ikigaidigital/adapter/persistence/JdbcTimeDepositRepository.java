package org.ikigaidigital.adapter.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.DepositDetails;
import org.ikigaidigital.application.DepositDetails.Withdrawal;
import org.ikigaidigital.application.TimeDepositRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Repository
public class JdbcTimeDepositRepository implements TimeDepositRepository {
    private final JdbcTemplate jdbc;

    public JdbcTimeDepositRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositDetails> findAll() {
        // Two queries for the whole collection, including deposits without withdrawals; no N+1 reads.
        Map<Integer, List<Withdrawal>> withdrawals = jdbc.query("""
            SELECT id, "timeDepositId", amount, date FROM withdrawals ORDER BY id
            """, (row, index) -> new Withdrawal(row.getInt("id"), row.getInt("timeDepositId"),
                row.getBigDecimal("amount"), row.getObject("date", LocalDate.class)))
            .stream().collect(Collectors.groupingBy(Withdrawal::timeDepositId));

        return jdbc.query("""
            SELECT id, "planType", balance, days FROM "timeDeposits" ORDER BY id
            """, (row, index) -> new DepositDetails(row.getInt("id"), row.getString("planType"),
                row.getBigDecimal("balance"), row.getInt("days"),
                withdrawals.getOrDefault(row.getInt("id"), List.of())));
    }

    @Override
    @Transactional
    public void updateBalances(Consumer<List<TimeDeposit>> update) {
        // Hold row locks until the entire batch commits, so concurrent requests cannot lose interest.
        List<TimeDeposit> deposits = jdbc.query("""
            SELECT id, "planType", balance, days FROM "timeDeposits" ORDER BY id FOR UPDATE
            """, (row, index) -> new TimeDeposit(row.getInt("id"), row.getString("planType"),
                row.getDouble("balance"), row.getInt("days")));

        update.accept(deposits);

        jdbc.batchUpdate("UPDATE \"timeDeposits\" SET balance = ? WHERE id = ?", deposits, 100,
            (statement, deposit) -> {
                // Preserve the shared calculator's result; do not round the balance again in storage.
                statement.setBigDecimal(1, BigDecimal.valueOf(deposit.getBalance()));
                statement.setInt(2, deposit.getId());
            });
    }
}
