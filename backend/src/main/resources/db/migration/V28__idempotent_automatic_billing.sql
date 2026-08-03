ALTER TABLE billing_runs
    ADD COLUMN active_period_key VARCHAR(100)
        GENERATED ALWAYS AS (
            CASE
                WHEN status = 'CANCELLED' THEN NULL
                ELSE CONCAT(agreement_id, ':', period_start, ':', period_end)
            END
        ) STORED,
    ADD UNIQUE KEY uk_billing_runs_active_period (active_period_key);
