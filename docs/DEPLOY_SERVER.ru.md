# Деплой серверной части

Серверная сторона состоит из трёх контейнеров:

| Сервис              | Порт   | Назначение                                                                |
|---------------------|--------|---------------------------------------------------------------------------|
| `postgres`          | 5432   | Хранение планов, шагов, результатов, справочников (actions / entity types). |
| `platform-knowledge`| 8081   | Генерация плана из текста пользователя (сейчас — heuristic stub).         |
| `platform-api`      | 8080   | REST gateway. Принимает запросы от локального executor'а, общается с БД и knowledge. |

Локальные `platform-executor` и `platform-agent` НЕ деплоятся на сервер — они запускаются на машине пользователя и ходят к серверу по HTTP.

---

## 1. Подготовка хоста

Минимальные требования: 2 CPU / 2 GB RAM / 10 GB диска, Linux с поддержкой Docker.

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y curl git docker.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER" && newgrp docker
```

Порт `8080` (`platform-api`) должен быть доступен снаружи (через файрвол / reverse proxy). `8081` и `5432` оставлять закрытыми — наружу они не нужны.

---

## 2. Клонирование репозитория

```bash
git clone https://github.com/your-org/automation-platform.git
cd automation-platform
```

---

## 3. Конфигурация (`docker/server/.env`)

```bash
cp docker/server/.env.example docker/server/.env
nano docker/server/.env
```

Что обязательно поменять:

- `POSTGRES_PASSWORD` — задать прод-пароль.
- `PLATFORM_API_PORT` — порт, под которым `platform-api` выставится наружу (по умолчанию `8080`).
- При желании — `LOG_LEVEL=INFO/DEBUG`.

Опционально (если используете registry):

```dotenv
API_IMAGE=ghcr.io/your-org/platform-api:1.0.0
KNOWLEDGE_IMAGE=ghcr.io/your-org/platform-knowledge:1.0.0
```

---

## 4. Запуск

Самый простой путь — через Makefile:

```bash
make server-deploy           # сборка локально + up -d
# или, если образы уже залиты в registry:
make server-pull
```

Под капотом это `scripts/deploy-server.sh`, который:

1. Проверяет наличие `docker/server/.env`.
2. Билдит образы (или `pull`, если запущено с `--pull`).
3. Поднимает контейнеры и ждёт пока `platform-api` пройдёт healthcheck (`/actuator/health`).

После старта Flyway внутри `platform-api` сам применит миграции из `platform-api/src/main/resources/db/migration` к Postgres.

Проверить состояние:

```bash
make server-logs                            # все логи
./scripts/logs-server.sh platform-api       # один сервис
curl http://localhost:8080/actuator/health  # должен вернуть {"status":"UP"}
```

---

## 5. Реверс-прокси / HTTPS (рекомендуется)

`platform-api` НЕ умеет TLS сам — поставьте перед ним nginx / traefik / Caddy. Пример минимального nginx:

```nginx
server {
    listen 443 ssl http2;
    server_name api.your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/api.your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.your-domain.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

После этого `docker/local/.env` на машинах пользователей будет содержать `PLATFORM_API_URL=https://api.your-domain.com`.

---

## 6. Обновления

```bash
git pull
make server-deploy            # rebuild + recreate
# или
make server-pull              # просто новые теги из registry
```

Persisted данные (`postgres_data` named volume) переживают пересоздание контейнеров.

---

## 7. Бэкап БД

```bash
docker exec platform-postgres \
    pg_dump -U platform platformdb \
    > backup-$(date +%F).sql
```

Восстановление:

```bash
cat backup-2026-05-16.sql | docker exec -i platform-postgres \
    psql -U platform -d platformdb
```

---

## 8. Troubleshooting

- **`platform-api` не становится healthy.** Смотреть `make server-logs`. Чаще всего — БД ещё не поднялась (compose сам перезапустит после `service_healthy`), либо в `application.properties` не подхватились env-переменные.
- **`KnowledgeClient` падает с 503.** Знач `platform-knowledge` не успел стартовать; healthcheck в compose уже это учитывает, но при ручных тестах подождите ~20 секунд.
- **Локальный executor не видит сервер.** Проверьте, что `PLATFORM_API_URL` в `docker/local/.env` доступен снаружи (через `curl`), и что reverse proxy не режет `POST` запросы > 1 МБ (планы могут быть большими).
- **Хочется зайти в psql.** `docker exec -it platform-postgres psql -U platform -d platformdb`.
