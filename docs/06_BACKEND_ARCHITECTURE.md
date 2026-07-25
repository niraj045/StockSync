# 6. Backend Architecture

## 6.1 Package Structure

```text
com.stocksync
├── auth
├── dashboard
├── inventory
├── party
├── site
├── quotation
├── agreement
├── order
├── challan
├── rental
├── hire
├── billing
├── payment
├── gst
├── reporting
├── document
├── audit
├── file
└── common
```

Each module should generally contain:

```text
controller
service
repository
entity
dto
mapper
validation
event
exception
```

## 6.2 Layer Responsibilities

### Controller

- Accept request
- Validate request DTO
- Call application service
- Return response DTO
- Never implement business rules
- Never access repositories directly

### Service

- Implement business use case
- Coordinate modules
- Manage transactions
- Validate business invariants
- Create audit events

### Repository

- Database access only
- No business rules

### Entity

- Persistence model
- Internal to backend
- Never returned directly through API

### DTO

- API request and response contracts
- Separate create, update, list, and detail DTOs

## 6.3 Suggested Dependencies

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-validation
- spring-boot-starter-actuator
- spring-boot-starter-jetty, if Jetty is chosen
- mysql-connector-j
- flyway-mysql
- springdoc-openapi
- mapstruct
- lombok, optional
- apache-poi
- pdfbox
- thymeleaf
- openhtmltopdf
- testcontainers
- junit-jupiter
- mockito

## 6.4 Authentication

Recommended:

- Session-based login
- HTTP-only secure cookies
- SameSite=Lax or Strict where practical
- CSRF protection
- BCrypt or DelegatingPasswordEncoder
- Session timeout
- Account activation status
- Failed-login throttling

## 6.5 Validation

Use:

- Bean Validation for basic input validation
- Service-level validation for business rules
- Database constraints as final protection

Examples:

- Quantity must be greater than zero.
- Receive date cannot be before issue date.
- Site must be active to issue stock.
- Posted challan cannot be edited.
- Payment cannot exceed allowed allocation.

## 6.6 Exception Types

Recommended custom exceptions:

- ResourceNotFoundException
- BusinessRuleException
- InsufficientStockException
- DuplicateNumberException
- InvalidStateTransitionException
- FileValidationException
- UnauthorizedOperationException
- ReportGenerationException

## 6.7 State Transitions

Use explicit methods for transitions.

Example:

```text
Draft Challan -> Posted Challan -> Cancelled Challan
```

Do not allow arbitrary status changes through generic update endpoints.

## 6.8 Auditing

Create audit entries for:

- Login
- Logout
- User creation
- Stock posting
- Stock adjustment
- Challan posting
- Challan cancellation
- Payment creation
- Payment reversal
- Invoice issue
- Agreement generation
- File upload
- Critical configuration changes

## 6.9 Mapping

Use MapStruct or explicit mappers.

Avoid automatic reflection-based mapping for critical financial and stock data.

## 6.10 Configuration Profiles

- `application.yml`
- `application-local.yml`
- `application-test.yml`
- `application-prod.yml`

Production secrets must come from environment variables.

## 6.11 Logging

Use structured logs containing:

- timestamp
- level
- request ID
- user ID
- operation
- entity type
- entity ID
- error code

Never log:

- Passwords
- Session cookies
- Full access tokens
- Sensitive uploaded document contents

## 6.12 Implemented Transaction Boundaries

Category, item, party, vendor, site, and file use cases are exposed through module services and
DTO-based controllers. Inventory is the sole owner of stock writes.

Purchase, scrap, and adjustment posting methods are transactional. They create the source
document and lines, lock affected balance rows pessimistically in deterministic item order, append
immutable ledger entries, update the balance projection, and record the audit action as one use
case. Failed validation or insufficient stock rolls back the entire posting.
