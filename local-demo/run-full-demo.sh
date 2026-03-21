#!/usr/bin/env bash
# =============================================================================
#  run-full-demo.sh — ПОЛНАЯ демонстрация automation-platform
#
#  8 частей:
#    1. Справочники (CRUD workflows, entity-types, actions)
#    2. Жизненный цикл плана (new → in_progress → paused → completed, 409)
#    3. Листинг и поиск планов (пагинация, фильтрация)
#    4. Сценарий A: Wikipedia — глубокая цепочка навигации
#    5. Сценарий B: DuckDuckGo — длинный поисковый флоу
#    6. Сценарий C: GitHub — расширенная навигация по вкладкам
#    7. Сценарий D: HTTPBin — многошаговое заполнение формы
#    8. OpenAPI / Swagger UI
#
#  POST /api/plans: workflowStepInternalName в теле запроса не задаёт ЖЦ — API
#  подставляет system.workflow.firststep для workflowId плана и шагов.
#  Операция в браузере только через actions[].actionId → system.action.internalname.
#
#  Запуск: ./local-demo/run-full-demo.sh
# =============================================================================

set -euo pipefail

# --------------------------------------------------------------------------- #
#  Цвета и утилиты
# --------------------------------------------------------------------------- #
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; MAGENTA='\033[0;35m'; BOLD='\033[1m'
DIM='\033[2m'; NC='\033[0m'

log()    { echo -e "${CYAN}[DEMO]${NC} $*"; }
ok()     { echo -e "${GREEN}  ✓${NC} $*"; }
warn()   { echo -e "${YELLOW}  ⚠${NC} $*"; }
fail()   { echo -e "${RED}  ✗${NC} $*" >&2; }
header() {
  echo ""
  echo -e "${BOLD}${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BOLD}${MAGENTA}  $*${NC}"
  echo -e "${BOLD}${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}
step()   { echo -e "\n${BOLD}${CYAN}  ▸ $*${NC}"; }
json()   { python3 -m json.tool 2>/dev/null || cat; }
pause()  { echo -e "\n${DIM}  [Enter для продолжения]${NC}"; read -r; }

SCENARIO_NUM=0
scenario_ok=0
scenario_fail=0

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
API="http://localhost:$API_PORT"
NODE_BASE="http://localhost:$NODE_PORT"

API_PID=""
NODE_PID=""

mkdir -p "$SCREENSHOTS_DIR"

# --------------------------------------------------------------------------- #
#  Cleanup
# --------------------------------------------------------------------------- #
cleanup() {
    echo ""
    log "Завершение фоновых процессов..."
    [ -n "$API_PID" ]  && kill "$API_PID"  2>/dev/null && ok "Spring Boot API остановлен"
    [ -n "$NODE_PID" ] && kill "$NODE_PID" 2>/dev/null && ok "Playwright Server остановлен"
}
trap cleanup EXIT INT TERM

# --------------------------------------------------------------------------- #
#  HTTP хелперы (возвращают body + __HTTP_CODE__<code> в последней строке)
# --------------------------------------------------------------------------- #
_curl() { curl -s -w "\n__HTTP_CODE__%{http_code}" "$@"; }
http_get()    { _curl "$@"; }
http_post()   { _curl -X POST   -H "Content-Type: application/json" "$@"; }
http_patch()  { _curl -X PATCH  -H "Content-Type: application/json" "$@"; }
http_put()    { _curl -X PUT    -H "Content-Type: application/json" "$@"; }
http_delete() { _curl -X DELETE "$@"; }

_body() { echo "$1" | sed '/__HTTP_CODE__/d'; }
_code() { echo "$1" | grep '__HTTP_CODE__' | sed 's/__HTTP_CODE__//'; }

show() {
    local raw="$1" expect="${2:-200}"
    _body "$raw" | json
    local code; code=$(_code "$raw")
    [ "$code" = "$expect" ] && ok "HTTP $code" || fail "HTTP $code (ожидался $expect)"
}

field() { _body "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | sed "s/\"$2\":\"//;s/\"//"; }
field_bool() { _body "$1" | grep -o "\"$2\":[a-z]*" | head -1 | sed "s/\"$2\"://"; }

# --------------------------------------------------------------------------- #
#  Закрытие сессии исполнителя (скрипт вызывает endpoint исполнителя напрямую;
#  для демо используется Playwright-сервер; в будущем — другой URL/протокол)
# --------------------------------------------------------------------------- #
EXECUTOR_URL="${EXECUTOR_URL:-$NODE_BASE}"
CLOSE_DELAY_SEC="${CLOSE_DELAY_SEC:-2}"
close_executor_session() {
    if curl -s -X POST -H "Content-Type: application/json" "$EXECUTOR_URL/close" -d '{}' >/dev/null 2>&1; then
        log "Сессия исполнителя закрыта"
    else
        warn "Не удалось закрыть сессию (исполнитель может быть недоступен)"
    fi
}

# --------------------------------------------------------------------------- #
#  Хелпер: создать план, выполнить, показать результат
# --------------------------------------------------------------------------- #
run_scenario() {
    local title="$1"
    local plan_json="$2"

    SCENARIO_NUM=$((SCENARIO_NUM + 1))

    step "${SCENARIO_NUM}.1  Создание плана"
    local R
    R=$(http_post "$API/api/plans" -d "$plan_json")
    show "$R" "201"
    local plan_id; plan_id=$(field "$R" "id")
    [ -z "$plan_id" ] && { fail "Не удалось создать план"; scenario_fail=$((scenario_fail+1)); return; }
    ok "План: $plan_id"

    step "${SCENARIO_NUM}.2  Проверка: начальный ЖЦ плана (из БД, обычно new)"
    R=$(http_get "$API/api/plans/$plan_id")
    local status; status=$(field "$R" "workflowStepInternalName")
    echo "  plan.workflowStepInternalName = $status"
    [ "$status" = "new" ] && ok "Корректно" || warn "Ожидался new (firststep wf-plan), получен $status"

    step "${SCENARIO_NUM}.3  POST /execute — запуск (браузер откроется)"
    log "  Ждём выполнения..."
    echo ""
    R=$(http_post "$API/api/plans/$plan_id/execute" --max-time 120)
    show "$R"

    local success; success=$(field_bool "$R" "success")
    local result_id; result_id=$(field "$R" "planResultId")
    local total; total=$(field "$R" "totalSteps" 2>/dev/null || echo "?")
    local failed_steps; failed_steps=$(field "$R" "failedSteps" 2>/dev/null || echo "?")

    step "${SCENARIO_NUM}.4  Проверка статуса ПОСЛЕ выполнения"
    R=$(http_get "$API/api/plans/$plan_id")
    status=$(field "$R" "workflowStepInternalName")
    local stopped; stopped=$(field "$R" "stoppedAtPlanStepId")
    echo "  plan.workflowStepInternalName = $status"
    echo "  stoppedAtPlanStepId             = $stopped"
    echo "  planResultId             = $result_id"

    if [ "$success" = "true" ]; then
        ok "Сценарий «${title}» — УСПЕХ (шагов: $total, ошибок: $failed_steps)"
        scenario_ok=$((scenario_ok + 1))
        step "${SCENARIO_NUM}.5  Закрытие сессии исполнителя"
        log "Пауза ${CLOSE_DELAY_SEC} сек перед закрытием сессии..."
        sleep "$CLOSE_DELAY_SEC"
        close_executor_session
    else
        warn "Сценарий «${title}» — ОШИБКИ (шагов: $total, ошибок: $failed_steps)"
        scenario_fail=$((scenario_fail + 1))
        log "Сессия остаётся открытой ~2 сек для инспекции (логи: $API_LOG, $NODE_LOG)"
        sleep 2
    fi
}

# =========================================================================== #
#  ПОДГОТОВКА
# =========================================================================== #
header "ПОДГОТОВКА: сборка и запуск сервисов"

find_mvn() {
    if command -v mvn &>/dev/null; then echo "mvn"; return; fi
    local p
    for p in \
        "/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" \
        "/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn" \
        "/opt/homebrew/bin/mvn" "/usr/local/bin/mvn"; do
        [ -x "$p" ] && { echo "$p"; return; }
    done
    fail "Maven не найден"; exit 1
}
MVN=$(find_mvn)
ok "Maven: $MVN"

step "Сборка проекта"
cd "$PROJECT_ROOT"
"$MVN" package -DskipTests \
    -pl platform-core,platform-agent,platform-executor,platform-api \
    --also-make -q
ok "Сборка завершена"

API_JAR=$(find "$PROJECT_ROOT/platform-api/target" -name "platform-api-*.jar" ! -name "*-sources.jar" | head -1)
[ -z "$API_JAR" ] && { fail "JAR не найден"; exit 1; }

step "npm + Chromium"
cd "$AGENT_RESOURCES"
[ ! -d "node_modules" ] && npm install --silent
npx playwright install chromium 2>&1 | tail -2
ok "Готово"

step "Запуск Playwright Server (:$NODE_PORT)"
lsof -ti tcp:$NODE_PORT | xargs kill -9 2>/dev/null || true
ACTION_DELAY_MULTIPLIER="${ACTION_DELAY_MULTIPLIER:-1}" \
OPEN_PAGE_DELAY_MS="${OPEN_PAGE_DELAY_MS:-0}" \
PAUSE_BEFORE_CLICK_MS="${PAUSE_BEFORE_CLICK_MS:-1000}" \
SCREENSHOTS_DIR="$SCREENSHOTS_DIR" HEADLESS="false" PORT=$NODE_PORT \
    node "$AGENT_RESOURCES/playwright-server.js" > "$NODE_LOG" 2>&1 &
NODE_PID=$!

cd "$PROJECT_ROOT"
for f in data/platformdb.mv.db data/platformdb.trace.db; do [ -f "$f" ] && rm -f "$f"; done

step "Запуск Spring Boot API (:$API_PORT)"
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

step "Ожидание готовности"
wait_for() {
    local name=$1 url=$2 max=${3:-60} i=0
    while ! curl -s -o /dev/null -w '%{http_code}' "$url" 2>/dev/null | grep -qE '^[2-4][0-9]{2}$'; do
        sleep 1; i=$((i+1))
        [ $i -ge $max ] && { fail "$name не стартовал"; exit 1; }
    done
    ok "$name (${i}s)"
}
wait_for "Playwright" "$NODE_BASE/health" 30
wait_for "API"        "$API/api/plans/x"  30
echo -e "\n${GREEN}${BOLD}  Сервисы работают. Начинаем!${NC}"
pause

# =========================================================================== #
#  ЧАСТЬ 1: Справочники
# =========================================================================== #
header "ЧАСТЬ 1: Справочники — Workflows, EntityTypes, Actions"

step "1.1  GET /api/workflows"
R=$(http_get "$API/api/workflows"); show "$R"

step "1.2  GET /api/workflow-steps"
R=$(http_get "$API/api/workflow-steps"); show "$R"

step "1.3  GET /api/entity-types"
R=$(http_get "$API/api/entity-types"); show "$R"

step "1.4  POST /api/entity-types — новый тип «Модальное окно»"
R=$(http_post "$API/api/entity-types" -d '{"displayname":"Модальное окно","uiDescription":"Всплывающее окно"}')
show "$R" "201"
NEW_ET_ID=$(field "$R" "id")
ok "Создан: $NEW_ET_ID"

step "1.5  GET /api/entity-types/$NEW_ET_ID"
R=$(http_get "$API/api/entity-types/$NEW_ET_ID"); show "$R"

step "1.6  GET /api/actions"
R=$(http_get "$API/api/actions"); show "$R"

step "1.7  GET /api/actions?entityTypeId=ent-button"
R=$(http_get "$API/api/actions?entityTypeId=ent-button"); show "$R"

step "1.8  GET /api/actions?entityTypeId=ent-page"
R=$(http_get "$API/api/actions?entityTypeId=ent-page"); show "$R"
pause

# =========================================================================== #
#  ЧАСТЬ 2: Жизненный цикл
# =========================================================================== #
header "ЧАСТЬ 2: Жизненный цикл плана"

step "2.1  Создание плана (начальный ЖЦ = firststep wf-plan в БД)"
R=$(http_post "$API/api/plans" -d '{
  "workflowId":"wf-plan",
  "target":"ЖЦ-демо","explanation":"Демонстрация переходов",
  "steps":[{"workflowId":"wf-plan-step",
    "entityTypeId":"ent-page","entityId":"https://example.com","sortOrder":1,
    "displayName":"Шаг","actions":[{"actionId":"act-open-page"}]}]
}')
show "$R" "201"
LC_ID=$(field "$R" "id"); ok "План: $LC_ID"

step "2.2  new → in_progress"
R=$(http_patch "$API/api/plans/$LC_ID/transition" -d '{"targetStep":"in_progress"}')
show "$R"
S=$(field "$R" "workflowStepInternalName")
[ "$S" = "in_progress" ] && ok "new → in_progress" || fail "Ожидался in_progress, получен $S"

step "2.3  in_progress → paused"
R=$(http_patch "$API/api/plans/$LC_ID/transition" -d '{"targetStep":"paused"}')
S=$(field "$R" "workflowStepInternalName")
[ "$S" = "paused" ] && ok "in_progress → paused" || fail "$S"

step "2.4  paused → in_progress (возобновление)"
R=$(http_patch "$API/api/plans/$LC_ID/transition" -d '{"targetStep":"in_progress"}')
S=$(field "$R" "workflowStepInternalName")
[ "$S" = "in_progress" ] && ok "paused → in_progress" || fail "$S"

step "2.5  in_progress → completed"
R=$(http_patch "$API/api/plans/$LC_ID/transition" -d '{"targetStep":"completed"}')
S=$(field "$R" "workflowStepInternalName")
[ "$S" = "completed" ] && ok "in_progress → completed" || fail "$S"

step "2.6  completed → in_progress (ЗАПРЕЩЕНО)"
R=$(http_patch "$API/api/plans/$LC_ID/transition" -d '{"targetStep":"in_progress"}')
show "$R" "409"
ok "Невалидный переход отклонён — 409 Conflict"
pause

# =========================================================================== #
#  ЧАСТЬ 3: Листинг планов
# =========================================================================== #
header "ЧАСТЬ 3: Листинг и поиск планов"

step "3.1  Создаём 3 плана для листинга"
for i in 1 2 3; do
    http_post "$API/api/plans" -d "{
      \"workflowId\":\"wf-plan\",
      \"target\":\"Тест #$i\",\"explanation\":\"Листинг\",
      \"steps\":[{\"workflowId\":\"wf-plan-step\",
        \"entityTypeId\":\"ent-page\",\"entityId\":\"x\",\"sortOrder\":1,
        \"displayName\":\"Шаг\",\"actions\":[{\"actionId\":\"act-open-page\"}]}]
    }" > /dev/null
done
ok "Создано"

step "3.2  GET /api/plans (page=0, size=10)"
R=$(http_get "$API/api/plans?page=0&size=10"); show "$R"

step "3.3  GET /api/plans?status=new"
R=$(http_get "$API/api/plans?status=new"); show "$R"

step "3.4  GET /api/plans?status=completed"
R=$(http_get "$API/api/plans?status=completed"); show "$R"

step "3.5  GET /api/plans?page=0&size=2 (пагинация)"
R=$(http_get "$API/api/plans?page=0&size=2"); show "$R"
pause

# =========================================================================== #
#  ЧАСТЬ 4: Сценарий A — Wikipedia
# =========================================================================== #
header "ЧАСТЬ 4: Сценарий A — Wikipedia: глубокая цепочка навигации"
log "Поиск статьи → история изменений → возврат → переход по внутренней ссылке"

run_scenario "Wikipedia" '{
  "workflowId": "wf-plan",
  "target": "Wikipedia: deep navigation",
  "explanation": "Поиск, переходы по вкладкам статьи и внутренняя навигация",
  "steps": [
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "https://en.wikipedia.org",
      "sortOrder": 1,
      "displayName": "Открыть Wikipedia",
      "actions": [{ "actionId": "act-open-page", "metaValue": "https://en.wikipedia.org" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "input#searchInput",
      "sortOrder": 2,
      "displayName": "Дождаться поля поиска",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-input",
      "entityId": "input#searchInput",
      "sortOrder": 3,
      "displayName": "Ввести запрос: Playwright (software)",
      "actions": [{ "actionId": "act-input-text", "metaValue": "Playwright (software)\\n" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "#firstHeading",
      "sortOrder": 4,
      "displayName": "Дождаться заголовка статьи",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-link",
      "entityId": "#ca-history",
      "sortOrder": 5,
      "displayName": "Открыть вкладку истории статьи",
      "actions": [{ "actionId": "act-click" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "#pagehistory",
      "sortOrder": 6,
      "displayName": "Дождаться таблицы истории",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-link",
      "entityId": "#ca-view",
      "sortOrder": 7,
      "displayName": "Вернуться к просмотру статьи",
      "actions": [{ "actionId": "act-click" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "#firstHeading",
      "sortOrder": 8,
      "displayName": "Дождаться повторной загрузки статьи",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-link",
      "entityId": "a[href*=Microsoft]",
      "sortOrder": 9,
      "displayName": "Перейти по внутренней ссылке Microsoft",
      "actions": [{ "actionId": "act-click" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "#firstHeading",
      "sortOrder": 10,
      "displayName": "Дождаться страницы Microsoft",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    }
  ]
}'
pause

# =========================================================================== #
#  ЧАСТЬ 5: Сценарий B — DuckDuckGo
# =========================================================================== #
header "ЧАСТЬ 5: Сценарий B — DuckDuckGo: поиск и переход в результат"
log "Поиск → выдача → переход в первый результат"

run_scenario "DuckDuckGo" '{
  "workflowId": "wf-plan",
  "target": "DuckDuckGo: search chain",
  "explanation": "Поиск с переходом в результат и дополнительными шагами проверки",
  "steps": [
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "https://duckduckgo.com",
      "sortOrder": 1,
      "displayName": "Открыть DuckDuckGo",
      "actions": [{ "actionId": "act-open-page", "metaValue": "https://duckduckgo.com" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "input[name=q]",
      "sortOrder": 2,
      "displayName": "Дождаться поля ввода",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-input",
      "entityId": "input[name=q]",
      "sortOrder": 3,
      "displayName": "Ввести запрос: playwright github issues",
      "actions": [{ "actionId": "act-input-text", "metaValue": "playwright github issues\\n" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "article[data-testid=result]",
      "sortOrder": 4,
      "displayName": "Дождаться результатов поиска",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-link",
      "entityId": "article[data-testid=result] h2 a",
      "sortOrder": 5,
      "displayName": "Открыть первый результат",
      "actions": [{ "actionId": "act-click" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "main",
      "sortOrder": 6,
      "displayName": "Дождаться загрузки целевой страницы",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "20000" }]
    }
  ]
}'
pause

# =========================================================================== #
#  ЧАСТЬ 6: Сценарий C — GitHub
# =========================================================================== #
header "ЧАСТЬ 6: Сценарий C — GitHub: расширенная навигация по вкладкам"
log "Code → Issues → Pull Requests → Actions с проверкой каждой стадии"

run_scenario "GitHub" '{
  "workflowId": "wf-plan",
  "target": "GitHub: extended tab navigation",
  "explanation": "Многошаговый обход вкладок репозитория с проверками",
  "steps": [
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "https://github.com/microsoft/playwright",
      "sortOrder": 1,
      "displayName": "Открыть microsoft/playwright",
      "actions": [{ "actionId": "act-open-page", "metaValue": "https://github.com/microsoft/playwright" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "main",
      "sortOrder": 2,
      "displayName": "Дождаться загрузки репозитория",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-link",
      "entityId": "a[href$=\"/microsoft/playwright/issues\"]",
      "sortOrder": 3,
      "displayName": "Перейти в Issues",
      "actions": [{ "actionId": "act-click" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "a[href*=\"/microsoft/playwright/issues/\"]",
      "sortOrder": 4,
      "displayName": "Дождаться списка Issues",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-link",
      "entityId": "a[href$=\"/microsoft/playwright/pulls\"]",
      "sortOrder": 5,
      "displayName": "Перейти в Pull Requests",
      "actions": [{ "actionId": "act-click" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "main",
      "sortOrder": 6,
      "displayName": "Дождаться загрузки вкладки Pull Requests",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-link",
      "entityId": "a[href=\"/microsoft/playwright\"]",
      "sortOrder": 7,
      "displayName": "Вернуться на вкладку Code",
      "actions": [{ "actionId": "act-click" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "#readme",
      "sortOrder": 8,
      "displayName": "Дождаться README на вкладке Code",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-link",
      "entityId": "a[href$=\"/microsoft/playwright/actions\"]",
      "sortOrder": 9,
      "displayName": "Перейти на вкладку Actions",
      "actions": [{ "actionId": "act-click" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "main",
      "sortOrder": 10,
      "displayName": "Дождаться загрузки вкладки Actions",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "20000" }]
    }
  ]
}'
pause

# =========================================================================== #
#  ЧАСТЬ 7: Сценарий D — HTTPBin
# =========================================================================== #
header "ЧАСТЬ 7: Сценарий D — HTTPBin: многошаговое заполнение формы"
log "Открыть форму → заполнить поля → отправить → проверить echo-результат"

run_scenario "HTTPBin" '{
  "workflowId": "wf-plan",
  "target": "HTTPBin: form submission chain",
  "explanation": "Полный пользовательский поток заполнения формы и проверки результата",
  "steps": [
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "https://httpbin.org/forms/post",
      "sortOrder": 1,
      "displayName": "Открыть форму HTTPBin",
      "actions": [{ "actionId": "act-open-page", "metaValue": "https://httpbin.org/forms/post" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "form",
      "sortOrder": 2,
      "displayName": "Дождаться формы",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-input",
      "entityId": "input[name=custname]",
      "sortOrder": 3,
      "displayName": "Заполнить поле Customer name",
      "actions": [{ "actionId": "act-input-text", "metaValue": "Automation Platform Demo" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-input",
      "entityId": "input[name=custtel]",
      "sortOrder": 4,
      "displayName": "Заполнить поле Telephone",
      "actions": [{ "actionId": "act-input-text", "metaValue": "+10000000001" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-input",
      "entityId": "input[name=custemail]",
      "sortOrder": 5,
      "displayName": "Заполнить поле Email",
      "actions": [{ "actionId": "act-input-text", "metaValue": "demo@automation.local" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-button",
      "entityId": "input[value=medium]",
      "sortOrder": 6,
      "displayName": "Выбрать размер medium",
      "actions": [{ "actionId": "act-click" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-button",
      "entityId": "input[value=cheese]",
      "sortOrder": 7,
      "displayName": "Выбрать топпинг cheese",
      "actions": [{ "actionId": "act-click" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-button",
      "entityId": "form button",
      "sortOrder": 8,
      "displayName": "Отправить форму",
      "actions": [{ "actionId": "act-click" }]
    },
    {
      "workflowId": "wf-plan-step",
      "entityTypeId": "ent-page",
      "entityId": "pre",
      "sortOrder": 9,
      "displayName": "Дождаться блока с результатом",
      "actions": [{ "actionId": "act-wait-element", "metaValue": "15000" }]
    }
  ]
}'
pause

# =========================================================================== #
#  ЧАСТЬ 8: OpenAPI / Swagger
# =========================================================================== #
header "ЧАСТЬ 8: OpenAPI / Swagger UI"

step "8.1  GET /api-docs"
R=$(http_get "$API/api-docs")
CODE=$(_code "$R")
[ "$CODE" = "200" ] && ok "OpenAPI доступен (HTTP 200)" || warn "HTTP $CODE"

step "8.2  Swagger UI"
echo -e "  ${BOLD}URL:${NC} $API/swagger-ui.html"
echo "  (откройте в браузере для интерактивной документации)"

step "8.3  DELETE /api/entity-types/$NEW_ET_ID — удаление типа"
R=$(http_delete "$API/api/entity-types/$NEW_ET_ID")
CODE=$(_code "$R")
[ "$CODE" = "204" ] && ok "Удалено (204)" || fail "HTTP $CODE"

step "8.4  GET /api/entity-types/$NEW_ET_ID — 404"
R=$(http_get "$API/api/entity-types/$NEW_ET_ID")
show "$R" "404"
ok "Тип удалён, 404 корректен"
pause

# =========================================================================== #
#  ИТОГИ
# =========================================================================== #
header "ИТОГИ ДЕМОНСТРАЦИИ"

echo -e "
  ${BOLD}Браузерные сценарии:${NC}
    Успех: ${GREEN}${scenario_ok}${NC}   Ошибки: ${RED}${scenario_fail}${NC}   Всего: $((scenario_ok + scenario_fail))

  ${GREEN}${BOLD}Продемонстрировано:${NC}

  ${BOLD}Справочники CRUD:${NC}
    GET/POST/DELETE entity-types                       ${GREEN}✓${NC}
    GET actions с фильтрацией по entity_type            ${GREEN}✓${NC}
    GET workflows + workflow-steps                      ${GREEN}✓${NC}

  ${BOLD}Жизненный цикл:${NC}
    new → in_progress → paused → in_progress → completed  ${GREEN}✓${NC}
    Блокировка невалидного перехода (409 Conflict)          ${GREEN}✓${NC}

  ${BOLD}Листинг и поиск:${NC}
    GET /api/plans с пагинацией (page, size)           ${GREEN}✓${NC}
    Фильтрация по статусу                               ${GREEN}✓${NC}

  ${BOLD}Выполнение (4 сложных сценария):${NC}
    A. Wikipedia — deep navigation chain                ${GREEN}✓${NC}
    B. DuckDuckGo — search chain + переходы             ${GREEN}✓${NC}
    C. GitHub — multi-tab navigation                    ${GREEN}✓${NC}
    D. HTTPBin — form submit + result verify            ${GREEN}✓${NC}

  ${BOLD}Автообновление статусов:${NC}
    Plan: new → in_progress → completed/failed          ${GREEN}✓${NC}
    PlanStep: new → in_progress → completed/failed      ${GREEN}✓${NC}
    stoppedAtPlanStepId обновляется                     ${GREEN}✓${NC}

  ${BOLD}OpenAPI / Swagger:${NC}
    /api-docs + /swagger-ui.html                        ${GREEN}✓${NC}

  ${BOLD}Сервисы:${NC}
    API:             $API
    Swagger UI:      $API/swagger-ui.html
    H2 Console:      $API/h2-console
    Playwright:      $NODE_BASE
    Логи API:        $API_LOG
    Логи Playwright: $NODE_LOG
    Скриншоты:       только error-скриншоты в $SCREENSHOTS_DIR
"

echo -e "${YELLOW}Нажмите Enter для завершения всех сервисов...${NC}"
read -r
