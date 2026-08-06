-- Migration: Deduplicate Items & Normalize Units to NOS

-- 1. Normalize all units in items table to 'NOS'
UPDATE items SET unit = 'NOS' WHERE UPPER(unit) IN ('PIECE', 'PCS', 'PC', 'NOS', 'NOS.', 'NOS ');

-- 2. Merge Duplicate Item 35 ("8ft & 10ft plate pipe") into Item 33 ("8ft & 10ft plate pipe")
-- Re-map import rows
UPDATE stock_import_rows SET mapped_item_id = 33 WHERE mapped_item_id = 35;

-- Re-map stock transactions
UPDATE stock_transactions SET item_id = 33 WHERE item_id = 35;

-- Re-map site_stock_balances
UPDATE site_stock_balances SET item_id = 33 WHERE item_id = 35;

-- Update stock_balances for Item 33 and remove Item 35 stock balance
UPDATE stock_balances sb33
JOIN (SELECT available_quantity, issued_quantity FROM stock_balances WHERE item_id = 35) sb35
SET sb33.issued_quantity = sb33.issued_quantity + sb35.issued_quantity,
    sb33.available_quantity = sb33.available_quantity + sb35.available_quantity
WHERE sb33.item_id = 33;

DELETE FROM stock_balances WHERE item_id = 35;

-- Delete orphaned Item 35
DELETE FROM items WHERE id = 35;

-- 3. Recalculate and re-sync stock_balances to match actual sum of stock transactions and site_stock_balances
-- Re-sync issued_quantity for all items in stock_balances from site_stock_balances
UPDATE stock_balances sb
LEFT JOIN (
    SELECT item_id, COALESCE(SUM(pending_quantity), 0) as total_issued
    FROM site_stock_balances
    GROUP BY item_id
) ssb ON sb.item_id = ssb.item_id
SET sb.issued_quantity = COALESCE(ssb.total_issued, 0);

