-- Add Part A and Part B titles to Quotation and Agreement structures

ALTER TABLE quotation_templates
ADD COLUMN default_part_a_title VARCHAR(255),
ADD COLUMN default_part_b_title VARCHAR(255);

ALTER TABLE quotations
ADD COLUMN part_a_title VARCHAR(255),
ADD COLUMN part_b_title VARCHAR(255);

ALTER TABLE agreement_templates
ADD COLUMN default_part_a_title VARCHAR(255),
ADD COLUMN default_part_b_title VARCHAR(255);

ALTER TABLE agreements
ADD COLUMN part_a_title VARCHAR(255),
ADD COLUMN part_b_title VARCHAR(255);
