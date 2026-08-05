-- Add default Part A text column to Templates
ALTER TABLE quotation_templates ADD COLUMN default_part_a_text TEXT;
ALTER TABLE agreement_templates ADD COLUMN default_part_a_text TEXT;
