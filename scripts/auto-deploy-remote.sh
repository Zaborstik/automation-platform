#!/usr/bin/env bash
# Автодеплой серверного стека (postgres + platform-api + platform-knowledge) на
# удалённый хост по SSH: rsync репозитория → на сервере ./scripts/deploy-server.sh
#
# Минимальная настройка — файл .env.deploy в корне репозитория (см. .env.deploy.example):
#   DEPLOY_REMOTE_HOST=1.2.3.4
#   DEPLOY_REMOTE_USER=ubuntu
#   DEPLOY_REMOTE_PASSWORD=***   # или оставьте пустым и используйте SSH-ключ
#
# Запуск с вашей машины (из корня репозитория):
#   ./scripts/auto-deploy-remote.sh
#
# Или одноразово без файла:
#   DEPLOY_REMOTE_HOST=1.2.3.4 DEPLOY_REMOTE_USER=ubuntu DEPLOY_REMOTE_PASSWORD=secret \
#     ./scripts/auto-deploy-remote.sh
#
# Требования на сервере: Docker + docker compose v2, открытый SSH.
# Требования локально: rsync, ssh; для пароля — пакет sshpass.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

DEPLOY_ENV_FILE="${REPO_ROOT}/.env.deploy"

if [[ -f "${DEPLOY_ENV_FILE}" ]]; then
    set -a
    # shellcheck source=/dev/null
    source "${DEPLOY_ENV_FILE}"
    set +a
fi

DEPLOY_REMOTE_HOST="${DEPLOY_REMOTE_HOST:-${1:-}}"
DEPLOY_REMOTE_USER="${DEPLOY_REMOTE_USER:-ubuntu}"
DEPLOY_REMOTE_DIR="${DEPLOY_REMOTE_DIR:-/home/ubuntu/automation-platform}"
DEPLOY_REMOTE_PASSWORD="${DEPLOY_REMOTE_PASSWORD:-${DEPLOY_SSH_PASSWORD:-}}"
DEPLOY_SERVER_EXTRA_ARGS="${DEPLOY_SERVER_EXTRA_ARGS:-}"
STRICT="${DEPLOY_SSH_STRICT_HOSTKEY:-accept-new}"

if [[ -z "${DEPLOY_REMOTE_HOST}" ]]; then
    echo "Задайте DEPLOY_REMOTE_HOST в .env.deploy (см. .env.deploy.example) или первым аргументом." >&2
    exit 1
fi

REMOTE="${DEPLOY_REMOTE_USER}@${DEPLOY_REMOTE_HOST}"
USE_SSHPASS=false
if [[ -n "${DEPLOY_REMOTE_PASSWORD}" ]]; then
    if ! command -v sshpass >/dev/null 2>&1; then
        echo "Для пароля SSH нужен sshpass (brew install sshpass / apt install sshpass)." >&2
        exit 1
    fi
    export SSHPASS="${DEPLOY_REMOTE_PASSWORD}"
    USE_SSHPASS=true
fi

remote_run() {
    if $USE_SSHPASS; then
        sshpass -e ssh -o BatchMode=no -o "StrictHostKeyChecking=${STRICT}" "$REMOTE" "$@"
    else
        ssh -o BatchMode=no -o "StrictHostKeyChecking=${STRICT}" "$REMOTE" "$@"
    fi
}

scp_to_remote() {
    local src="$1"
    local dst="$2"
    if $USE_SSHPASS; then
        sshpass -e scp -o "StrictHostKeyChecking=${STRICT}" "${src}" "${REMOTE}:${dst}"
    else
        scp -o "StrictHostKeyChecking=${STRICT}" "${src}" "${REMOTE}:${dst}"
    fi
}

echo "[*] Проверка SSH: ${REMOTE} ..."
remote_run 'echo "SSH OK ($(hostname))"' </dev/null

echo "[*] Проверка Docker на сервере..."
remote_run 'docker --version && (docker compose version || docker-compose --version)'

echo "[*] Rsync проекта на сервер (${DEPLOY_REMOTE_DIR}) ..."
REPO_TRAILING="${REPO_ROOT}/"
if $USE_SSHPASS; then
    export RSYNC_RSH="sshpass -e ssh -o StrictHostKeyChecking=${STRICT}"
else
    export RSYNC_RSH="ssh -o StrictHostKeyChecking=${STRICT}"
fi

rsync -az --delete --human-readable \
    --exclude '.git' \
    --exclude '.idea' \
    --exclude '**/target' \
    --exclude '.cursor' \
    --exclude '.env.deploy' \
    --exclude 'node_modules' \
    --exclude 'local-demo/api.log' \
    --exclude 'data' \
    "${REPO_TRAILING}" "${REMOTE}:${DEPLOY_REMOTE_DIR}/"

SERVER_ENV_REL="${DEPLOY_REMOTE_DIR}/docker/server/.env"
remote_run "bash -lc 'mkdir -p \"${DEPLOY_REMOTE_DIR}/docker/server\"'" </dev/null

if [[ -n "${DEPLOY_POSTGRES_PASSWORD:-}" ]]; then
    echo "[*] Загружаем docker/server/.env (пароль БД из DEPLOY_POSTGRES_PASSWORD)..."
    tmp_env="$(mktemp)"
    {
        echo "POSTGRES_DB=platformdb"
        echo "POSTGRES_USER=platform"
        echo "POSTGRES_PASSWORD=${DEPLOY_POSTGRES_PASSWORD}"
        echo "PLATFORM_API_PORT=8080"
        echo "LOG_LEVEL=INFO"
    } >"${tmp_env}"
    scp_to_remote "${tmp_env}" "${SERVER_ENV_REL}"
    rm -f "${tmp_env}"
elif [[ -f "${REPO_ROOT}/docker/server/.env" ]]; then
    echo "[*] Локальный docker/server/.env уже попал на сервер через rsync."
else
    echo "[!] Нет ни DEPLOY_POSTGRES_PASSWORD, ни локального docker/server/.env — на сервере берётся .env.example."
    remote_run "bash -lc 'cd \"${DEPLOY_REMOTE_DIR}\" && if [[ ! -f docker/server/.env ]]; then cp docker/server/.env.example docker/server/.env; echo \"ВНИМАНИЕ: отредактируйте docker/server/.env на сервере.\" >&2; fi'" </dev/null
fi

echo "[*] Запуск deploy на сервере: scripts/deploy-server.sh ${DEPLOY_SERVER_EXTRA_ARGS}..."
# DEPLOY_SERVER_EXTRA_ARGS доверяем только вашему .env.deploy (например: --pull)
remote_run "bash -lc 'cd \"${DEPLOY_REMOTE_DIR}\" && chmod +x scripts/*.sh 2>/dev/null || true && exec ./scripts/deploy-server.sh ${DEPLOY_SERVER_EXTRA_ARGS}'" </dev/null

echo
echo "[✓] Готово. API: http://${DEPLOY_REMOTE_HOST}:8080 (порт см. PLATFORM_API_PORT в docker/server/.env на сервере)."
echo "    Логи на сервере: ssh ${REMOTE} \"cd ${DEPLOY_REMOTE_DIR} && ./scripts/logs-server.sh\""
