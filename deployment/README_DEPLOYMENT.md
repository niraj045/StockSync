# StockSync BigRock Deployment

This package deploys StockSync independently at `/opt/stocksync`. It does not connect to or modify the existing `caretakers.ind.in` cPanel website or either cPanel database. The production application is served at `https://stocksync.caretakers.ind.in`.

## Architecture

- `frontend`: Nginx publishes ports `80` and `443`, redirects HTTP to HTTPS, and serves React plus `/api` proxying.
- `backend`: Spring Boot listens only inside Docker on `8081`.
- `mysql`: MySQL 8.4 owns database `shuttering_inventory`; `127.0.0.1:3307` is available only through an encrypted SSH tunnel for Workbench.
- `stocksync_private`: one internal Docker bridge network shared by all three services.
- `mysql_data`: named Docker volume for database files.
- `deployment/storage`: bind-mounted as `/data` for uploads, generated PDFs, agreements, invoices, reports, and exports.

The backend has one established `FILE_STORAGE_PATH`. Production therefore mounts the entire persistent storage tree at `/data`; the `uploads`, `documents`, and `reports` directories remain available beneath that same root without changing application storage behavior.

## 1. Validate Locally

Start Docker Desktop, then run the existing application tests and builds from PowerShell:

```powershell
cd C:\stockSync\backend
.\mvnw.cmd test
.\mvnw.cmd package

cd C:\stockSync\frontend
npm test
npm run build
```

No Selenium suite is required.

## 2. Create Local Environment

Copy the placeholder file. The real `.env` is ignored by Git and is never packaged automatically.

```powershell
cd C:\stockSync
Copy-Item deployment\.env.example deployment\.env
openssl rand -base64 36
openssl rand -base64 36
notepad deployment\.env
```

Use the two generated values for `MYSQL_PASSWORD` and `MYSQL_ROOT_PASSWORD`. They must be different and at least 20 characters. Production uses:

```dotenv
APP_ORIGIN=https://stocksync.caretakers.ind.in
SESSION_COOKIE_SECURE=true
```

## 3. Start and Verify Locally

```powershell
cd C:\stockSync
docker compose --project-directory deployment --env-file deployment\.env -f deployment\docker-compose.prod.yml config
docker compose --project-directory deployment --env-file deployment\.env -f deployment\docker-compose.prod.yml build
docker compose --project-directory deployment --env-file deployment\.env -f deployment\docker-compose.prod.yml up -d
docker compose --project-directory deployment --env-file deployment\.env -f deployment\docker-compose.prod.yml ps
docker stats --no-stream
docker compose --project-directory deployment --env-file deployment\.env -f deployment\docker-compose.prod.yml logs --tail=100
```

Verify:

```powershell
Invoke-WebRequest http://127.0.0.1/ -UseBasicParsing
Invoke-RestMethod http://127.0.0.1/api/v1/health
Invoke-WebRequest http://127.0.0.1/api/v1/auth/csrf -SessionVariable csrfSession -UseBasicParsing
```

Open `http://127.0.0.1/login`, sign in, refresh a nested React route, and generate/download one PDF. Flyway must report successful migrations through the latest version. Neither `3306` nor `8081` is published by Compose.

## 4. Stop Local Stack

Stop without `-v`; this preserves the local MySQL volume and storage files.

```powershell
docker compose --project-directory deployment --env-file deployment\.env -f deployment\docker-compose.prod.yml down
```

## 5. Upload to BigRock

The uploader includes only `backend`, `frontend`, and `deployment`; it excludes `.git`, dependencies, build output, tests, storage contents, and `deployment/.env`.

```powershell
cd C:\stockSync
.\deployment\scripts\upload-from-windows.ps1
```

Equivalent direct invocation:

```powershell
powershell -ExecutionPolicy Bypass -File .\deployment\scripts\upload-from-windows.ps1 -Server 66.116.253.40 -User stocksync
```

## 6. Connect and Prepare the VPS

The upload script prints the timestamped archive name. Connect and use that exact name:

```bash
ssh stocksync@66.116.253.40
cd /opt/stocksync
tar -xzf stocksync-deployment-YYYYMMDD-HHMMSS.tar.gz
chmod +x deployment/scripts/*.sh backend/mvnw
sudo ./deployment/scripts/prepare-server.sh
```

`prepare-server.sh` only validates Docker/Compose, RAM, and disk, then creates StockSync directories and ownership. It does not install or remove server software.

## 7. Create Server Environment

```bash
cd /opt/stocksync
cp deployment/.env.example deployment/.env
openssl rand -base64 36
openssl rand -base64 36
id -u stocksync
id -g stocksync
nano deployment/.env
chmod 600 deployment/.env
```

Enter two different database passwords and set `APP_UID`/`APP_GID` to the displayed `stocksync` IDs. Do not place cPanel database names or credentials in this file.

## 8. Start Production

```bash
cd /opt/stocksync
./deployment/scripts/start.sh
./deployment/scripts/status.sh
```

The start script validates `.env`, validates Compose, builds images, starts in dependency order, waits for all health checks, and prints the application URL.

## 9. Initial IP Test

Test before changing DNS:

```bash
curl -I http://66.116.253.40/
curl http://66.116.253.40/api/v1/health
curl -i -c /tmp/stocksync-cookies http://66.116.253.40/api/v1/auth/csrf
docker compose --project-directory deployment --env-file deployment/.env -f deployment/docker-compose.prod.yml ps
sudo ss -lntp | grep -E ':(80|3306|8081)\b'
```

Expected exposure: Nginx on public port `80`; no host listeners created by this stack for MySQL `3306` or backend `8081`. The CSRF response must set `XSRF-TOKEN`, and login must work through the same Nginx origin.

## 10. Verify Persistence

Create a small test record and one generated PDF through the application, then restart containers:

```bash
docker compose --project-directory deployment --env-file deployment/.env -f deployment/docker-compose.prod.yml restart
./deployment/scripts/status.sh
find deployment/storage -maxdepth 4 -type f
```

Confirm the test record and generated PDF remain available. Never run `docker compose down -v` in production.

## 11. Back Up and Restore

Create and validate a compressed daily backup:

```bash
./deployment/scripts/backup-database.sh
ls -lh deployment/storage/backups
gzip -t deployment/storage/backups/stocksync-*.sql.gz
```

Backups are retained for `BACKUP_RETENTION_DAYS`. Restore requires a filename and explicit `RESTORE` confirmation:

```bash
./deployment/scripts/restore-database.sh stocksync-YYYYMMDD-HHMMSS.sql.gz
```

The restore script is locked to the independent `shuttering_inventory` database and cannot target the cPanel databases.

## 12. Daily Operations

```bash
./deployment/scripts/status.sh
./deployment/scripts/logs.sh 200
./deployment/scripts/stop.sh
./deployment/scripts/start.sh
```

`stop.sh` removes containers and the private network but preserves the MySQL volume and bind-mounted files.

## 13. DNS

The `stocksync.caretakers.ind.in` A record must point to `66.116.253.40`. Do not change the root `caretakers.ind.in` cPanel website.

## 14. HTTPS

After DNS resolves, issue the first certificate and install the renewal job:

```bash
cd /opt/stocksync
./deployment/scripts/issue-certificate.sh
(crontab -l 2>/dev/null; echo '17 3 * * * /opt/stocksync/deployment/scripts/renew-certificate.sh >> /opt/stocksync/deployment/storage/certbot-renew.log 2>&1') | crontab -
./deployment/scripts/renew-certificate.sh
```

Certificate state is persisted below `deployment/certbot/conf` and excluded from deployment archives. The MySQL and backend containers remain private.
