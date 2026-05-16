#!/usr/bin/env bash
# Start the local stack (executor + agent + playwright sidecar).
#
# Requires docker/local/.env with PLATFORM_API_URL pointing at your server.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

ensure_env_file "${LOCAL_ENV_FILE}" "${LOCAL_ENV_EXAMPLE}"

# Friendly sanity check: refuse to start with the placeholder server URL.
if grep -q '^PLATFORM_API_URL=https://your-server.example.com' "${LOCAL_ENV_FILE}"; then
    echo "[!] docker/local/.env still has the placeholder PLATFORM_API_URL." >&2
    echo "    Update it with your real server address before starting." >&2
    exit 1
fi

cd "${REPO_ROOT}"

echo "[*] Building/starting local stack..."
docker_compose --env-file "${LOCAL_ENV_FILE}" -f "${LOCAL_COMPOSE_FILE}" up -d --build

echo
echo "[*] Waiting for platform-executor to become healthy..."
for i in $(seq 1 30); do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' platform-executor 2>/dev/null || echo "starting")
    if [[ "${STATUS}" == "healthy" ]]; then
        echo "[\u2713] platform-executor is healthy."
        break
    fi
    sleep 2
done

echo
docker_compose --env-file "${LOCAL_ENV_FILE}" -f "${LOCAL_COMPOSE_FILE}" ps
echo
echo "Local executor URL:    http://localhost:${PLATFORM_EXECUTOR_PORT:-7070}"
echo "Local agent URL:       (internal) http://platform-agent:7071"
echo "Playwright sidecar:    (internal) http://playwright-server:3000"
echo
echo "Point your chat-overlay at the executor URL above."
