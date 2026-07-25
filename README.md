# Shuttering Inventory Management System

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
14. [Codex Project Instructions](CODEX_PROJECT_INSTRUCTIONS.md)

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

Phases 1 through 4 are implemented:

- Foundation, deployment, session authentication, user administration, and immutable audit logging
- Master data for categories, items, parties, sites, vendors, and entity-linked documents
- Inventory ledger, balance projection, purchases, scrap, adjustments, history, and dashboard totals
- Transactional stock posting with mandatory idempotency keys, row locking, and negative-stock protection
- Quotation drafting and approval, agreement conversion and DOCX generation, and site orders with agreement quantity controls

Challans, rental billing, payments, GST, and reporting remain future phases.

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

Vite serves the frontend at `http://localhost:5173` and proxies `/api` to the backend at `http://localhost:8080`.

After signing in, master data is available from the sidebar and inventory is available at
`http://localhost:5173/inventory`. Administrators can maintain master data. Administrators and
operations users can post inventory movements; viewers have read-only access.

Phase 4 is available at:

- `http://localhost:5173/quotations`
- `http://localhost:5173/agreements`
- `http://localhost:5173/orders`

Administrators and operations users can create and transition these records. Only administrators
can upload agreement templates or terminate an active agreement. Agreement templates accept DOCX
or PDF files up to 10 MB. PDF templates are retained as references while StockSync generates the
operational agreement as DOCX.

If port 8080 is occupied and the backend is started on another port, override the development proxy target:

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
