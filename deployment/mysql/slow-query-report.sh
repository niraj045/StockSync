#!/usr/bin/env bash
# Prints the slowest normalized statements recorded by MySQL's slow-query log.
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.production}"

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

compose=(docker compose --project-name stocksync-production --project-directory "$ROOT_DIR" --env-file "$ENV_FILE" -f "$ROOT_DIR/compose.production.yaml")

"${compose[@]}" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql mysql -uroot --table <<'SQL'
SELECT
  start_time,
  query_time,
  rows_examined,
  rows_sent,
  LEFT(REPLACE(REPLACE(sql_text, '\n', ' '), '\r', ' '), 300) AS statement
FROM slow_log
WHERE db = DATABASE()
   OR db IS NULL
ORDER BY query_time DESC
LIMIT 50;
SQL

printf '\nUse EXPLAIN ANALYZE <statement> for entries with high rows_examined.\n'
