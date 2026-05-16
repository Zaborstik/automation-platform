# Локальный запуск (на машине пользователя)

> **Документ:** локальный Docker-стек (executor, agent, Playwright) на машине пользователя.  
> **К чему относится:** каталог [`docker/local/`](../docker/local/) и связь с удалённым API.

Локально крутятся три контейнера:

| Сервис              | Порт           | Назначение                                                       |
|---------------------|----------------|------------------------------------------------------------------|
| `platform-executor` | `7070`         | Принимает запросы от chat-overlay, оркеструет план.              |
| `platform-agent`    | `7071` (вну.)  | Spring Boot обёртка над Playwright-сайдкаром.                    |
| `playwright-server` | `3000` (вну.)  | Node-сервер, в котором живёт Playwright + headless Chromium.     |

Наружу публикуется только `platform-executor` на `127.0.0.1:7070` (loopback). Туда стучится **chat-overlay** (Tauri, нативное приложение, в Docker не упаковано) и/или `curl`.

---

## 1. Что нужно установить

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (macOS / Windows) или Docker Engine + Compose v2 (Linux).
- ≥ 4 GB RAM свободной — образ Playwright тяжёлый.
- Сетевой доступ к серверу с `platform-api` (см. [`DEPLOY_SERVER.ru.md`](DEPLOY_SERVER.ru.md)).

Опционально (если планируете запускать chat-overlay в dev-режиме):
- Node 20+ и Rust toolchain — см. `platform-agent/chat-overlay/README*`.

---

## 2. Конфигурация (`docker/local/.env`)

```bash
cp docker/local/.env.example docker/local/.env
$EDITOR docker/local/.env
```

Минимум нужно поменять:

- `PLATFORM_API_URL` — URL вашего сервера, например `https://api.your-domain.com`.
- `PLATFORM_AGENT_BASE_URL` — URL приложения, которое будет автоматизироваться (по умолчанию подставляется тот же, что и API).
- `PLATFORM_AGENT_HEADLESS=true` — в Docker запускать только headless.

Скрипт `start-local.sh` откажется запускаться, если `PLATFORM_API_URL` остался равным `https://your-server.example.com` (защита от деплоя с плейсхолдером).

---

## 3. Запуск

```bash
make local-up
# эквивалентно:
# ./scripts/start-local.sh
```

Скрипт:

1. Проверяет `.env`.
2. Собирает образы (`platform-executor`, `platform-agent`, `platform-playwright`).
3. Поднимает контейнеры.
4. Ждёт пока `platform-executor` пройдёт healthcheck.

Проверить:

```bash
curl http://localhost:7070/actuator/health
# {"status":"UP"}
```

Логи:

```bash
make local-logs                                # все
./scripts/logs-local.sh platform-executor      # один сервис
```

Остановить:

```bash
make local-down
```

---

## 4. Подключение chat-overlay (Tauri)

Chat-overlay — нативное desktop-приложение, в Docker оно НЕ упаковано.

В файле `platform-agent/src/main/resources/chat-overlay-app.config.json` уже стоит:

```json
{
  "executorUrl": "http://localhost:7070",
  "statusPollIntervalMs": 1500
}
```

Если вы выставили executor на другом порту, поправьте `executorUrl` соответственно.

Дальше запускайте overlay как обычно (`pnpm tauri dev` из `chat-overlay/`). Он будет ходить в `http://localhost:7070/local/run` и опрашивать статус через `/local/status/{runId}`.

---

## 5. Тестовый прогон без overlay (curl)

```bash
# Запустить план из текста пользователя
curl -X POST http://localhost:7070/local/run \
     -H 'Content-Type: application/json' \
     -d '{"userInput":"открой страницу https://example.com"}'
# -> {"runId":"...","status":"RUNNING"}

# Получить статус
curl http://localhost:7070/local/status/<runId>
```

Executor сам:
1. Дёрнет `POST /api/plans/from-request` на сервере (`platform-api` → `platform-knowledge`).
2. Получит план обратно.
3. Подымет сессию у `platform-agent`.
4. Прогонит шаги, репортя результаты в `POST /api/plans/{id}/steps/{stepId}/result`.
5. Завершит run через `POST /api/plans/{id}/runs/finish`.

---

## 6. Troubleshooting

- **`platform-executor` не healthy.** Скорее всего падает на старте из-за неверного `PLATFORM_API_URL` — проверьте `make local-logs platform-executor`.
- **Playwright падает с `Failed to launch chromium`.** В compose уже выставлен `shm_size: 1gb`. Если у вас Linux без `/dev/shm` нужного размера, увеличьте значение в `docker/local/docker-compose.yml`.
- **`platform-agent` не видит playwright-server.** Они в одной compose-сети (`platform-local-net`), агент ходит на `http://playwright-server:3000` — проверьте, что оба контейнера запустились (`docker ps`).
- **CORS / CSP ошибки в chat-overlay.** В `tauri.conf.json` `connect-src` уже разрешает `http://localhost:7070` и `http://127.0.0.1:7070`. Если меняете порт — обновите и здесь.
- **Хочется ходить руками в БД.** Это серверная штука — подключайтесь к серверу и используйте `psql` внутри контейнера `platform-postgres` (см. серверный док).

---

## 7. Полезные команды

```bash
make help            # список всех make-целей
make local-up        # старт
make local-down      # стоп
make local-logs      # логи
make images          # пересобрать ВСЕ docker-образы (server+local)
make build           # mvn package всех модулей без тестов
```
