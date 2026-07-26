ALTER TABLE agreements DROP CHECK ck_agreements_status;
ALTER TABLE agreements DROP CHECK ck_agreements_dates;
ALTER TABLE agreements DROP CHECK ck_agreements_amounts;

ALTER TABLE agreements
    ADD COLUMN agreement_date DATE NULL AFTER site_id,
    ADD COLUMN billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' AFTER rental_type,
    ADD COLUMN custom_billing_cycle_days INT NULL AFTER billing_cycle,
    ADD COLUMN grace_period_days INT NOT NULL DEFAULT 0 AFTER custom_billing_cycle_days,
    ADD COLUMN minimum_billing_days INT NOT NULL DEFAULT 0 AFTER grace_period_days,
    ADD COLUMN party_legal_name_snapshot VARCHAR(150) NULL,
    ADD COLUMN party_trade_name_snapshot VARCHAR(150) NULL,
    ADD COLUMN party_gstin_snapshot VARCHAR(15) NULL,
    ADD COLUMN party_pan_snapshot VARCHAR(10) NULL,
    ADD COLUMN party_address_snapshot VARCHAR(500) NULL,
    ADD COLUMN party_state_snapshot VARCHAR(100) NULL,
    ADD COLUMN party_contact_snapshot VARCHAR(250) NULL,
    ADD COLUMN site_name_snapshot VARCHAR(150) NULL,
    ADD COLUMN site_code_snapshot VARCHAR(50) NULL,
    ADD COLUMN site_address_snapshot VARCHAR(500) NULL,
    ADD COLUMN site_contact_snapshot VARCHAR(100) NULL,
    ADD COLUMN quotation_number_snapshot VARCHAR(50) NULL,
    ADD COLUMN quotation_date_snapshot DATE NULL,
    ADD COLUMN quotation_approved_at_snapshot TIMESTAMP(6) NULL,
    ADD COLUMN subtotal DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN discount_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN taxable_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN cgst_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN sgst_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN igst_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN total_tax DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN other_charge DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN round_off DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN grand_total DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN generated_document_attachment_id BIGINT NULL,
    ADD COLUMN ready_for_review_at TIMESTAMP(6) NULL,
    ADD COLUMN ready_for_review_by VARCHAR(50) NULL,
    ADD COLUMN activated_at TIMESTAMP(6) NULL,
    ADD COLUMN activated_by VARCHAR(50) NULL,
    ADD COLUMN expired_at TIMESTAMP(6) NULL,
    ADD COLUMN expired_by VARCHAR(50) NULL,
    ADD COLUMN termination_reason VARCHAR(1000) NULL,
    ADD COLUMN terminated_at TIMESTAMP(6) NULL,
    ADD COLUMN terminated_by VARCHAR(50) NULL,
    ADD COLUMN closed_at TIMESTAMP(6) NULL,
    ADD COLUMN closed_by VARCHAR(50) NULL,
    ADD COLUMN cancellation_reason VARCHAR(1000) NULL,
    ADD COLUMN cancelled_at TIMESTAMP(6) NULL,
    ADD COLUMN cancelled_by VARCHAR(50) NULL;

UPDATE agreements a
JOIN parties p ON p.id=a.party_id
JOIN sites s ON s.id=a.site_id
LEFT JOIN quotations q ON q.id=a.quotation_id
SET a.agreement_date=COALESCE(a.effective_date, CURRENT_DATE),
    a.party_legal_name_snapshot=p.legal_name, a.party_trade_name_snapshot=p.trade_name,
    a.party_gstin_snapshot=p.gstin, a.party_pan_snapshot=p.pan, a.party_address_snapshot=p.address,
    a.party_state_snapshot=p.state, a.party_contact_snapshot=CONCAT_WS(' / ',p.contact_person,p.phone,p.email),
    a.site_name_snapshot=s.site_name, a.site_code_snapshot=s.site_code,
    a.site_address_snapshot=s.address, a.site_contact_snapshot=s.contact_person,
    a.quotation_number_snapshot=q.quotation_number, a.quotation_date_snapshot=q.quotation_date,
    a.quotation_approved_at_snapshot=q.approved_at,
    a.subtotal=COALESCE(q.subtotal,0), a.discount_amount=COALESCE(q.discount_amount,0),
    a.taxable_amount=COALESCE(q.taxable_amount,0), a.cgst_amount=COALESCE(q.cgst_amount,0),
    a.sgst_amount=COALESCE(q.sgst_amount,0), a.igst_amount=COALESCE(q.igst_amount,0),
    a.total_tax=COALESCE(q.total_tax,0), a.other_charge=COALESCE(q.other_charge,0),
    a.round_off=COALESCE(q.round_off,0), a.grand_total=COALESCE(q.grand_total,0),
    a.status=CASE WHEN a.status='GENERATED' THEN 'READY_FOR_REVIEW' ELSE a.status END;

ALTER TABLE agreements
    MODIFY agreement_date DATE NOT NULL,
    MODIFY party_legal_name_snapshot VARCHAR(150) NOT NULL,
    MODIFY site_name_snapshot VARCHAR(150) NOT NULL,
    MODIFY site_code_snapshot VARCHAR(50) NOT NULL,
    ADD CONSTRAINT fk_agreement_generated_attachment FOREIGN KEY (generated_document_attachment_id) REFERENCES file_attachments(id),
    ADD CONSTRAINT ck_agreements_status CHECK (status IN ('DRAFT','READY_FOR_REVIEW','ACTIVE','EXPIRED','TERMINATED','CLOSED','CANCELLED')),
    ADD CONSTRAINT ck_agreements_dates CHECK (
      (expiry_date IS NULL OR expiry_date >= effective_date)
      AND (expiry_date IS NULL OR agreement_date <= expiry_date)
    ),
    ADD CONSTRAINT ck_agreements_billing CHECK (
      billing_cycle IN ('WEEKLY','MONTHLY','CUSTOM')
      AND (billing_cycle <> 'CUSTOM' OR custom_billing_cycle_days > 0)
      AND grace_period_days >= 0 AND minimum_billing_days >= 0
    ),
    ADD CONSTRAINT ck_agreements_amounts CHECK (
      security_deposit >= 0 AND subtotal >= 0 AND discount_amount >= 0 AND taxable_amount >= 0
      AND cgst_amount >= 0 AND sgst_amount >= 0 AND igst_amount >= 0 AND total_tax >= 0
      AND transport_charge >= 0 AND loading_charge >= 0 AND unloading_charge >= 0
      AND other_charge >= 0 AND grand_total >= 0
    );

ALTER TABLE agreement_items
    ADD COLUMN source_quotation_item_id BIGINT NULL,
    ADD COLUMN item_code_snapshot VARCHAR(50) NULL,
    ADD COLUMN item_name_snapshot VARCHAR(150) NULL,
    ADD COLUMN description_snapshot VARCHAR(500) NULL,
    ADD COLUMN size_snapshot VARCHAR(100) NULL,
    ADD COLUMN unit_snapshot VARCHAR(30) NULL,
    ADD COLUMN weight_snapshot DECIMAL(19,4) NULL,
    ADD COLUMN rental_type VARCHAR(40) NULL,
    ADD COLUMN area_rate DECIMAL(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN weight_rate DECIMAL(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN loss_rate_per_piece DECIMAL(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN loss_rate_per_weight DECIMAL(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN damage_rate DECIMAL(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN sequence_number INT NOT NULL DEFAULT 0,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_agreement_item_source FOREIGN KEY (source_quotation_item_id) REFERENCES quotation_items(id);

UPDATE agreement_items ai
JOIN items i ON i.id=ai.item_id
JOIN agreements a ON a.id=ai.agreement_id
SET ai.item_code_snapshot=i.item_code, ai.item_name_snapshot=i.item_name,
    ai.size_snapshot=i.size, ai.unit_snapshot=i.unit, ai.weight_snapshot=i.weight_per_piece,
    ai.rental_type=a.rental_type, ai.sequence_number=ai.id;

ALTER TABLE agreement_items
    MODIFY item_code_snapshot VARCHAR(50) NOT NULL,
    MODIFY item_name_snapshot VARCHAR(150) NOT NULL,
    MODIFY unit_snapshot VARCHAR(30) NOT NULL,
    MODIFY rental_type VARCHAR(40) NOT NULL,
    ADD CONSTRAINT ck_agreement_item_rates CHECK (
      agreed_quantity > 0 AND unit_rate >= 0 AND rental_rate >= 0
      AND area_rate >= 0 AND weight_rate >= 0 AND loss_rate_per_piece >= 0
      AND loss_rate_per_weight >= 0 AND damage_rate >= 0
    );

CREATE INDEX idx_agreements_quotation ON agreements(quotation_id);
CREATE INDEX idx_agreements_expiry ON agreements(expiry_date);
CREATE INDEX idx_agreements_status_site ON agreements(status,site_id);
CREATE INDEX idx_agreement_items_source ON agreement_items(source_quotation_item_id);
