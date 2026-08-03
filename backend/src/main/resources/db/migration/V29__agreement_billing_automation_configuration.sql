ALTER TABLE agreements
    ADD COLUMN measurement_basis VARCHAR(20) NOT NULL DEFAULT 'ITEM_QUANTITY',
    ADD COLUMN billing_commencement_rule VARCHAR(30) NOT NULL DEFAULT 'FIRST_DISPATCH',
    ADD COLUMN fixed_billing_start_date DATE NULL,
    ADD COLUMN next_billing_date DATE NULL,
    ADD COLUMN last_auto_period_end DATE NULL;

ALTER TABLE agreements
    ADD CONSTRAINT chk_agreement_measurement_basis
        CHECK (measurement_basis IN ('ITEM_QUANTITY', 'SQUARE_FEET')),
    ADD CONSTRAINT chk_agreement_billing_commencement
        CHECK (billing_commencement_rule IN ('FIRST_DISPATCH', 'AGREEMENT_EFFECTIVE_DATE', 'FIXED_DATE'));
