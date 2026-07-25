# 13. Testing Strategy

## 13.1 Testing Levels

- Unit tests
- Repository integration tests
- Service integration tests
- API tests
- Frontend component tests
- End-to-end tests
- User acceptance testing

## 13.2 Critical Backend Tests

### Inventory

- Purchase increases available stock.
- Issue decreases available stock.
- Receive increases available stock.
- Loss does not increase available stock.
- Scrap decreases available stock.
- Adjustment requires reason.
- Negative stock is blocked.
- Concurrent issue cannot oversell.

### Challans

- Order can split across multiple challans.
- Total issued does not exceed order.
- Posted challan cannot be edited.
- Cancelled challan reverses stock correctly.
- Partial receiving reduces pending quantity.
- Extra return is recorded separately.
- Size exchange creates correct movements.

### Billing

- Rental day calculation
- Slab transitions
- Partial return billing
- Additional charges
- GST
- TDS
- Outstanding balance

### Payments

- Partial payment
- Multiple allocations
- Reversal
- Over-allocation blocked
- Security deposit separate

## 13.3 Integration Testing

Use Testcontainers with MySQL.

Test:

- Flyway migrations
- JPA mappings
- Transactions
- Locking
- Unique constraints
- Report queries

## 13.4 API Testing

Test:

- Authentication
- Authorization
- Validation
- Error codes
- Pagination
- Filtering
- Posting actions
- File uploads
- PDF download

## 13.5 Frontend Testing

Recommended:

- Vitest
- React Testing Library
- Playwright for end-to-end

Test:

- Dashboard rendering
- Challan form
- Split dispatch
- File upload
- Report download
- Error display
- Session timeout

## 13.6 User Acceptance Scenarios

1. Add item.
2. Purchase stock.
3. Confirm dashboard inward and available totals.
4. Create party and site.
5. Create agreement or order.
6. Issue first partial challan.
7. Issue second partial challan.
8. Receive partial return.
9. Record damage.
10. Generate monthly site report.
11. Create invoice.
12. Record payment.
13. Confirm outstanding balance.
14. Download PDF.
15. Restart containers and confirm data remains.
16. Restore backup in a test environment.
