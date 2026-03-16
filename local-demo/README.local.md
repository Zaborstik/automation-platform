# Demo Runner — Локальный запуск

> **Этот файл и вся папка `local-demo/` не попадают в git** (добавлены в `.gitignore`).

## Запуск за один шаг

```bash
./local-demo/run-demo.sh
```

Или из корня проекта в IntelliJ: `Edit Configurations → + → Shell Script → local-demo/run-demo.sh` → Run.

---

## Что происходит при запуске

| Шаг | Что делает скрипт |
|-----|-------------------|
| 1   | Проверяет `java`, `node`, `npm`, `curl` |
| 2   | Собирает проект через Maven (`-DskipTests`) |
| 3   | Устанавливает npm-зависимости (`playwright`, `express`) |
| 4   | Запускает **Playwright Server** на `http://localhost:3000` |
| 5   | Запускает **Spring Boot API** на `http://localhost:8080` (H2, без Docker) |
| 6   | Ждёт готовности обоих сервисов |
| 7   | Создаёт demo-план через `POST /api/plans` |
| 8   | Запускает выполнение через `POST /api/plans/{id}/execute` |
| 9   | Сохраняет скриншоты, открывает последний в Preview |
| 10  | Ждёт Enter → завершает все фоновые процессы |

---

## Demo-сценарий: поиск на DuckDuckGo

Бот открывает браузер и выполняет следующие шаги с **человекоподобным движением курсора**:

```
1. Открыть https://duckduckgo.com
2. Навести курсор на поле ввода, кликнуть, набрать текст посимвольно
3. Нажать кнопку Search
4. Дождаться загрузки результатов
```

Поисковый запрос: `Spring Boot Playwright browser automation demo`

После каждого шага автоматически сохраняется скриншот в `local-demo/screenshots/`.

---

## Требования

| Инструмент | Версия | Как проверить |
|------------|--------|---------------|
| Java (JDK) | 21+    | `java -version` |
| Node.js    | 18+    | `node --version` |
| npm        | входит в Node | `npm --version` |
| Maven      | 3.8+   | `mvn --version` (или встроенный IntelliJ) |
| curl       | любая  | `curl --version` |

> Docker **не нужен** — база данных H2 запускается в памяти вместе с API.

---

## Файлы и логи

```
local-demo/
  run-demo.sh        ← главный скрипт
  README.local.md    ← этот файл
  screenshots/       ← скриншоты каждого шага сценария
  api.log            ← логи Spring Boot (создаётся при запуске)
  playwright.log     ← логи Playwright Server (создаётся при запуске)
```

---

## Полезные URL после запуска

| URL | Что это |
|-----|---------|
| `http://localhost:8080/api/plans` | REST API планов |
| `http://localhost:8080/h2-console` | H2 консоль (просмотр БД) |
| `http://localhost:3000/health` | Healthcheck Playwright Server |

---

## Частые проблемы

**Maven не найден**
```bash
brew install maven
# или установить JDK через sdkman: sdk install java 21
```

**Порт уже занят**
```bash
lsof -ti tcp:8080 | xargs kill -9
lsof -ti tcp:3000 | xargs kill -9
```

**Chromium не установлен**
```bash
cd platform-agent/src/main/resources
npm install
npx playwright install chromium
```

**Ошибка на шаге создания плана**
Проверьте `api.log` — возможно, API ещё не успел подняться или произошла ошибка Flyway-миграции.
Попробуйте запустить повторно — H2 каждый раз создаётся с нуля.
