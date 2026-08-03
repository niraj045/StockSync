ALTER TABLE quotations DROP CHECK ck_quotations_rental_type;
ALTER TABLE agreements DROP CHECK ck_agreements_rental_type;

ALTER TABLE quotations
    ADD CONSTRAINT ck_quotations_rental_type CHECK (rental_type IN (
        'PER_PIECE_PER_DAY','PER_PIECE_PER_WEEK','PER_PIECE_PER_MONTH',
        'PLATE_AREA_PER_DAY','SCAFFOLD_AREA_PER_DAY','PLOT_AREA_PER_DAY',
        'FIXED_RATE','SLAB_BASED'
    ));

ALTER TABLE agreements
    ADD CONSTRAINT ck_agreements_rental_type CHECK (rental_type IN (
        'PER_PIECE_PER_DAY','PER_PIECE_PER_WEEK','PER_PIECE_PER_MONTH',
        'PLATE_AREA_PER_DAY','SCAFFOLD_AREA_PER_DAY','PLOT_AREA_PER_DAY',
        'FIXED_RATE','SLAB_BASED'
    ));
