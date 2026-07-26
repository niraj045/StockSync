# 13. Testing Strategy

## Phase 5A

Unit coverage includes financial-year boundaries and calculation rules. MySQL 8.4 integration validation covers V8, concurrency, permissions, transitions, optimistic locking, snapshots, PDF and proof that stock remains unchanged. Frontend validation covers role actions, dependent sites, line validation, clone/download and unsaved changes.

The hardening suite renders and parses the real PDF for canonical roles, verifies safe headers and immutable item/party/site output, checks in-memory cleanup behavior, and compares stock state around quotation transitions. React Testing Library covers protected routes, role actions, dependent sites, dynamic lines, validations, confirmations, API field mapping, conflicts, downloads, and unsaved-change warnings.

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

## 13.7 Automated Coverage Implemented Through Phase 3

The backend integration suite covers authentication and user-management invariants, master-data
validation and authorization, file metadata, inventory idempotency, purchase/scrap/adjustment
projection updates, transactional rollback on insufficient stock, role restrictions, and two
concurrent outbound requests attempting to oversell one balance.

The inventory concurrency test runs both requests simultaneously against MySQL and verifies that
one succeeds, the other returns `INSUFFICIENT_STOCK`, and the final projected balance remains
correct. The frontend currently includes authentication-context tests and a TypeScript production
build; broader component and browser coverage remains part of production readiness.

## 13.8 Phase 4.1 Import Coverage

The parser suite runs against the retained client XLSX fixture and proves:

- 42 source items, 16 locations, 120 site cells, 32 godown cells, and 152 balance rows;
- blank/zero cells are skipped and negative quantities are rejected;
- T/V are ignored and P:R contributes the omitted 2,418;
- corrected totals are 20,637 party/site, 25,382 godown, and 46,019 combined;
- duplicate names and mixed dates are detected.

MySQL integration coverage applies Flyway V1–V7 and verifies alias/exact/manual item mapping,
whole-column location mapping, unresolved mapping blocks, role boundaries, checksum confirmation,
atomic posting, rollback on a late-row failure, no operational-document creation, audit actions,
sequential idempotency, compensating reversal, repeated-reversal rejection, and later-movement
reversal protection.
