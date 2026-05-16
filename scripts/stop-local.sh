#!/usr/bin/env bash
# Stop and remove the local stack containers.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

cd "${REPO_ROOT}"

if [[ -f "${LOCAL_ENV_FILE}" ]]; then
    docker_compose --env-file "${LOCAL_ENV_FILE}" -f "${LOCAL_COMPOSE_FILE}" down "$@"
else
    docker_compose -f "${LOCAL_COMPOSE_FILE}" down "$@"
fi
