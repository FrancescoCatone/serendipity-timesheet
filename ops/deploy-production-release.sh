#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-/opt/serendipity-timesheet}"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env.production}"
COMPOSE_FILE="${COMPOSE_FILE:-$PROJECT_DIR/docker-compose.prod.yml}"
WORK_DIR="${WORK_DIR:-/tmp/serendipity-deploy}"

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 /absolute/path/to/deploy-bundle.tgz" >&2
  exit 1
fi

BUNDLE_FILE="$1"

if [[ ! -f "$BUNDLE_FILE" ]]; then
  echo "Deploy bundle not found: $BUNDLE_FILE" >&2
  exit 1
fi

mkdir -p "$PROJECT_DIR" "$WORK_DIR"

if [[ -f "$PROJECT_DIR/ops/backup-production-db.sh" ]]; then
  echo "Creating safety backup before deploy"
  bash "$PROJECT_DIR/ops/backup-production-db.sh"
else
  echo "Skipping pre-deploy backup: backup script not found yet"
fi

echo "Extracting deploy bundle into $PROJECT_DIR"
tar -xzf "$BUNDLE_FILE" -C "$PROJECT_DIR"

chmod +x "$PROJECT_DIR"/ops/*.sh

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE" >&2
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Missing compose file: $COMPOSE_FILE" >&2
  exit 1
fi

cd "$PROJECT_DIR"

echo "Starting docker compose deploy"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build

BACKEND_CONTAINER_ID="$(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps -q backend)"
if [[ -z "$BACKEND_CONTAINER_ID" ]]; then
  echo "Backend container not found after deploy" >&2
  exit 1
fi

echo "Waiting for backend healthcheck"
for attempt in {1..24}; do
  HEALTH_STATUS="$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$BACKEND_CONTAINER_ID")"
  if [[ "$HEALTH_STATUS" == "healthy" ]]; then
    echo "Backend is healthy."
    break
  fi

  if [[ "$HEALTH_STATUS" == "unhealthy" ]]; then
    echo "Backend reported unhealthy status." >&2
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs backend --tail 200
    exit 1
  fi

  if [[ "$attempt" -eq 24 ]]; then
    echo "Timed out while waiting for backend healthcheck. Last status: $HEALTH_STATUS" >&2
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs backend --tail 200
    exit 1
  fi

  sleep 5
done

echo "Container status after deploy"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

echo "Backend logs tail"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs backend --tail 100

echo "Deploy completed successfully."
