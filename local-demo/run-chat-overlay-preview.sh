#!/usr/bin/env bash
# =============================================================================
#  Просмотр: Playwright (целевая страница + курсор) + десктоп-панель чата (Tauri).
#
#  Запуск (из корня репозитория):
#    ./local-demo/run-chat-overlay-preview.sh
#
#  Требуется Rust (cargo). Скрипт подхватывает ~/.cargo/env от rustup — как в интерактивном zsh.
#
#  Делает: при необходимости cargo build --release → Playwright-сервер → Chromium
#  с тестовой страницей → бинарник platform-chat-overlay (системный WebView).
#
#  Порт Playwright по умолчанию 3010 (не пересекается с run-demo.sh на 3000).
#  Переопределение: CHAT_PREVIEW_PORT=3020 ./local-demo/run-chat-overlay-preview.sh
#
#  Если Chromium не скачан: cd platform-agent/src/main/resources && npx playwright install chromium
#
#  Первая сборка Rust может занять много минут. Не нажимайте Ctrl+Z — zsh приостановит задачу
#  («suspended»); дождитесь конца или отмените Ctrl+C. Сообщение «Blocking waiting for file lock
#  on artifact directory» часто из‑за второго cargo (IDE, другой терминал); этот скрипт использует
#  отдельный CARGO_TARGET_DIR, чтобы реже конфликтовать с rust-analyzer.
#
#  Конфиг оверлея: platform-agent/src/main/resources/chat-overlay-app.config.json
#  (displayName, iconPath, logFilePath, windowMargin, windowHeightFraction)
# =============================================================================

set -euo pipefail

# rustup ставит cargo в ~/.cargo/bin; при `bash run-chat-overlay-preview.sh` не подхватывается ~/.zprofile
if [[ -f "${HOME}/.cargo/env" ]]; then
    # shellcheck source=/dev/null
    . "${HOME}/.cargo/env"
else
    export PATH="${HOME}/.cargo/bin:${PATH:-}"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
AGENT_RESOURCES="$PROJECT_ROOT/platform-agent/src/main/resources"
CHAT_OVERLAY_DIR="$PROJECT_ROOT/platform-agent/chat-overlay"
# Отдельный target, чтобы не ждать блокировку общего target/ с rust-analyzer и другими сборками.
export CARGO_TARGET_DIR="${CHAT_OVERLAY_DIR}/src-tauri/target-chat-preview"
TAURI_BIN="$CARGO_TARGET_DIR/release/platform-chat-overlay"
SCREENSHOTS_DIR="$SCRIPT_DIR/screenshots"
PORT="${CHAT_PREVIEW_PORT:-3010}"
NODE_BASE="http://localhost:$PORT"

NODE_PID=""
TAURI_PID=""

CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
log() { echo -e "${CYAN}[chat-preview]${NC} $*"; }
ok()  { echo -e "${GREEN}[chat-preview]${NC} $*"; }

cleanup() {
    if [[ -n "${TAURI_PID:-}" ]]; then
        kill "$TAURI_PID" 2>/dev/null || true
        wait "$TAURI_PID" 2>/dev/null || true
    fi
    if [[ -n "${NODE_PID:-}" ]]; then
        curl -sf -X POST "$NODE_BASE/close" >/dev/null 2>&1 || true
        kill "$NODE_PID" 2>/dev/null || true
        wait "$NODE_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

command -v node >/dev/null 2>&1 || { echo "Нужен node в PATH"; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "Нужен curl"; exit 1; }
command -v cargo >/dev/null 2>&1 || {
    echo -e "${RED}Нужен Rust (cargo). Установка: https://rustup.rs/${NC}"
    exit 1
}

mkdir -p "$SCREENSHOTS_DIR"

if [[ ! -d "$AGENT_RESOURCES/node_modules" ]]; then
    log "Устанавливаю npm-зависимости в platform-agent/src/main/resources …"
    (cd "$AGENT_RESOURCES" && npm install)
fi

if [[ ! -x "$TAURI_BIN" ]]; then
    log "Сборка chat-overlay (Tauri, release). Первый запуск может занять несколько минут …"
    log "Не используйте Ctrl+Z (приостановка). Если долго «file lock» — закройте другую сборку cargo или rust-analyzer."
    (cd "$CHAT_OVERLAY_DIR/src-tauri" && cargo build --release)
fi

if [[ ! -x "$TAURI_BIN" ]]; then
    echo "Не найден бинарник после сборки: $TAURI_BIN"
    exit 1
fi

log "Запуск Playwright-сервера на порту $PORT …"
export SCREENSHOTS_DIR
export PORT
unset HEADLESS 2>/dev/null || true
(cd "$AGENT_RESOURCES" && node playwright-server.js) &
NODE_PID=$!

log "Жду /health …"
for _ in $(seq 1 50); do
    if curl -sf "$NODE_BASE/health" >/dev/null; then
        break
    fi
    sleep 0.2
done
if ! curl -sf "$NODE_BASE/health" >/dev/null; then
    echo "Сервер не ответил на $NODE_BASE/health"
    exit 1
fi

log "Инициализация браузера (окно Chromium) …"
curl -sf -X POST "$NODE_BASE/initialize" \
    -H 'Content-Type: application/json' \
    -d '{"headless":false,"baseUrl":"http://localhost:8080"}' >/dev/null

log "Открываю тестовую страницу в Chromium …"
curl -sf -X POST "$NODE_BASE/execute" \
    -H 'Content-Type: application/json' \
    -d '{"type":"OPEN_PAGE","target":"https://example.com","explanation":"chat overlay preview","parameters":{}}' >/dev/null

log "Запуск окна чата (Tauri) …"
"$TAURI_BIN" &
TAURI_PID=$!

ok "Готово: Chromium — страница и курсор; отдельное окно — панель из Tauri (конфиг: src/main/resources/chat-overlay-app.config.json)."
echo -e "${YELLOW}Нажмите Enter, чтобы закрыть Tauri, Chromium и остановить сервер.${NC}"
read -r _
