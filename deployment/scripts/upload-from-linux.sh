#!/usr/bin/env bash
set -euo pipefail

SERVER="66.116.253.40"
USER="stocksync"
REMOTE_DIR="/opt/stocksync"

TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
ARCHIVE_NAME="stocksync-deployment-$TIMESTAMP.tar.gz"
ARCHIVE_PATH="/tmp/$ARCHIVE_NAME"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "Creating deployment archive $ARCHIVE_NAME..."
cd "$PROJECT_ROOT"

tar -czf "$ARCHIVE_PATH" \
    --exclude=.git \
    --exclude=backend/target \
    --exclude=backend/src/test \
    --exclude=frontend/node_modules \
    --exclude=frontend/dist \
    --exclude=frontend/coverage \
    --exclude=deployment/.env \
    --exclude='deployment/certbot/conf/*' \
    --exclude='deployment/*.tar.gz' \
    --exclude='deployment/storage/uploads/*' \
    --exclude='deployment/storage/documents/*' \
    --exclude='deployment/storage/reports/*' \
    --exclude='deployment/storage/backups/*' \
    backend frontend deployment

if [[ ! -f "$ARCHIVE_PATH" ]]; then
    echo "Error: Failed to create archive."
    exit 1
fi

echo "Uploading $ARCHIVE_NAME to $USER@$SERVER:$REMOTE_DIR/..."
scp "$ARCHIVE_PATH" "$USER@$SERVER:$REMOTE_DIR/"

echo ""
echo "Upload complete! Please run the following commands on your server:"
echo ""
echo "ssh $USER@$SERVER"
echo "cd $REMOTE_DIR"
echo "tar -xzf $ARCHIVE_NAME"
echo "chmod +x deployment/scripts/*.sh backend/mvnw"
echo "./deployment/scripts/start.sh"
echo ""
