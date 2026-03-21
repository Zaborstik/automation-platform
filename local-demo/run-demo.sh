#!/usr/bin/env bash
# =============================================================================
#  run-demo.sh — запуск automation-platform demo одной командой
#
#  Что делает:
#    1. Проверяет prerequisites (java, node, npm, curl)
#    2. Собирает проект Maven
#    3. Устанавливает npm-зависимости Playwright-сервера
#    4. Запускает Spring Boot API (H2, dev-профиль — Docker не нужен)
#    5. Запускает Playwright-сервер (node)
#    6. Ждёт готовности обоих сервисов
#    7. Создаёт demo-план и исполняет его через REST API
#    8. Показывает результат и путь к скриншотам
#    9. Ждёт Enter → завершает все фоновые процессы
#
#  Сценарий: открыть DuckDuckGo → ввести поисковый запрос → нажать поиск →
#            дождаться результатов → авто-скриншот каждого шага
#
#  API: при POST /api/plans начальный ЖЦ плана и шагов берётся из БД (system.workflow.firststep),
#  а не из JSON. Тип UI-операции задаётся только через actions[].actionId → system.action.internalname.
#
#  Запуск: ./local-demo/run-demo.sh
#  В IntelliJ: Edit Configurations → + → Shell Script → выбрать этот файл
# =============================================================================

set -euo pipefail

# --------------------------------------------------------------------------- #
#  Цвета и утилиты вывода
# --------------------------------------------------------------------------- #
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

log()   { echo -e "${CYAN}[DEMO]${NC} $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}   $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
fail()  { echo -e "${RED}[FAIL]${NC} $*" >&2; }
header(){ echo -e "\n${BOLD}${CYAN}══════════════════════════════════════════${NC}"; echo -e "${BOLD}${CYAN}  $*${NC}"; echo -e "${BOLD}${CYAN}══════════════════════════════════════════${NC}\n"; }

# --------------------------------------------------------------------------- #
#  Пути
# --------------------------------------------------------------------------- #
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
AGENT_RESOURCES="$PROJECT_ROOT/platform-agent/src/main/resources"
SCREENSHOTS_DIR="$SCRIPT_DIR/screenshots"
API_LOG="$SCRIPT_DIR/api.log"
NODE_LOG="$SCRIPT_DIR/playwright.log"

API_PORT=8080
NODE_PORT=3000
API_BASE="http://localhost:$API_PORT"
NODE_BASE="http://localhost:$NODE_PORT"

API_PID=""
NODE_PID=""

mkdir -p "$SCREENSHOTS_DIR"

# --------------------------------------------------------------------------- #
#  Cleanup при любом выходе
# --------------------------------------------------------------------------- #
cleanup() {
    echo ""
    log "Завершение фоновых процессов..."
    [ -n "$API_PID" ]  && kill "$API_PID"  2>/dev/null && ok "Spring Boot API остановлен (PID $API_PID)"
    [ -n "$NODE_PID" ] && kill "$NODE_PID" 2>/dev/null && ok "Playwright Server остановлен (PID $NODE_PID)"
    log "Логи сохранены: $API_LOG, $NODE_LOG"
    log "Скриншоты: $SCREENSHOTS_DIR"
}
trap cleanup EXIT INT TERM

# --------------------------------------------------------------------------- #
#  1. Проверка prerequisites
# --------------------------------------------------------------------------- #
header "Проверка окружения"

check_cmd() {
    local cmd=$1; local hint=${2:-""}
    if ! command -v "$cmd" &>/dev/null; then
        fail "Не найдена команда: $cmd${hint:+ — $hint}"
        exit 1
    fi
    ok "$cmd — $(command -v "$cmd")"
}

check_cmd java  "Установите JDK 21+"
check_cmd node  "Установите Node.js 18+"
check_cmd npm   "Входит в Node.js"
check_cmd curl  "Обычно уже есть на macOS"

JAVA_VER=$(java -version 2>&1 | head -1)
NODE_VER=$(node --version)
ok "Java: $JAVA_VER"
ok "Node: $NODE_VER"

# --------------------------------------------------------------------------- #
#  Поиск Maven
# --------------------------------------------------------------------------- #
find_mvn() {
    # 1. mvn в PATH
    if command -v mvn &>/dev/null; then echo "mvn"; return; fi
    # 2. IntelliJ IDEA bundled Maven (macOS)
    local ij_mvn="/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn"
    if [ -x "$ij_mvn" ]; then echo "$ij_mvn"; return; fi
    # 3. IntelliJ IDEA CE
    local ij_ce_mvn="/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn"
    if [ -x "$ij_ce_mvn" ]; then echo "$ij_ce_mvn"; return; fi
    # 4. Homebrew
    if [ -x "/opt/homebrew/bin/mvn" ]; then echo "/opt/homebrew/bin/mvn"; return; fi
    if [ -x "/usr/local/bin/mvn" ]; then echo "/usr/local/bin/mvn"; return; fi
    fail "Maven не найден. Добавьте mvn в PATH или установите через 'brew install maven'."
    exit 1
}
MVN=$(find_mvn)
ok "Maven: $MVN"

# --------------------------------------------------------------------------- #
#  2. Сборка проекта
# --------------------------------------------------------------------------- #
header "Сборка проекта"
log "Запуск: $MVN package -DskipTests -pl platform-core,platform-agent,platform-executor,platform-api --also-make -q"
cd "$PROJECT_ROOT"
"$MVN" package -DskipTests \
    -pl platform-core,platform-agent,platform-executor,platform-api \
    --also-make -q
ok "Сборка завершена"

# Найти jar Spring Boot
API_JAR=$(find "$PROJECT_ROOT/platform-api/target" -name "platform-api-*.jar" ! -name "*-sources.jar" | head -1)
if [ -z "$API_JAR" ]; then
    fail "platform-api JAR не найден в platform-api/target/"
    exit 1
fi
ok "API JAR: $API_JAR"

# --------------------------------------------------------------------------- #
#  3. npm install для Playwright-сервера
# --------------------------------------------------------------------------- #
header "Установка npm-зависимостей"
cd "$AGENT_RESOURCES"
if [ ! -d "node_modules" ]; then
    log "npm install..."
    npm install --silent
    ok "Зависимости установлены"
else
    ok "node_modules уже есть, пропускаем npm install"
fi

# Убедимся что браузеры Playwright скачаны
log "Установка/обновление Chromium для Playwright (может занять ~1 мин при первом запуске)..."
npx playwright install chromium 2>&1 | tail -5
ok "Chromium готов"

# --------------------------------------------------------------------------- #
#  4. Запуск Playwright-сервера
# --------------------------------------------------------------------------- #
header "Запуск Playwright Server (port $NODE_PORT)"

# Убиваем остатки на порту, если есть
lsof -ti tcp:$NODE_PORT | xargs kill -9 2>/dev/null || true

SCREENSHOTS_DIR="$SCREENSHOTS_DIR" \
HEADLESS="false" \
PORT=$NODE_PORT \
    node "$AGENT_RESOURCES/playwright-server.js" > "$NODE_LOG" 2>&1 &
NODE_PID=$!
ok "Playwright Server запущен (PID $NODE_PID)"

# --------------------------------------------------------------------------- #
#  5. Запуск Spring Boot API
# --------------------------------------------------------------------------- #
header "Запуск Spring Boot API (port $API_PORT)"

# Всегда запускаем API из корня проекта, чтобы H2 создавал data/platformdb в одном месте
cd "$PROJECT_ROOT"
# Демо: чистая БД при каждом запуске (избегаем Flyway "failed migration" после смены миграций)
for f in data/platformdb.mv.db data/platformdb.trace.db; do
    [ -f "$f" ] && rm -f "$f" && log "Удалена старая БД: $f"
done

# Убиваем остатки на порту, если есть
lsof -ti tcp:$API_PORT | xargs kill -9 2>/dev/null || true

SPRING_PROFILES_ACTIVE=dev \
PLATFORM_AGENT_SERVER_URL="$NODE_BASE" \
PLATFORM_AGENT_BASE_URL="about:blank" \
PLATFORM_AGENT_HEADLESS="false" \
    java -jar "$API_JAR" \
        --server.port=$API_PORT \
        --logging.level.root=WARN \
        --logging.level.com.zaborstik=INFO \
    > "$API_LOG" 2>&1 &
API_PID=$!
ok "Spring Boot API запущен (PID $API_PID)"

# --------------------------------------------------------------------------- #
#  6. Ожидание готовности
# --------------------------------------------------------------------------- #
header "Ожидание готовности сервисов"

wait_for_http() {
    local name=$1; local url=$2; local max_wait=${3:-60}; local log_file=${4:-}
    log "Ожидаем $name ($url)..."
    local i=0
    while ! curl -s -o /dev/null -w '%{http_code}' "$url" 2>/dev/null | grep -qE '^[2-4][0-9]{2}$'; do
        sleep 1
        i=$((i+1))
        if [ $i -ge $max_wait ]; then
            if [ -n "$log_file" ]; then
                fail "$name не стартовал за $max_wait сек. Логи: $log_file"
            else
                fail "$name не стартовал за $max_wait сек."
            fi
            exit 1
        fi
        [ $((i % 5)) -eq 0 ] && log "  ещё ждём $name... ($i сек)"
    done
    ok "$name готов ($i сек)"
}

wait_for_http "Playwright Server" "$NODE_BASE/health"      30 "$NODE_LOG"
wait_for_http "Spring Boot API"   "$API_BASE/api/plans/x" 20 "$API_LOG"

# --------------------------------------------------------------------------- #
#  7. Создание demo-плана
# --------------------------------------------------------------------------- #
header "Создание demo-плана: поиск на DuckDuckGo"

DEMO_QUERY="Spring Boot Playwright browser automation demo"
log "Поисковый запрос: \"$DEMO_QUERY\""

PLAN_JSON=$(cat <<EOF
{
  "workflowId": "wf-plan",
  "target": "Найти информацию в интернете",
  "explanation": "Demo: открыть DuckDuckGo и найти '${DEMO_QUERY}'",
  "steps": [
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "https://duckduckgo.com",
      "sortOrder": 0,
      "displayName": "Открыть DuckDuckGo",
      "actions": [
        { "actionId": "act-open-page", "metaValue": "https://duckduckgo.com" }
      ]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-input",
      "entityId": "input[name='q']",
      "sortOrder": 1,
      "displayName": "Ввести запрос и нажать Enter",
      "actions": [
        { "actionId": "act-input-text", "metaValue": "${DEMO_QUERY}\\n" }
      ]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "article[data-testid='result']",
      "sortOrder": 2,
      "displayName": "Дождаться результатов поиска",
      "actions": [
        { "actionId": "act-wait-element" }
      ]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-link",
      "entityId": "article[data-testid='result'] h2 a",
      "sortOrder": 3,
      "displayName": "Открыть первый результат",
      "actions": [
        { "actionId": "act-click" }
      ]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "domcontentloaded",
      "sortOrder": 4,
      "displayName": "Дождаться загрузки страницы",
      "actions": [
        { "actionId": "act-wait-element" }
      ]
    }
  ]
}
EOF
)

log "POST $API_BASE/api/plans"
CREATE_RESPONSE=$(curl -s -X POST "$API_BASE/api/plans" \
    -H "Content-Type: application/json" \
    -d "$PLAN_JSON" || true)

PLAN_ID=$(echo "$CREATE_RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | sed 's/"id":"//;s/"//')

if [ -z "$PLAN_ID" ]; then
    fail "Не удалось создать план. Ответ API:"
    echo "$CREATE_RESPONSE"
    exit 1
fi
ok "План создан: ID = $PLAN_ID"

# --------------------------------------------------------------------------- #
#  8. Запуск выполнения плана
# --------------------------------------------------------------------------- #
header "Запуск выполнения"

log "POST $API_BASE/api/plans/$PLAN_ID/execute"
log "Браузер откроется через несколько секунд..."

EXEC_RESPONSE=$(curl -s -X POST "$API_BASE/api/plans/$PLAN_ID/execute" \
    --max-time 120 || true)

echo ""
echo -e "${BOLD}══ Результат выполнения ══${NC}"
echo "$EXEC_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$EXEC_RESPONSE"

# Проверяем успех
IS_SUCCESS=$(echo "$EXEC_RESPONSE" | grep -o '"success":[a-z]*' | head -1 | grep -c 'true' || true)
PLAN_RESULT_ID=$(echo "$EXEC_RESPONSE" | grep -o '"planResultId":"[^"]*"' | head -1 | sed 's/"planResultId":"//;s/"//')

echo ""
if [ "$IS_SUCCESS" -gt 0 ]; then
    ok "Сценарий выполнен УСПЕШНО (planResultId: $PLAN_RESULT_ID)"
else
    warn "Сценарий завершился с ошибками (planResultId: $PLAN_RESULT_ID)"
    warn "Проверьте логи: $API_LOG"
fi

# --------------------------------------------------------------------------- #
#  9. Скриншоты (только при ошибках)
# --------------------------------------------------------------------------- #
header "Скриншоты ошибок"

PLAYWRIGHT_SCREENSHOTS_DIR="$AGENT_RESOURCES/screenshots"
DEMO_SCREENSHOTS_DIR="$SCREENSHOTS_DIR"

# Копируем только error-скриншоты этого запуска
if [ -d "$PLAYWRIGHT_SCREENSHOTS_DIR" ]; then
    find "$PLAYWRIGHT_SCREENSHOTS_DIR" -name "error-*.png" -newer "$SCRIPT_DIR/run-demo.sh" \
        -exec cp {} "$DEMO_SCREENSHOTS_DIR/" \; 2>/dev/null || true
fi

ERROR_SCREENSHOT=$(find "$DEMO_SCREENSHOTS_DIR" -name "error-*.png" -newer "$SCRIPT_DIR/run-demo.sh" \
    2>/dev/null | sort | tail -1)
if [ -n "$ERROR_SCREENSHOT" ]; then
    warn "Скриншот ошибки: $ERROR_SCREENSHOT"
    open "$ERROR_SCREENSHOT" 2>/dev/null || true
else
    ok "Скриншотов ошибок нет — всё прошло успешно"
fi

# --------------------------------------------------------------------------- #
#  10. Финал
# --------------------------------------------------------------------------- #
header "Demo завершён"

echo -e "  ${BOLD}API работает на:${NC}  $API_BASE"
echo -e "  ${BOLD}H2 Console:${NC}       $API_BASE/h2-console"
echo -e "  ${BOLD}Playwright:${NC}       $NODE_BASE"
echo -e "  ${BOLD}Логи API:${NC}         $API_LOG"
echo -e "  ${BOLD}Логи Playwright:${NC}  $NODE_LOG"
echo -e "  ${BOLD}Скриншоты:${NC}        $DEMO_SCREENSHOTS_DIR"
echo ""
echo -e "${YELLOW}Нажмите Enter для завершения всех сервисов...${NC}"
read -r
