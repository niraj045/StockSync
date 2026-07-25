# Codex Project Instructions

Use this file as the primary context when asking Codex to implement or modify the project.

## Project

Build a shuttering inventory management web application.

The application is used by approximately three business users to manage:

- Inventory
- Parties
- Sites
- Quotations
- Agreements
- Orders
- Issued challans
- Received challans
- Split dispatches
- Rental calculations
- Invoices
- Payments
- GST
- Reports
- Documents
- Audit logs

## Required Stack

### Backend

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Hibernate
- Spring Security
- Bean Validation
- Flyway
- Maven
- MySQL 8.4
- Jetty may be used as the embedded server
- MapStruct preferred
- JUnit 5
- Testcontainers

### Frontend

- React
- TypeScript
- Vite
- Ant Design
- React Router
- TanStack Query
- Axios
- Day.js
- Recharts

### Deployment

- Docker Compose
- Nginx
- Spring Boot backend
- MySQL
- Persistent file storage
- HTTPS
- Automated backups

## Architecture Rules

1. Use a modular monolith.
2. Do not introduce microservices.
3. Do not introduce Kafka, Redis, Kubernetes, Elasticsearch, or WebSockets.
4. Organize code by business module.
5. Controllers must not call repositories directly.
6. Entities must not be returned by APIs.
7. Use request and response DTOs.
8. Use service interfaces between modules.
9. Only the inventory module may update stock balances.
10. All stock-changing operations must be transactional.
11. Posted stock and financial records must be immutable.
12. Corrections must use reversal or adjustment records.
13. Use optimistic or pessimistic locking to prevent overselling.
14. Never use floating-point types for money.
15. Use `BigDecimal` for money, quantity, weight, rate, and tax.
16. Use Flyway for all schema changes.
17. Never edit an already-applied migration.
18. Use session-based authentication with secure cookies.
19. Protect state-changing operations with CSRF.
20. Store uploaded files outside the public web root.
21. Store file metadata in MySQL.
22. Do not expose MySQL publicly.
23. Add audit logging for important actions.
24. Keep production secrets in environment variables.
25. Provide tests for critical business rules.

## Core Business Rules

### Stock

- Stock is maintained through an immutable transaction ledger.
- Stock balance is updated for performance.
- Every stock change must reference its source entity.
- Negative available stock is blocked.
- Posted transactions are not deleted.

### Split Challans

- One order may be fulfilled by multiple issued challans.
- Remaining quantity equals ordered quantity minus posted issued quantity.
- A challan cannot exceed remaining quantity without explicit authorized override.

### Receiving

- Partial returns are supported.
- Extra quantities are recorded separately.
- Damage and loss are recorded separately.
- Size exchanges must create explicit item movements.
- Pending site quantity updates automatically.

### Billing

- Support multiple rental calculation models.
- Support rate slabs.
- Support transport, loading, unloading, labour, loss, damage, and custom charges.
- Security deposit remains separate from normal payments.
- Payments may be partial.
- TDS is supported.
- Outstanding balance is calculated automatically.

## API Rules

- Base path: `/api/v1`
- Use explicit action endpoints for posting, issuing, cancelling, reversing, and generating.
- Use standard error responses.
- Use pagination for list endpoints.
- Use OpenAPI documentation.
- Use idempotency protection for critical posting endpoints where practical.

## Coding Style

- Prefer clear code over clever code.
- Keep methods small.
- Use meaningful domain names.
- Avoid generic `Utils` classes.
- Avoid premature abstractions.
- Avoid excessive inheritance.
- Prefer composition.
- Use records for immutable DTOs where appropriate.
- Add comments only where business logic is not obvious.
- Add tests before refactoring critical calculations.

## Before Implementing Any Feature

1. Read the relevant documentation file.
2. Identify the owning module.
3. Identify affected entities and tables.
4. Identify transaction boundaries.
5. Identify validation rules.
6. Identify audit requirements.
7. Identify API contract.
8. Identify required tests.
9. Avoid changing unrelated modules.
10. Update documentation when behaviour changes.

## Definition of Done

A feature is complete when:

- Business rules are implemented.
- Validation is implemented.
- Authorization is implemented.
- Audit logging is implemented where needed.
- Database migration exists.
- API is documented.
- Backend tests pass.
- Frontend handles loading, empty, success, and error states.
- Docker deployment still works.
- No secrets are committed.
