-- A legacy opening-stock workbook does not supply a reorder threshold.
-- Normal item creation still requires this value at the API boundary, while
-- confirmed import-created items preserve the unknown value as NULL.
ALTER TABLE items
    MODIFY COLUMN minimum_stock DECIMAL(19,4) NULL;
