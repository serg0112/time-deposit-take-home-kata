package org.ikigaidigital;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TimeDepositApiIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.9-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired RequestMappingHandlerMapping mappings;
    @Autowired EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void clearDatabase() {
        jdbc.execute("DROP TRIGGER IF EXISTS reject_balance ON time_deposits");
        jdbc.execute("DROP FUNCTION IF EXISTS reject_second_update()");
        jdbc.execute("TRUNCATE withdrawals, time_deposits RESTART IDENTITY");
    }

    @Test
    void emptyDatabaseReturnsAnEmptyArrayAndCanBeUpdated() {
        ResponseEntity<String> response = http.getForEntity("/time-deposits", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
        assertThat(updateBalances().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM time_deposits", Integer.class)).isZero();
    }

    @Test
    void returnsExactSchemaWithOrderedWithdrawalsAndEmptyHistory() throws Exception {
        insertDeposit(2, "premium", "1200.123", 46);
        insertDeposit(1, "basic", "600.00", 31);
        jdbc.update("""
            INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES
                (8, 2, 12.34, DATE '2026-02-01'), (3, 2, 50.25, DATE '2026-01-15')
            """);

        ResponseEntity<String> response = http.getForEntity("/time-deposits", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(response.getBody())).isEqualTo(json.readTree("""
            [
              {"id":1,"planType":"basic","balance":600.00,"days":31,"withdrawals":[]},
              {"id":2,"planType":"premium","balance":1200.123,"days":46,"withdrawals":[
                {"id":3,"timeDepositId":2,"amount":50.25,"date":"2026-01-15"},
                {"id":8,"timeDepositId":2,"amount":12.34,"date":"2026-02-01"}
              ]}
            ]
            """));
        assertBalance(2, "1200.123");
    }

    @Test
    void retrievesWithdrawalHistoriesWithoutPerDepositQueries() throws Exception {
        for (int id = 1; id <= 12; id++) {
            insertDeposit(id, "basic", "1200.00", 31);
            jdbc.update("""
                INSERT INTO withdrawals (time_deposit_id, amount, date) VALUES (?, 10.00, DATE '2026-01-15')
                """, id);
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            ResponseEntity<String> response = http.getForEntity("/time-deposits", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(json.readTree(response.getBody())).hasSize(12);
            // A collection fetch or two bulk queries are both acceptable; a query per deposit is not.
            assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 2L);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @Test
    void updatesEveryBalanceAndPreservesAgePlansAndWithdrawalHistory() {
        insertDeposit(1, "basic", "1200.00", 31);
        insertDeposit(2, "student", "1200.00", 365);
        insertDeposit(3, "premium", "1200.00", 46);
        insertDeposit(4, "student", "1200.00", 366);
        insertDeposit(5, "basic", "1200.00", 30);
        insertDeposit(6, "basic", "18.00", 31);
        jdbc.update("""
            INSERT INTO withdrawals (time_deposit_id, amount, date) VALUES (1, 50.25, DATE '2026-01-15')
            """);
        List<Map<String, Object>> historyBefore = jdbc.queryForList("SELECT * FROM withdrawals");
        List<Map<String, Object>> metadataBefore = jdbc.queryForList(
            "SELECT id, plan_type, days FROM time_deposits ORDER BY id");

        ResponseEntity<String> response = updateBalances();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        assertBalance(1, "1201.00");
        assertBalance(2, "1203.00");
        assertBalance(3, "1205.00");
        assertBalance(4, "1200.00");
        assertBalance(5, "1200.00");
        assertBalance(6, "18.01");
        assertThat(jdbc.queryForList("SELECT * FROM withdrawals")).isEqualTo(historyBefore);
        assertThat(jdbc.queryForList("SELECT id, plan_type, days FROM time_deposits ORDER BY id"))
            .isEqualTo(metadataBefore);
    }

    @Test
    void preservesExistingFractionalCentsRatherThanRoundingTheBalance() {
        insertDeposit(1, "basic", "1000.123", 31);

        assertThat(updateBalances().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertBalance(1, "1000.9530000000001");
    }

    @Test
    void rollsBackTheWholeBatchWhenOneDatabaseUpdateFails() {
        insertDeposit(1, "basic", "1200.00", 31);
        insertDeposit(2, "premium", "1200.00", 46);
        // Fail the second row inside PostgreSQL, after the first row's UPDATE has been attempted.
        jdbc.execute("""
            CREATE FUNCTION reject_second_update() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN
                IF NEW.id = 2 THEN RAISE EXCEPTION 'deliberate test failure'; END IF;
                RETURN NEW;
            END $$
            """);
        jdbc.execute("""
            CREATE TRIGGER reject_balance BEFORE UPDATE ON time_deposits
            FOR EACH ROW EXECUTE FUNCTION reject_second_update()
            """);

        assertThat(updateBalances().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertBalance(1, "1200.00");
        assertBalance(2, "1200.00");
    }

    @Test
    void concurrentRequestsBothAccrueInterestWithoutLostUpdates() throws Exception {
        insertDeposit(1, "basic", "1000.00", 31);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            CompletableFuture<ResponseEntity<String>> first = CompletableFuture.supplyAsync(
                () -> updateAfter(start), executor);
            CompletableFuture<ResponseEntity<String>> second = CompletableFuture.supplyAsync(
                () -> updateAfter(start), executor);
            start.countDown();

            assertThat(first.get(30, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(second.get(30, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertBalance(1, "1001.6600000000001");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void exposesExactlyTheTwoOperationsDeclaredInTheOpenApiContract() throws Exception {
        Map<String, Object> contract;
        try (InputStream source = Files.newInputStream(Path.of("openapi.yaml"))) {
            contract = new Yaml().load(source);
        }
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) contract.get("paths");
        Set<String> declared = paths.entrySet().stream()
            .flatMap(path -> path.getValue().keySet().stream()
                .map(method -> method.toUpperCase() + " " + path.getKey()))
            .collect(Collectors.toSet());
        Set<String> actual = mappings.getHandlerMethods().keySet().stream()
            .flatMap(mapping -> mapping.getMethodsCondition().getMethods().stream()
                .flatMap(method -> mapping.getPatternValues().stream().map(path -> method.name() + " " + path)))
            .collect(Collectors.toSet());

        assertThat(declared).containsExactlyInAnyOrder("GET /time-deposits", "POST /time-deposits/update-balances");
        assertThat(actual).isEqualTo(declared);
    }

    private ResponseEntity<String> updateAfter(CountDownLatch start) {
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Concurrent updates did not start in time");
            }
            return updateBalances();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private ResponseEntity<String> updateBalances() {
        return http.postForEntity("/time-deposits/update-balances", null, String.class);
    }

    private void insertDeposit(int id, String plan, String balance, int days) {
        jdbc.update("INSERT INTO time_deposits (id, plan_type, balance, days) VALUES (?, ?, ?, ?)",
            id, plan, new BigDecimal(balance), days);
    }

    private void assertBalance(int id, String expected) {
        BigDecimal actual = jdbc.queryForObject("SELECT balance FROM time_deposits WHERE id = ?",
            BigDecimal.class, id);
        assertThat(actual).isEqualByComparingTo(expected);
    }
}
