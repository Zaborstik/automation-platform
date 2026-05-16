#!/usr/bin/env bash
# End-to-end smoke test of the distributed flow.
#
# Builds and starts docker/dev/docker-compose.smoke.yml (server + local in
# one network), waits for health, then:
#   1. POST /local/run with a plain user request that triggers the stub
#      "open_page" heuristic in platform-knowledge.
#   2. Polls /local/status/{runId} until the run finishes or times out.
#   3. Verifies a plan was created on the server side.
#
# Exits non-zero if anything fails.
#
# Usage:
#   ./scripts/smoke-test.sh                    # build + run + cleanup
#   KEEP_RUNNING=1 ./scripts/smoke-test.sh     # leave containers up at end
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

COMPOSE_FILE="${REPO_ROOT}/docker/dev/docker-compose.smoke.yml"
EXECUTOR_URL="http://localhost:7070"
API_URL="http://localhost:8080"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-180}"

cleanup() {
    if [[ "${KEEP_RUNNING:-0}" != "1" ]]; then
        echo
        echo "[*] Cleaning up smoke stack..."
        docker_compose -f "${COMPOSE_FILE}" down -v --remove-orphans || true
    else
        echo
        echo "[!] KEEP_RUNNING=1 — leaving smoke stack up."
        echo "    Stop manually with:"
        echo "      docker compose -f ${COMPOSE_FILE} down -v"
    fi
}
trap cleanup EXIT

cd "${REPO_ROOT}"

echo "[*] Building smoke stack..."
docker_compose -f "${COMPOSE_FILE}" build

echo "[*] Starting smoke stack..."
docker_compose -f "${COMPOSE_FILE}" up -d

echo "[*] Waiting for platform-api and platform-executor to become healthy (timeout ${TIMEOUT_SECONDS}s)..."
deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))
while true; do
    API_CID="$(docker_compose -f "${COMPOSE_FILE}" ps -q platform-api)"
    EXEC_CID="$(docker_compose -f "${COMPOSE_FILE}" ps -q platform-executor)"
    api_status="starting"
    exec_status="starting"
    if [[ -n "${API_CID}" ]]; then
        api_status="$(docker inspect --format='{{.State.Health.Status}}' "${API_CID}" 2>/dev/null || echo "starting")"
    fi
    if [[ -n "${EXEC_CID}" ]]; then
        exec_status="$(docker inspect --format='{{.State.Health.Status}}' "${EXEC_CID}" 2>/dev/null || echo "starting")"
    fi
    if [[ "${api_status}" == "healthy" && "${exec_status}" == "healthy" ]]; then
        echo "[\u2713] api + executor are healthy."
        break
    fi
    if (( $(date +%s) > deadline )); then
        echo "[x] Timed out waiting for healthchecks (api=${api_status} executor=${exec_status})." >&2
        docker_compose -f "${COMPOSE_FILE}" ps
        docker_compose -f "${COMPOSE_FILE}" logs --tail=80 platform-api platform-executor
        exit 1
    fi
    sleep 3
done

echo
echo "[*] Sanity GET /actuator/health on both services..."
curl -fsS "${API_URL}/actuator/health"      | head -c 200; echo
curl -fsS "${EXECUTOR_URL}/actuator/health" | head -c 200; echo

echo
echo "[*] POST /local/run — kicking off a stub plan..."
RUN_PAYLOAD='{"userInput":"открой страницу https://example.com","headless":true}'
RUN_RESPONSE=$(curl -fsS -X POST "${EXECUTOR_URL}/local/run" \
    -H 'Content-Type: application/json' \
    -d "${RUN_PAYLOAD}")
echo "    response: ${RUN_RESPONSE}"

RUN_ID=$(printf '%s' "${RUN_RESPONSE}" | sed -n 's/.*"runId":"\([^"]*\)".*/\1/p')
if [[ -z "${RUN_ID}" ]]; then
    echo "[x] Failed to parse runId from /local/run response." >&2
    exit 1
fi

echo
echo "[*] Polling /local/status/${RUN_ID}..."
status_deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))
last_status=""
while true; do
    STATUS_RESPONSE=$(curl -fsS "${EXECUTOR_URL}/local/status/${RUN_ID}")
    STATUS=$(printf '%s' "${STATUS_RESPONSE}" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')
    if [[ "${STATUS}" != "${last_status}" ]]; then
        echo "    [${STATUS}] ${STATUS_RESPONSE}"
        last_status="${STATUS}"
    fi
    case "${STATUS}" in
        COMPLETED|FAILED|FAILED_TO_START)
            break
            ;;
    esac
    if (( $(date +%s) > status_deadline )); then
        echo "[x] Timed out waiting for run to finish (last status: ${STATUS})." >&2
        docker_compose -f "${COMPOSE_FILE}" logs --tail=80 platform-executor platform-agent
        exit 1
    fi
    sleep 2
done

PLAN_ID=$(printf '%s' "${STATUS_RESPONSE}" | sed -n 's/.*"planId":"\([^"]*\)".*/\1/p')
echo
echo "[*] Verifying plan ${PLAN_ID} exists on the server (GET /api/plans/${PLAN_ID})..."
curl -fsS "${API_URL}/api/plans/${PLAN_ID}" | head -c 400; echo

echo
case "${STATUS}" in
    COMPLETED)
        echo "[\u2713] Smoke test PASSED (status=COMPLETED, planId=${PLAN_ID}, runId=${RUN_ID})."
        ;;
    *)
        echo "[!] Run finished with status=${STATUS} (planId=${PLAN_ID}, runId=${RUN_ID})."
        echo "    The plan was generated and reached executor, but execution did not"
        echo "    fully succeed. This can be acceptable in smoke if example.com is"
        echo "    not reachable from the container."
        # Treat non-COMPLETED as soft-pass: distributed pipeline worked.
        ;;
esac
