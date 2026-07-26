# Shuttering Inventory Management System

## Phase 5A quotation verification

With MySQL and the backend running, open `http://localhost:5173/quotation-templates` as ADMIN, create an active template, then open `http://localhost:5173/quotations`. Draft numbers are assigned on creation as `QT/2026-27/0001`. Quotations never reserve or change stock. See `docs/16_PHASE_5A_QUOTATIONS.md`.

Phase 5A hardening persists party, site, and item display snapshots so finalized PDFs are not changed by later master-data edits. The canonical read-only finance authority is `ROLE_ACCOUNTS`.

This repository documentation defines the product, requirements, architecture, development standards, deployment model, and implementation roadmap for a shuttering inventory management web application.

The system is intended for a shuttering-material business that needs to manage inventory, sites, parties, agreements, issued and received challans, rental billing, payments, GST reports, and operational reports.

## Confirmed Technology Stack

- Backend: Java 21, Spring Boot, Spring Web MVC, Spring Data JPA, Hibernate, Spring Security, Bean Validation, Flyway, Maven
- Frontend: React, TypeScript, Vite, Ant Design, React Router, TanStack Query, Axios, Day.js, Recharts
- Database: MySQL 8.4 LTS
- Documents: Apache POI, Thymeleaf, OpenHTMLtoPDF, Apache PDFBox
- Deployment: Docker Compose on a Linux VPS or dedicated server
- Reverse proxy: Nginx
- Architecture style: Modular monolith
- File storage: Persistent server filesystem mounted into the backend container
- Authentication: Session-based authentication with secure cookies
- Reporting: PDF and Excel exports

## Documentation Index

1. [Project Overview](docs/01_PROJECT_OVERVIEW.md)
2. [Product Requirements](docs/02_PRODUCT_REQUIREMENTS.md)
3. [Functional Scope](docs/03_FUNCTIONAL_SCOPE.md)
4. [System Architecture](docs/04_SYSTEM_ARCHITECTURE.md)
5. [Domain and Database Design](docs/05_DOMAIN_AND_DATABASE.md)
6. [Backend Architecture](docs/06_BACKEND_ARCHITECTURE.md)
7. [Frontend Architecture](docs/07_FRONTEND_ARCHITECTURE.md)
8. [API Guidelines](docs/08_API_GUIDELINES.md)
9. [Reporting and Document Processing](docs/09_REPORTING_AND_DOCUMENTS.md)
10. [Security, Audit, and Backup](docs/10_SECURITY_AUDIT_BACKUP.md)
11. [Deployment and DevOps](docs/11_DEPLOYMENT_AND_DEVOPS.md)
12. [Implementation Roadmap](docs/12_IMPLEMENTATION_ROADMAP.md)
13. [Testing Strategy](docs/13_TESTING_STRATEGY.md)
14. [Phase 4 Manual Testing](docs/14_PHASE_4_MANUAL_TESTING.md)
15. [Phase 4.1 Opening Stock Import](docs/15_PHASE_4_1_OPENING_STOCK_IMPORT.md)
16. [Codex Project Instructions](CODEX_PROJECT_INSTRUCTIONS.md)

## Important Product Decision

The first version must be built as a modular monolith.

The business domain is large, but the current operational scale is small:

- About three active users
- One client business
- One deployment server
- Highly connected inventory, challan, site, billing, and payment transactions
- No independent scaling requirement
- No separate development teams

The system should therefore be structured as independent internal modules but deployed as one Spring Boot application.

> Build it like modules. Deploy it like one application.

## Current Implementation

Phases 1 through 4.1 are implemented:

- Foundation, deployment, session authentication, user administration, and immutable audit logging
- Master data for categories, items, parties, sites, vendors, and entity-linked documents
- Inventory ledger, balance projection, purchases, scrap, adjustments, history, and dashboard totals
- Transactional stock posting with mandatory idempotency keys, row locking, and negative-stock protection
- Quotation drafting and approval, agreement conversion and DOCX generation, and site orders with agreement quantity controls
- Controlled client opening-stock import with Apache POI parsing, explicit item and party/site mapping,
  dry-run reconciliation, immutable ledger posting, audit reporting, and safe reversal

Challans, rental billing, payments, GST, and reporting remain future phases.

## Phase 5B Agreements

Agreements are available at `http://localhost:5173/agreements`. Approve a quotation, then use **Convert quotation**. Agreement operations never reserve or modify stock. ADMIN performs activation and final lifecycle actions; OPERATIONS prepares drafts and documents; ROLE_ACCOUNTS and VIEWER are read-only.

## Local Startup with Docker Compose

Prerequisites:

- Docker Engine with Docker Compose v2
- Ports configured through `APP_PORT` must be available

Start the full application:

```bash
cp .env.example .env
```

Replace both example database passwords in `.env`, then run:

```bash
docker compose up --build -d
docker compose ps
```

Open `http://localhost:8088` when using the example port. The backend health endpoints are:

- Through Nginx: `http://localhost:8088/api/v1/health`
- Inside the backend container: `http://localhost:8080/actuator/health`

View logs or stop the stack:

```bash
docker compose logs -f backend
docker compose down
```

`docker compose down` preserves the named MySQL and uploaded-file volumes. Add `--volumes` only when intentionally deleting local persisted data.

## Local Development without Containers

For a one-command Windows setup, open Git Bash in the repository root and run:

```bash
./start.sh
```

The launcher initializes a project-local MySQL 8.4 database when needed, starts the backend on
port `8081`, starts Vite on port `5173`, applies Flyway migrations, and creates a local ADMIN
account only when it does not already exist. It prints the application, Workbench, and login
credentials after all health checks pass.

```bash
./start.sh --status
./start.sh --stop
```

Its local data and logs are in ignored `.mysql-stocksync-dev-data/` and `.stocksync-runtime/`
directories. Override the development defaults with `STOCKSYNC_DB_PASSWORD`,
`STOCKSYNC_MYSQL_ROOT_PASSWORD`, `STOCKSYNC_ADMIN_PASSWORD`, and a matching BCrypt
`STOCKSYNC_ADMIN_PASSWORD_HASH` before the first start.

A separate, non-destructive Selenium client-story test is available under `e2e/`.
See `e2e/README.md` for visible and headless browser commands.

Backend prerequisites are Java 21 and MySQL 8.4. Maven does not need to be installed globally because the repository includes the Maven Wrapper. Create the database and credentials matching `backend/src/main/resources/application-local.yml`, or override them with `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD`.

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

On Windows Command Prompt or PowerShell, use:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

For the frontend, use Node.js 22 and npm:

```bash
cd frontend
npm install
npm run dev
```

The local profile serves the backend at `http://localhost:8081`. Vite serves the frontend at
`http://localhost:5173` and proxies `/api` to the backend at `http://localhost:8081`.

After signing in, master data is available from the sidebar and inventory is available at
`http://localhost:5173/inventory`. Administrators can maintain master data. Administrators and
operations users can post inventory movements; viewers have read-only access.

Phase 4 is available at:

- `http://localhost:5173/quotations`
- `http://localhost:5173/agreements`
- `http://localhost:5173/orders`

Phase 4.1 is available at:

- `http://localhost:5173/opening-stock-imports`

The original and prepared client workbooks are retained under `client-data/original` and
`client-data/prepared`. Upload either controlled workbook from **Inventory → Opening Stock
Import**. The prepared workbook preserves the original `Sheet1` and adds mapping/read-me sheets.
Only `Sheet1` columns B, C:R, and U are imported; source totals T and V are deliberately ignored.
Administrators and operations users may upload, map, and validate. Only administrators may post
or reverse opening balances.

Administrators and operations users can create and transition these records. Only administrators
can upload agreement templates or terminate an active agreement. Agreement templates accept DOCX
or PDF files up to 10 MB. PDF templates are retained as references while StockSync generates the
operational agreement as DOCX.

To use a different local backend port, override the development proxy target:

```powershell
$env:VITE_API_PROXY_TARGET = 'http://localhost:8081'
npm run dev
```

## Build and Configuration Validation

```bash
cd backend
./mvnw clean package

cd ../frontend
npm ci
npm run build

cd ..
docker compose config
```
