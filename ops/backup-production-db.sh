#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-/opt/serendipity-timesheet}"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env.production}"
COMPOSE_FILE="${COMPOSE_FILE:-$PROJECT_DIR/docker-compose.prod.yml}"
BACKUP_DIR="${BACKUP_DIR:-/opt/serendipity-backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE" >&2
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Missing compose file: $COMPOSE_FILE" >&2
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"

mkdir -p "$BACKUP_DIR"

timestamp="$(date +%Y-%m-%d_%H-%M-%S)"
backup_file="$BACKUP_DIR/${POSTGRES_DB}_${timestamp}.dump"
latest_link="$BACKUP_DIR/latest.dump"

echo "Creating PostgreSQL backup in $backup_file"

docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  exec -T db pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc > "$backup_file"

ln -sfn "$backup_file" "$latest_link"

find "$BACKUP_DIR" -maxdepth 1 -type f -name "${POSTGRES_DB}_*.dump" -mtime +"$RETENTION_DAYS" -delete

echo "Backup completed: $backup_file"
