# mcp-entity — MCP-сервер над внешним Entity REST API

Это **референсный MCP-сервер** (Model Context Protocol) на Node.js, который через stdio
экспонирует CRUD-инструменты над внешним REST API (стиль Jmix/CUBA: `POST /json/login`,
`/json/v2/mcp/entities/*`). Запускается LLM-клиентом (Claude Desktop, Cursor, Codex и т.п.)
как локальный процесс.

Он **не** связан с `platform-api` и в Maven-реактор не входит. Подробнее см.
[`mcp-servers/README.ru.md`](../README.ru.md).

## Требования

- Node.js >= 18 (нужен встроенный `fetch`).
- Доступ к внешнему серверу, к которому будет ходить MCP-обёртка.

## Установка

```bash
cd mcp-servers/dev-entity
npm install
```

## Переменные окружения

| Переменная              | Назначение                                                    | По умолчанию              |
| ----------------------- | ------------------------------------------------------------- | ------------------------- |
| `MCP_ENTITY_BASE_URL`   | Базовый URL внешнего REST API.                                | `http://localhost:8080`   |
| `MCP_ENTITY_USER`       | Логин для `POST /json/login`. Обязателен.                     | —                         |
| `MCP_ENTITY_PASSWORD`   | Пароль для `POST /json/login`. Обязателен.                    | —                         |

После логина сервер кэширует токен и отправляет его в заголовке `X-Auth`. При ответе `401/403`
или JSON-ошибке `illegal token` токен сбрасывается и логин повторяется один раз автоматически.

## Запуск вручную (для отладки)

```bash
MCP_ENTITY_BASE_URL=http://localhost:8080 \
MCP_ENTITY_USER=admin \
MCP_ENTITY_PASSWORD=admin \
npm start
```

Сервер работает по stdio: после старта он молча ждёт MCP-запросов на stdin
и пишет ответы на stdout. Для интерактивного использования его нужно подключить
к MCP-клиенту (см. ниже).

## Подключение к LLM-клиенту

### Cursor

В `~/.cursor/mcp.json` (или в `mcp.json` рабочего пространства):

```json
{
  "mcpServers": {
    "dev-entity": {
      "command": "node",
      "args": [
        "/Users/zaborstik/IdeaProjects/automation-platform/mcp-servers/dev-entity/server.js"
      ],
      "env": {
        "MCP_ENTITY_BASE_URL": "http://localhost:8080",
        "MCP_ENTITY_USER": "admin",
        "MCP_ENTITY_PASSWORD": "admin"
      }
    }
  }
}
```

### Claude Desktop

В `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) —
формат идентичен:

```json
{
  "mcpServers": {
    "dev-entity": {
      "command": "node",
      "args": [
        "/Users/zaborstik/IdeaProjects/automation-platform/mcp-servers/dev-entity/server.js"
      ],
      "env": {
        "MCP_ENTITY_BASE_URL": "http://localhost:8080",
        "MCP_ENTITY_USER": "admin",
        "MCP_ENTITY_PASSWORD": "admin"
      }
    }
  }
}
```

Путь в `args` нужно подставить под свой рабочий каталог. Если запускаете на другом
хосте — заменить `MCP_ENTITY_BASE_URL`.

## Экспонируемые инструменты

| Имя             | Метаданные / данные | Назначение                                             |
| --------------- | ------------------- | ------------------------------------------------------ |
| `create_table`  | Metadata            | Создать тип сущности (таблицу).                        |
| `add_field`     | Metadata            | Добавить поле к типу. Поддерживает lookup-поля.        |
| `list_tables`   | Metadata            | Список всех типов сущностей.                           |
| `get_table`     | Metadata            | Получить тип сущности по id или имени.                 |
| `create_rows`   | Data                | Вставить одну или несколько строк.                     |
| `read_rows`     | Data                | Прочитать строки с фильтрами / пагинацией / сортировкой. |
| `update_rows`   | Data                | Обновить одну или несколько строк по PK.               |
| `delete_rows`   | Data                | Удалить одну или несколько строк по PK.                |

Поддерживаемые типы полей для `add_field.fieldType`:
`INTEGER`, `DOUBLE`, `BOOLEAN`, `STRING`, `DATETIME`, `DATE`, `TIME`, `TEXT`, `BIGINT`.
Если указан `lookupEntity` (опц. + `lookupField`) — создаётся autocomplete-поле.

Полные JSON Schema для входов смотрите в [server.js](./server.js) (хендлер `ListToolsRequestSchema`).

## Архитектура (коротко)

```
LLM client ──stdio──▶ server.js ──HTTP──▶ External Entity REST API
                       │
                       ├─ login(): кэш токена
                       ├─ requestWithAuth(): X-Auth + retry на 401/illegal token
                       └─ tool handlers: маппинг MCP → REST
```

Файл [server.js](./server.js) содержит и описание инструментов (`ListToolsRequestSchema`),
и их обработчики (`CallToolRequestSchema`), и HTTP-клиент. При желании его можно разрезать
на `auth.js` / `tools.js` / `server.js` без смены поведения.
