ALTER TABLE issued_challans ADD COLUMN ref_no VARCHAR(50);
ALTER TABLE issued_challans ADD COLUMN driver_phone VARCHAR(50);

ALTER TABLE receiving_challans ADD COLUMN ref_no VARCHAR(50);

ALTER TABLE issued_challan_items ADD COLUMN notes VARCHAR(255);
