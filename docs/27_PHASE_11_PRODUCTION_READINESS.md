# Phase 11 — Production Readiness and Performance

## Delivered architecture

StockSync remains a single-node modular monolith:

```text
HTTPS Nginx -> Spring Boot -> MySQL
                       \-> uploaded-files volume
Daily backup service -> database dump + uploaded files -> local and off-server paths
```

Redis, load balancing, shared sessions, and a second backend are intentionally
excluded for the current 3–8 users.

## Production deployment

1. Copy `.env.production.example` to `.env.production`.
2. Replace both database passwords with independent random values of at least
   20 characters.
3. Set `SERVER_NAME`, and place the TLS certificate and key at the configured
   paths.
4. Validate and start:

```bash
./deploy-production.sh validate
./deploy-production.sh start
./deploy-production.sh status
```

The production Compose file does not publish MySQL or Spring Boot. Only Nginx
publishes ports 80 and 443. It applies CPU/RAM limits, bounded container logs,
health checks, restart policies, graceful backend shutdown, and
`no-new-privileges`.

## Security and Nginx

- HTTP redirects to HTTPS.
- TLS 1.2/1.3, HSTS, CSP, frame, MIME, referrer, and permissions headers are set.
- Session cookies are Secure, HttpOnly, SameSite=Lax, and expire after the
  configured inactivity timeout.
- CSRF remains enabled in Spring Security.
- Nginx limits login/password and expensive import/export routes in dry-run
  mode. Review Nginx logs before changing `limit_req_dry_run on` to `off`.
- Actuator health, metrics, cache metrics, JVM metrics, Hikari metrics, and HTTP
  latency histograms are available only inside the private Docker network.

## Database performance

The existing API list endpoints use Spring Data pagination for growing business
tables. Stable, deliberately small template dropdowns remain bounded lists.
Entities use lazy relationships by default, while aggregate detail reads use
targeted entity graphs/fetch joins.

MySQL records statements slower than 500 ms into `mysql.slow_log`. Generate a
report and investigate high `rows_examined` statements:

```bash
./deployment/mysql/slow-query-report.sh
```

Run `EXPLAIN ANALYZE` against the exact statement and real filter values before
adding an index. Migrations V17 and V18 contain the current report/dashboard
indexes; applied migrations must never be edited.

HikariCP is bounded to 10 connections by default, with two idle connections and
explicit acquisition, validation, idle, and lifetime timeouts. Enable leak
detection temporarily by setting `DB_LEAK_DETECTION_MS=60000`.

## Caching

Caffeine caches stable category and agreement/quotation-template reads for ten
minutes with bounded size and statistics. Category/template mutations evict the
related cache. Stock, balances, invoices, payments, permissions, and posting
results are never cached. Nginx gives one-year immutable browser caching only
to versioned static assets.

## Backup and restore

The backup service immediately creates a consistent MySQL dump and uploaded-file
archive, then repeats daily. Every set includes SHA-256 checksums, is retained
locally for seven days, and is copied to `OFFSITE_BACKUP_PATH`.

Create an on-demand backup:

```bash
./deploy-production.sh backup
```

Restore requires an explicit confirmation and replaces the target database and
uploaded files:

```bash
ENV_FILE=.env.production \
  ./deployment/backup/restore.sh /path/to/backup/20260729T120000Z --confirm
```

Perform a restore drill into an isolated non-production Compose project before
launch and monthly thereafter. Record the backup identifier, checksum result,
restore duration, row-count checks, document-download check, operator, and date.

## Capacity testing

The lightweight repeatable smoke load runs eight concurrent users by default:

```bash
VUS=8 DURATION=2m ./deployment/loadtest/run-capacity-test.sh
```

Evidence is written under `e2e-data/capacity/`. Run the existing browser
client-story and daily-operation tests at the same time for real authenticated
workflows, plus one manual import, PDF, report, billing, and backup operation.

Run twice:

- 2 vCPU / 2 GB: use the default limits in `.env.production.example`.
- 2 vCPU / 4 GB: raise MySQL and backend to 1536 MB each.

Record idle/peak RAM, peak CPU, p95/p99 latency, slow queries, restarts, OOM
events, and host swap. The VPS decision is evidence-based; it cannot be made
from source inspection alone.

## Final go-live gates

- Real domain and trusted TLS certificate installed.
- Production secrets generated and stored outside Git.
- Admin uses a unique password; the local `Password123` account is forbidden.
- Rate-limit dry-run logs reviewed before enforcement.
- On-demand backup verified and off-server copy confirmed.
- Full isolated restore drill passed.
- 2 GB and 4 GB capacity evidence reviewed.
- Firewall exposes only SSH, HTTP, and HTTPS.
- Monitoring and disk-space alerts configured on the host.
