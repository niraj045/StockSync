ALTER TABLE quotations DROP CHECK ck_quotations_rental_type;
ALTER TABLE agreements DROP CHECK ck_agreements_rental_type;

ALTER TABLE quotations
    ADD CONSTRAINT ck_quotations_rental_type CHECK (rental_type IN (
        'PER_PIECE_PER_DAY','PER_PIECE_PER_MONTH','PLATE_AREA_PER_DAY',
        'SCAFFOLD_AREA_PER_DAY','PLOT_AREA_PER_DAY','FIXED_RATE','SLAB_BASED'
    ));

ALTER TABLE agreements
    ADD CONSTRAINT ck_agreements_rental_type CHECK (rental_type IN (
        'PER_PIECE_PER_DAY','PER_PIECE_PER_MONTH','PLATE_AREA_PER_DAY',
        'SCAFFOLD_AREA_PER_DAY','PLOT_AREA_PER_DAY','FIXED_RATE','SLAB_BASED'
    ));

UPDATE quotation_items qi
JOIN quotations q ON q.id = qi.quotation_id
JOIN quotation_templates qt ON qt.id = q.quotation_template_id
SET qi.rental_type = 'PER_PIECE_PER_MONTH'
WHERE qt.template_code = 'STEELFAB_EXACT_HIRE_V1'
  AND qi.rental_type = 'PER_PIECE_PER_DAY';

UPDATE quotations q
JOIN quotation_templates qt ON qt.id = q.quotation_template_id
SET q.rental_type = 'PER_PIECE_PER_MONTH'
WHERE qt.template_code = 'STEELFAB_EXACT_HIRE_V1'
  AND q.rental_type = 'PER_PIECE_PER_DAY';

UPDATE agreement_items ai
JOIN agreements a ON a.id = ai.agreement_id
JOIN quotations q ON q.id = a.quotation_id
JOIN quotation_templates qt ON qt.id = q.quotation_template_id
SET ai.rental_type = 'PER_PIECE_PER_MONTH'
WHERE qt.template_code = 'STEELFAB_EXACT_HIRE_V1'
  AND ai.rental_type = 'PER_PIECE_PER_DAY';

UPDATE agreements a
JOIN quotations q ON q.id = a.quotation_id
JOIN quotation_templates qt ON qt.id = q.quotation_template_id
SET a.rental_type = 'PER_PIECE_PER_MONTH'
WHERE qt.template_code = 'STEELFAB_EXACT_HIRE_V1'
  AND a.rental_type = 'PER_PIECE_PER_DAY';
