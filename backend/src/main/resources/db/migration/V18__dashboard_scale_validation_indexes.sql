CREATE INDEX idx_dashboard_site_orders_status_date ON site_orders(status, order_date);
CREATE INDEX idx_dashboard_issued_order_date ON issued_challans(site_order_id, dispatch_date);
CREATE INDEX idx_dashboard_receiving_site_status_date ON receiving_challans(site_id, status, receive_date);
CREATE INDEX idx_dashboard_invoices_due_status ON invoices(due_date, status);
CREATE INDEX idx_dashboard_payment_status_date ON payment_receipts(status, payment_date);
