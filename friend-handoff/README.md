# StockSync populated testing handoff

Install Docker Desktop or Docker Engine with Compose v2, clone this commit, and
run from the repository root:

```bash
chmod +x setup-friend.sh
./setup-friend.sh
```

The script builds the application, starts MySQL 8.4, restores the populated E2E
database on first start, mounts the generated PDFs/report exports, and waits
until the application is healthy.

- App: `http://127.0.0.1:8088/login`
- Login: `admin` / `Password123`
- MySQL: `127.0.0.1:3309`
- Database: `shuttering_inventory_e2e`
- Database user: `inventory_user` / `inventory_password`

The credentials and data are strictly for local testing.

Useful commands:

```bash
./setup-friend.sh --status
./setup-friend.sh --stop
./setup-friend.sh --reset
```

`--stop` preserves the populated database. `--reset` deletes only the
`stocksync-friend` Docker volume and restores the bundled snapshot again.

Override ports if needed:

```bash
FRIEND_APP_PORT=8090 FRIEND_DB_PORT=3310 ./setup-friend.sh
```
