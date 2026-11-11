-- Optional local demo data. Run only after the application has applied its Flyway migration.
-- Existing rows are left untouched, so rerunning this file does not reset accrued interest.
INSERT INTO time_deposits (id, plan_type, balance, days) VALUES
    (1, 'basic', 1200.00, 31),
    (2, 'student', 1200.00, 365),
    (3, 'premium', 1200.00, 46),
    (4, 'student', 1200.00, 366),
    (5, 'basic', 1200.00, 30)
ON CONFLICT (id) DO NOTHING;

-- Assumption: balances already reflect past withdrawals; this history is returned, not deducted again.
INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES
    (1, 1, 50.25, DATE '2026-01-15')
ON CONFLICT (id) DO NOTHING;
