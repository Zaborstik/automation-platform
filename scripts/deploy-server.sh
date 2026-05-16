#!/usr/bin/env bash
# Bring the server-side stack up: postgres + platform-api + platform-knowledge.
#
# Usage:
#   ./scripts/deploy-server.sh            # build local images and start
#   ./scripts/deploy-server.sh --pull     # pull pre-built images (uses
#                                         #   API_IMAGE / KNOWLEDGE_IMAGE
#                                         #   set in docker/server/.env)
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

PULL=false
for arg in "$@"; do
    case "$arg" in
        --pull) PULL=true ;;
        *) echo "Unknown arg: $arg" >&2; exit 1 ;;
    esac
done

ensure_env_file "${SERVER_ENV_FILE}" "${SERVER_ENV_EXAMPLE}"

cd "${REPO_ROOT}"

if $PULL; then
    echo "[*] Pulling pre-built images defined in ${SERVER_ENV_FILE}..."
    docker_compose --env-file "${SERVER_ENV_FILE}" -f "${SERVER_COMPOSE_FILE}" pull
    echo "[*] Starting server stack..."
    docker_compose --env-file "${SERVER_ENV_FILE}" -f "${SERVER_COMPOSE_FILE}" up -d
else
    echo "[*] Building server images locally..."
    docker_compose --env-file "${SERVER_ENV_FILE}" -f "${SERVER_COMPOSE_FILE}" build
    echo "[*] Starting server stack..."
    docker_compose --env-file "${SERVER_ENV_FILE}" -f "${SERVER_COMPOSE_FILE}" up -d
fi

echo
echo "[*] Waiting for platform-api to become healthy..."
for i in $(seq 1 30); do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' platform-api 2>/dev/null || echo "starting")
    if [[ "${STATUS}" == "healthy" ]]; then
        echo "[\u2713] platform-api is healthy."
        break
    fi
    sleep 2
done

echo
echo "Server stack status:"
docker_compose --env-file "${SERVER_ENV_FILE}" -f "${SERVER_COMPOSE_FILE}" ps
echo
echo "platform-api should now be reachable on http://<this-host>:${PLATFORM_API_PORT:-8080}"
