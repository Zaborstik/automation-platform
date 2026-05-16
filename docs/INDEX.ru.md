# Указатель документации

Все файлы ниже лежат в каталоге **`docs/`** (единая точка входа). README модулей (`platform-*`, `mcp-servers`) дублируются симлинками в **`docs/services/`** — редактировать нужно файл в папке модуля.

---

## Обзор и процессы

| Файл | К чему относится |
|------|------------------|
| [`README.ru.md`](README.ru.md) | Репозиторий целиком: архитектура server/local, быстрый старт, `make`, структура каталогов. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Как предлагать изменения, Issues/PR, стиль коммитов. |
| [`YAML_FILES_EXPLAINED.ru.md`](YAML_FILES_EXPLAINED.ru.md) | Назначение YAML в корне и CI (qodana, workflows, pre-commit и т.д.). |

---

## Архитектура и доменная логика

| Файл | К чему относится |
|------|------------------|
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Фактическая архитектура монорепозитория: модули, БД Flyway, API, расхождения с целевой схемой. |
| [`CORE-DETAILED-LOGIC-WITH-EXAMPLES.md`](CORE-DETAILED-LOGIC-WITH-EXAMPLES.md) | Подробная логика `platform-core`: планы, шаги, примеры. |
| [`DEPENDENCIES_AND_SERVICES_ANALYSIS.md`](DEPENDENCIES_AND_SERVICES_ANALYSIS.md) | Анализ зависимостей и сервисов (обзор связей). |
| [`LOGIC-WORKFLOW-DETAILED.md`](LOGIC-WORKFLOW-DETAILED.md) | Детализированный сценарий рабочих процессов / пайплайна. |
| [`LOGIC-FLOW-BOT-2.md`](LOGIC-FLOW-BOT-2.md) | Логический поток по отдельному треку (бот/автоматизация документации). |

---

## Деплой и эксплуатация

| Файл | К чему относится |
|------|------------------|
| [`DEPLOY_SERVER.ru.md`](DEPLOY_SERVER.ru.md) | Сервер: Docker, Postgres, `platform-api`, `platform-knowledge`, автодеплой по SSH. |
| [`DEPLOY_LOCAL.ru.md`](DEPLOY_LOCAL.ru.md) | Локальная машина: executor, agent, Playwright, chat-overlay. |

Шаблоны окружения (не Markdown): корень **`docker/server/.env.example`**, **`docker/local/.env.example`**, **`.env.deploy.example`**.

---

## Описание модулей (README в репозитории)

Файлы ниже хранятся в каталогах модулей; в **`docs/services/…`** на них симлинки для удобной навигации.

| Путь в `docs/services/` | Модуль / тема |
|-------------------------|----------------|
| [`services/platform-core/README.ru.md`](services/platform-core/README.ru.md) | Библиотека `platform-core`: домен, Plan DSL. |
| [`services/platform-api/README.ru.md`](services/platform-api/README.ru.md) | Серверный gateway `platform-api` (REST, Spring Boot). |
| [`services/platform-agent/README.ru.md`](services/platform-agent/README.ru.md) | Локальный агент + Playwright-сайдкар. |
| [`services/platform-executor/README.ru.md`](services/platform-executor/README.ru.md) | Локальный оркестратор выполнения планов. |
| [`services/mcp-servers/README.ru.md`](services/mcp-servers/README.ru.md) | MCP-серверы в каталоге `mcp-servers/`. |
| [`services/mcp-servers/dev-entity/README.ru.md`](services/mcp-servers/dev-entity/README.ru.md) | MCP dev-entity. |

Явного `README` у **`platform-knowledge`** в репозитории нет — см. общий [`README.ru.md`](README.ru.md) и [`ARCHITECTURE.md`](ARCHITECTURE.md).

---

## Шаблоны GitHub

Файлы в **`.github/`** (Issue templates, PR template, SECURITY) не переносятся — их ожидает интерфейс GitHub по стандартным путям.
