#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_FILE="${1:-$ROOT_DIR/deploy-bundle.tgz}"

echo "Creating deploy bundle: $OUTPUT_FILE"

rm -f "$OUTPUT_FILE"

tar \
  --exclude='.git' \
  --exclude='.github' \
  --exclude='.idea' \
  --exclude='.vscode' \
  --exclude='.cache_ggshield' \
  --exclude='backend/target' \
  --exclude='frontend/node_modules' \
  --exclude='frontend/dist' \
  --exclude='deploy-bundle.tgz' \
  --exclude='latest.dump' \
  --exclude='serendipity_db.dump' \
  --exclude='PRODUCTION_DB_BACKUP.md' \
  --exclude='PRODUCTION_RELEASE_CHECKLIST.md' \
  --exclude='DEPLOY_HANDOFF.md' \
  --exclude='commercial' \
  -czf "$OUTPUT_FILE" \
  -C "$ROOT_DIR" \
  backend \
  frontend \
  ops \
  docker-compose.prod.yml \
  .env.production.example \
  README.md

echo "Deploy bundle created successfully."
