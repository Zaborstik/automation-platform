#!/usr/bin/env bash
# Tail logs of the server stack.
#   ./scripts/logs-server.sh platform-api
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

cd "${REPO_ROOT}"

if [[ -f "${SERVER_ENV_FILE}" ]]; then
    docker_compose --env-file "${SERVER_ENV_FILE}" -f "${SERVER_COMPOSE_FILE}" logs -f "$@"
else
    docker_compose -f "${SERVER_COMPOSE_FILE}" logs -f "$@"
fi
