# StockSync Selenium E2E

This separate Node.js project runs a non-destructive browser story against a
running local StockSync stack. It verifies:

- anonymous quotation access redirects to login;
- the local ADMIN account can sign in;
- master-data, inventory, opening-stock, quotation, user, and audit pages load.

## Run

Start StockSync from Git Bash:

```bash
./start.sh
```

In another terminal:

```powershell
cd C:\stockSync\e2e
npm install
npm test
```

Chrome opens visibly by default. For a headless run:

```powershell
npm run test:headless
```

Optional environment overrides:

```powershell
$env:E2E_BASE_URL = "http://127.0.0.1:5173"
$env:E2E_USERNAME = "admin"
$env:E2E_PASSWORD = "Password123"
$env:E2E_TIMEOUT_MS = "30000"
npm test
```

On failure, a screenshot is saved under `e2e/artifacts/`.

The scenario deliberately does not post opening stock or create inventory
transactions. Destructive/data-creating workflows should use a disposable
database with known fixtures.

## Real-world daily operation

The data-creating scenario logs in as ADMIN and uses the UI to create a unique
category, quotation template, item, customer party, customer site, and
quotation. It then sends and approves the quotation and checks audit activity.

```powershell
npm run test:daily
```

Headless:

```powershell
npm run test:daily:headless
```

Every run uses timestamped `E2E-...` codes so it does not collide with earlier
runs. It intentionally leaves the demonstration records in the database for
inspection. Quotations do not create stock transactions or alter balances.
