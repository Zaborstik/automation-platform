#!/usr/bin/env bash
# Shared helpers for the deploy/local scripts.
set -euo pipefail

# Repo root (one level above this script).
REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
export REPO_ROOT

# Compose files & env files for server/local stacks.
SERVER_COMPOSE_FILE="${REPO_ROOT}/docker/server/docker-compose.yml"
SERVER_ENV_FILE="${REPO_ROOT}/docker/server/.env"
SERVER_ENV_EXAMPLE="${REPO_ROOT}/docker/server/.env.example"

LOCAL_COMPOSE_FILE="${REPO_ROOT}/docker/local/docker-compose.yml"
LOCAL_ENV_FILE="${REPO_ROOT}/docker/local/.env"
LOCAL_ENV_EXAMPLE="${REPO_ROOT}/docker/local/.env.example"

export SERVER_COMPOSE_FILE SERVER_ENV_FILE SERVER_ENV_EXAMPLE
export LOCAL_COMPOSE_FILE LOCAL_ENV_FILE LOCAL_ENV_EXAMPLE

# Pick the docker compose CLI binary (v2 plugin or legacy).
docker_compose() {
    if docker compose version >/dev/null 2>&1; then
        docker compose "$@"
    elif command -v docker-compose >/dev/null 2>&1; then
        docker-compose "$@"
    else
        echo "docker compose CLI not found. Install Docker Desktop or docker-compose." >&2
        exit 1
    fi
}

ensure_env_file() {
    local env_file="$1"
    local example_file="$2"
    if [[ ! -f "$env_file" ]]; then
        if [[ -f "$example_file" ]]; then
            echo "[!] $env_file not found; copying from $example_file"
            cp "$example_file" "$env_file"
            echo "    Edit it before re-running this script."
            exit 1
        else
            echo "[!] Neither $env_file nor $example_file exists; aborting." >&2
            exit 1
        fi
    fi
}
