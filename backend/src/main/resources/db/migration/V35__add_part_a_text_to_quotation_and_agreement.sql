-- Add Part A custom text column to Quotation and Agreement structures
ALTER TABLE quotations ADD COLUMN part_a_text TEXT;
ALTER TABLE agreements ADD COLUMN part_a_text TEXT;
