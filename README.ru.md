# automation-platform

Микросервисная платформа для генерации и выполнения планов автоматизации UI. Тяжёлая логика (БД, генерация плана) живёт на сервере, исполнение (Playwright, браузер) — на машине пользователя.

---

## Архитектура

```
┌──────────────────────────── LOCAL (машина пользователя) ────────────────────────────┐
│                                                                                     │
│   ┌────────────────┐     HTTP      ┌───────────────────┐    HTTP      ┌──────────┐  │
│   │  chat-overlay  │ ────────────▶ │ platform-executor │ ───────────▶ │  agent   │  │
│   │     (Tauri)    │   :7070       │      :7070        │  :7071       │  :7071   │  │
│   └────────────────┘               └────────┬──────────┘              └────┬─────┘  │
│                                             │                              │ HTTP   │
│                                             │ HTTP REST                    ▼        │
│                                             │                       ┌──────────────┐│
│                                             │                       │ playwright-  ││
│                                             │                       │ server :3000 ││
│                                             │                       └──────────────┘│
└─────────────────────────────────────────────┼───────────────────────────────────────┘
                                              │
                                              ▼  HTTPS
┌──────────────────────────────── SERVER (удалённый хост) ─────────────────────────────┐
│                                                                                     │
│   ┌────────────────┐     HTTP    ┌────────────────────┐                              │
│   │  platform-api  │ ──────────▶ │ platform-knowledge │                              │
│   │     :8080      │   :8081     │       :8081        │                              │
│   └──────┬─────────┘             └────────────────────┘                              │
│          │ JPA                                                                       │
│          ▼                                                                           │
│   ┌────────────────┐                                                                 │
│   │   PostgreSQL   │                                                                 │
│   └────────────────┘                                                                 │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

| Модуль                | Где живёт | Что делает                                                                 |
|-----------------------|-----------|----------------------------------------------------------------------------|
| `platform-core`       | jar lib   | Общие DTO/контракты (`Plan`, `PlanStep`, `Action`, `Resolver`, …).         |
| `platform-knowledge`  | server    | Spring Boot. `POST /api/knowledge/generate-plan` → план (stub).            |
| `platform-api`        | server    | Spring Boot. CRUD планов, справочники, lifecycle прогонов, бридж в knowledge. |
| `platform-executor`   | local     | Spring Boot. Тянет план с сервера, оркеструет шаги, репортит прогресс.     |
| `platform-agent`      | local     | Spring Boot. Тонкий REST поверх Node Playwright сайдкара.                   |
| `chat-overlay`        | local     | Tauri-приложение, UI пользователя поверх executor'а.                       |

Связь только по HTTP. `platform-api` не зависит ни от executor'а, ни от агента.

---

## Быстрый старт

### 1. Сервер

```bash
git clone https://github.com/your-org/automation-platform.git
cd automation-platform
cp docker/server/.env.example docker/server/.env
$EDITOR docker/server/.env            # POSTGRES_PASSWORD и т.п.
make server-deploy                    # postgres + api + knowledge
curl http://localhost:8080/actuator/health
```

Подробно: [`docs/DEPLOY_SERVER.ru.md`](docs/DEPLOY_SERVER.ru.md).

### 2. Локальная машина

```bash
git clone https://github.com/your-org/automation-platform.git
cd automation-platform
cp docker/local/.env.example docker/local/.env
$EDITOR docker/local/.env             # PLATFORM_API_URL=https://api.your-domain.com
make local-up                         # executor + agent + playwright
curl http://localhost:7070/actuator/health
```

Подробно: [`docs/DEPLOY_LOCAL.ru.md`](docs/DEPLOY_LOCAL.ru.md).

### 3. Тестовый прогон

```bash
curl -X POST http://localhost:7070/local/run \
     -H 'Content-Type: application/json' \
     -d '{"userInput":"открой страницу https://example.com"}'
```

---

## Make targets

```bash
make help              # список целей
make build             # ./mvnw -DskipTests install всего реактора
make test              # запустить тесты
make images            # собрать все docker-образы
make push REGISTRY=ghcr.io/your-org    # запушить server-образы

make server-deploy     # запустить серверный стек (build + up)
make server-pull       # пулл pre-built образов с registry и запуск
make server-down       # stop серверного стека
make server-logs       # tail -f

make local-up          # запустить локальный стек
make local-down        # stop локального
make local-logs        # tail -f
```

---

## Структура репозитория

```
automation-platform/
├── platform-core/          # общие DTO/Resolver (jar)
├── platform-api/           # Spring Boot, server-side
├── platform-knowledge/     # Spring Boot, server-side (генерация плана)
├── platform-executor/      # Spring Boot, local-side (оркестратор)
├── platform-agent/         # Spring Boot, local-side + playwright-server.js
│   └── chat-overlay/       # Tauri UI
├── docker/
│   ├── server/             # Dockerfile.api, Dockerfile.knowledge, docker-compose.yml
│   └── local/              # Dockerfile.executor, Dockerfile.agent, Dockerfile.playwright, docker-compose.yml
├── docs/
│   ├── DEPLOY_SERVER.ru.md
│   └── DEPLOY_LOCAL.ru.md
├── scripts/                # build/deploy/logs helpers
├── docker-compose.dev-db.yml   # только Postgres, для локальной разработки
├── Makefile
└── README.ru.md
```

---

## Разработка

Для разработки Java-сервисов без Docker:

```bash
# Поднять только Postgres
docker compose -f docker-compose.dev-db.yml up -d

# Запустить knowledge
cd platform-knowledge && ../mvnw spring-boot:run

# Запустить api (в другом терминале)
cd platform-api && ../mvnw spring-boot:run

# Запустить agent / executor по необходимости
```

Конфигурация по умолчанию ожидает Postgres на `localhost:5432`, knowledge на `localhost:8081`, agent на `localhost:7071`, executor на `localhost:7070`.

---

## Известные ограничения

- Генерация плана — heuristic stub (`StubPlanGenerator`). Интеграция с LLM запланирована отдельно.
- `RemoteResolver` поддерживает только разрешение action / entity type через REST; более сложные операции (`resolveBinding`, `resolveContext`) пока возвращают пустые ответы.
- Smoke-тест «всё в одном docker-compose» лежит в `docker/dev/docker-compose.smoke.yml` и запускается через `make smoke` (только для разработки).
