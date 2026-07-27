#!/usr/bin/env bash
# StockSync one-command launcher for Windows Git Bash.
# Usage: ./start.sh | ./start.sh --status | ./start.sh --stop
set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_DIR="$ROOT_DIR/.stocksync-runtime"
MYSQL_DATA_DIR="$ROOT_DIR/.mysql-stocksync-dev-data"
MYSQL_HOME="${STOCKSYNC_MYSQL_HOME:-C:/tmp/mysql-install/mysql-8.4.10-winx64}"
JDK_HOME="${STOCKSYNC_JAVA_HOME:-C:/tmp/stocksync-build-tools/jdk/jdk-21.0.11+10}"
MAVEN_HOME="${STOCKSYNC_MAVEN_HOME:-C:/tmp/stocksync-build-tools/maven/apache-maven-3.9.11}"

DB_NAME="${STOCKSYNC_DB_NAME:-shuttering_inventory}"
DB_USER="${STOCKSYNC_DB_USER:-inventory_user}"
DB_PASSWORD="${STOCKSYNC_DB_PASSWORD:-inventory_password}"
MYSQL_ROOT_PASSWORD="${STOCKSYNC_MYSQL_ROOT_PASSWORD:-stocksync_root_dev}"
ADMIN_USERNAME="${STOCKSYNC_ADMIN_USERNAME:-admin}"
ADMIN_EMAIL="${STOCKSYNC_ADMIN_EMAIL:-admin@stocksync.local}"
ADMIN_PASSWORD="${STOCKSYNC_ADMIN_PASSWORD:-Password123}"
# BCrypt for Password123. When overriding the admin password, also supply its hash.
ADMIN_PASSWORD_HASH="${STOCKSYNC_ADMIN_PASSWORD_HASH:-\$2b\$10\$BuxOBAjd5fFnARIM0afB1uYK6h6vTNcfR/fKbXkrluOepgFcmLyT2}"

MYSQL_EXE="$MYSQL_HOME/bin/mysqld.exe"
MYSQL_CLIENT="$MYSQL_HOME/bin/mysql.exe"
MYSQL_ADMIN="$MYSQL_HOME/bin/mysqladmin.exe"
MYSQL_PID_FILE="$RUNTIME_DIR/mysql.pid"
BACKEND_PID_FILE="$RUNTIME_DIR/backend.pid"
FRONTEND_PID_FILE="$RUNTIME_DIR/frontend.pid"
mkdir -p "$RUNTIME_DIR"

info() { printf '\033[0;34m[INFO]\033[0m %s\n' "$*"; }
ok()   { printf '\033[0;32m[OK]\033[0m %s\n' "$*"; }
fail() { printf '\033[0;31m[ERROR]\033[0m %s\n' "$*" >&2; exit 1; }

windows_path() {
  if command -v cygpath >/dev/null 2>&1; then cygpath -aw "$1"; else printf '%s' "$1" | sed 's#/#\\#g'; fi
}

tcp_ready() {
  local port="$1"
  (exec 3<>"/dev/tcp/127.0.0.1/$port") >/dev/null 2>&1
}

wait_for_tcp() {
  local port="$1" name="$2" timeout="$3" elapsed=0
  info "Waiting for $name on port $port..."
  until tcp_ready "$port"; do
    (( elapsed >= timeout )) && return 1
    sleep 1
    elapsed=$((elapsed + 1))
  done
  ok "$name is ready."
}

http_ready() { curl --silent --fail --max-time 2 "$1" >/dev/null 2>&1; }

wait_for_http() {
  local url="$1" name="$2" timeout="$3" elapsed=0
  info "Waiting for $name..."
  until http_ready "$url"; do
    (( elapsed >= timeout )) && return 1
    sleep 1
    elapsed=$((elapsed + 1))
  done
  ok "$name is ready."
}

read_pid() { local file="$1"; [[ -f "$file" ]] && tr -dc '0-9' < "$file"; }

stop_pid_file() {
  local file="$1" name="$2" pid
  pid="$(read_pid "$file")"
  if [[ -n "$pid" ]]; then
    powershell.exe -NoProfile -Command \
      "\$p=Get-Process -Id $pid -ErrorAction SilentlyContinue; if(\$p){Stop-Process -Id $pid -Force}" \
      >/dev/null 2>&1 || true
    rm -f "$file"
    ok "$name stopped."
  fi
}

mysql_app() {
  "$MYSQL_CLIENT" --protocol=TCP -h 127.0.0.1 -P 3306 \
    -u "$DB_USER" "--password=$DB_PASSWORD" "$@"
}

initialize_mysql() {
  [[ -x "$MYSQL_EXE" ]] || fail "MySQL 8.4 was not found at $MYSQL_EXE"
  if [[ ! -d "$MYSQL_DATA_DIR/mysql" ]]; then
    info "Initializing project-local MySQL 8.4..."
    rm -rf "$MYSQL_DATA_DIR"
    mkdir -p "$MYSQL_DATA_DIR"
    (cd "$MYSQL_HOME/bin" && ./mysqld.exe --no-defaults --initialize-insecure --console \
      "--basedir=$(windows_path "$MYSQL_HOME")" \
      "--datadir=$(windows_path "$MYSQL_DATA_DIR")" \
      >"$RUNTIME_DIR/mysql-initialize.log" 2>&1) ||
      fail "MySQL initialization failed. See .stocksync-runtime/mysql-initialize.log"
    # The portable Windows MySQL package recreates these empty first-start
    # undo tablespaces. No application data exists at this point.
    rm -f "$MYSQL_DATA_DIR/undo_001" "$MYSQL_DATA_DIR/undo_002" \
      "$MYSQL_DATA_DIR/undo_1_trunc.log"
    touch "$RUNTIME_DIR/mysql-needs-provisioning"
  fi
}

start_mysql() {
  if tcp_ready 3306; then
    mysql_app -e "SELECT 1" >/dev/null 2>&1 ||
      fail "Port 3306 is occupied, but StockSync cannot connect as $DB_USER."
    ok "Using MySQL already running on port 3306."
    return
  fi

  initialize_mysql
  info "Starting project-local MySQL..."
  (cd "$MYSQL_HOME/bin" && ./mysqld.exe --no-defaults \
    "--basedir=$(windows_path "$MYSQL_HOME")" \
    "--datadir=$(windows_path "$MYSQL_DATA_DIR")" \
    --port=3306 --bind-address=127.0.0.1 \
    "--pid-file=$(windows_path "$MYSQL_DATA_DIR/mysql.pid")" --console \
    >"$RUNTIME_DIR/mysql.log" 2>&1) &
  echo "$!" > "$MYSQL_PID_FILE"
  wait_for_tcp 3306 "MySQL" 30 ||
    fail "MySQL failed to start. See .stocksync-runtime/mysql.log"

  if [[ -f "$RUNTIME_DIR/mysql-needs-provisioning" ]]; then
    info "Creating database and local database user..."
    "$MYSQL_CLIENT" -h localhost -P 3306 -u root <<SQL ||
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'localhost';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'127.0.0.1';
ALTER USER 'root'@'localhost' IDENTIFIED BY '$MYSQL_ROOT_PASSWORD';
FLUSH PRIVILEGES;
SQL
      fail "MySQL provisioning failed."
    rm -f "$RUNTIME_DIR/mysql-needs-provisioning"
  fi
  mysql_app -e "SELECT 1" >/dev/null 2>&1 || fail "The StockSync database login failed."
}

start_backend() {
  if http_ready "http://127.0.0.1:8081/actuator/health"; then
    ok "Backend is already healthy on port 8081."
    return
  fi
  tcp_ready 8081 && fail "Port 8081 is occupied by an unhealthy/non-StockSync process."
  [[ -d "$JDK_HOME" ]] || fail "Java 21 was not found at $JDK_HOME"
  export JAVA_HOME="$JDK_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
  export DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME DB_USER DB_PASSWORD
  local maven_command
  local maven_repo
  maven_repo="$(windows_path "$ROOT_DIR/.m2")"
  if [[ -f "$MAVEN_HOME/bin/mvn.cmd" ]]; then
    maven_command="$MAVEN_HOME/bin/mvn.cmd"
  else
    maven_command="$ROOT_DIR/backend/mvnw.cmd"
  fi

  info "Starting Spring Boot backend..."
  (cd "$ROOT_DIR/backend" && "$maven_command" \
    "-Dmaven.repo.local=$maven_repo" spring-boot:run "-Dspring-boot.run.profiles=local") \
    >"$RUNTIME_DIR/backend.log" 2>&1 &
  echo "$!" > "$BACKEND_PID_FILE"
  wait_for_http "http://127.0.0.1:8081/actuator/health" "Backend" 90 ||
    fail "Backend failed. See .stocksync-runtime/backend.log"
}

seed_local_admin() {
  info "Ensuring the local ADMIN test account exists..."
  mysql_app "$DB_NAME" <<SQL ||
INSERT INTO users
  (full_name, username, email, password_hash, active, created_by, updated_by)
SELECT 'Admin User', '$ADMIN_USERNAME', '$ADMIN_EMAIL', '$ADMIN_PASSWORD_HASH',
       TRUE, 'local-startup', 'local-startup'
WHERE NOT EXISTS (
  SELECT 1 FROM users
  WHERE LOWER(username)=LOWER('$ADMIN_USERNAME') OR LOWER(email)=LOWER('$ADMIN_EMAIL')
);
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT id, 'ROLE_ADMIN' FROM users WHERE LOWER(username)=LOWER('$ADMIN_USERNAME');
SQL
    fail "Could not create/verify the local admin account."
}

start_frontend() {
  if http_ready "http://127.0.0.1:5173"; then
    ok "Frontend is already running on port 5173."
    return
  fi
  tcp_ready 5173 && fail "Port 5173 is occupied by a non-StockSync process."
  command -v npm.cmd >/dev/null 2>&1 || fail "Node.js/npm is not on PATH."
  if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
    info "Installing frontend dependencies..."
    (cd "$ROOT_DIR/frontend" && npm.cmd install) || fail "npm install failed."
  fi
  info "Starting Vite frontend..."
  (cd "$ROOT_DIR/frontend" && npm.cmd run dev -- --host 127.0.0.1) \
    >"$RUNTIME_DIR/frontend.log" 2>&1 &
  echo "$!" > "$FRONTEND_PID_FILE"
  wait_for_http "http://127.0.0.1:5173" "Frontend" 30 ||
    fail "Frontend failed. See .stocksync-runtime/frontend.log"
}

show_status() {
  printf '\nStockSync local services\n'
  if tcp_ready 3306; then printf '  MySQL:    running (127.0.0.1:3306)\n'; else printf '  MySQL:    stopped\n'; fi
  if http_ready "http://127.0.0.1:8081/actuator/health"; then printf '  Backend:  healthy (http://127.0.0.1:8081)\n'; else printf '  Backend:  stopped/unhealthy\n'; fi
  if http_ready "http://127.0.0.1:5173"; then printf '  Frontend: running (http://127.0.0.1:5173)\n'; else printf '  Frontend: stopped\n'; fi
}

stop_all() {
  info "Stopping services launched by start.sh..."
  stop_pid_file "$FRONTEND_PID_FILE" "Frontend"
  stop_pid_file "$BACKEND_PID_FILE" "Backend"
  if [[ -f "$MYSQL_PID_FILE" ]] && tcp_ready 3306; then
    "$MYSQL_ADMIN" -h localhost -P 3306 \
      -u root "--password=$MYSQL_ROOT_PASSWORD" shutdown >/dev/null 2>&1 || true
  fi
  stop_pid_file "$MYSQL_PID_FILE" "MySQL"
  show_status
}

start_all() {
  cd "$ROOT_DIR"
  start_mysql
  start_backend
  seed_local_admin
  start_frontend
  printf '\n\033[0;32mStockSync is ready.\033[0m\n'
  printf '  App URL:        http://127.0.0.1:5173/login\n'
  printf '  Backend health: http://127.0.0.1:8081/actuator/health\n'
  printf '  Login username: %s\n' "$ADMIN_USERNAME"
  printf '  Login password: %s\n' "$ADMIN_PASSWORD"
  printf '  Workbench:      %s / %s at 127.0.0.1:3306\n' "$DB_USER" "$DB_PASSWORD"
  printf '\nLogs: .stocksync-runtime/   Stop: ./start.sh --stop\n'
}

case "${1:-start}" in
  start|--local|-l) start_all ;;
  --stop|-s|stop) stop_all ;;
  --status|status) show_status ;;
  *) printf 'Usage: ./start.sh [start|--stop|--status]\n'; exit 2 ;;
esac
