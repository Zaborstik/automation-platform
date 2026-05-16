#!/usr/bin/env bash
# Tail logs of the local stack. Pass service names as args to filter:
#   ./scripts/logs-local.sh platform-executor
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

cd "${REPO_ROOT}"

if [[ -f "${LOCAL_ENV_FILE}" ]]; then
    docker_compose --env-file "${LOCAL_ENV_FILE}" -f "${LOCAL_COMPOSE_FILE}" logs -f "$@"
else
    docker_compose -f "${LOCAL_COMPOSE_FILE}" logs -f "$@"
fi
