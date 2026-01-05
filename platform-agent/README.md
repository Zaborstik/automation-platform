# Platform Agent

UI-агент для выполнения планов через браузер с использованием Playwright.

## Архитектура

Platform Agent состоит из двух частей:

1. **Java компоненты** (`platform-agent`):
   - `AgentClient` - HTTP клиент для взаимодействия с Playwright сервером
   - `AgentService` - сервис для выполнения планов через агента
   - DTO для команд и результатов

2. **Node.js сервер** (`playwright-server.js`):
   - HTTP API для управления Playwright
   - Визуализация действий (подсветка, плавные движения)
   - Запись видео и скриншотов

## Установка

### 1. Установка Node.js зависимостей

```bash
cd platform-agent/src/main/resources
npm install
```

### 2. Запуск Playwright сервера

```bash
# В видимом режиме (по умолчанию)
node playwright-server.js

# В headless режиме
node playwright-server.js --headless

# С кастомным портом
PORT=3001 node playwright-server.js
```

Сервер будет доступен на `http://localhost:3000` (или указанном порте).

## Использование

### Базовый пример

```java
import org.example.agent.client.AgentClient;
import org.example.agent.service.AgentService;
import org.example.core.resolver.InMemoryResolver;
import org.example.core.plan.Plan;

// Создаем клиент
AgentClient client = new AgentClient("http://localhost:3000");

// Создаем сервис
Resolver resolver = new InMemoryResolver();
AgentService agentService = new AgentService(
    client, 
    resolver, 
    "http://localhost:8080",  // базовый URL приложения
    false  // headless режим
);

// Выполняем план
Plan plan = ...; // получен из ExecutionEngine
List<StepExecutionResult> results = agentService.executePlan(plan);

// Закрываем браузер
agentService.close();
```

### Команды агента

Агент поддерживает следующие команды:

- **OPEN_PAGE** - открыть страницу
- **CLICK** - кликнуть по элементу
- **HOVER** - навести курсор на элемент
- **TYPE** - ввести текст
- **WAIT** - ожидание (по селектору или networkidle)
- **EXPLAIN** - логирование объяснения действия
- **HIGHLIGHT** - подсветка элемента
- **SCREENSHOT** - сделать скриншот

### Преобразование PlanStep в команды

`AgentService` автоматически преобразует `PlanStep` в `AgentCommand`:

- `open_page` → `OPEN_PAGE`
- `click` → `CLICK` (с разрешением `action(actionId)` через Resolver)
- `hover` → `HOVER` (с разрешением `action(actionId)` через Resolver)
- `type` → `TYPE`
- `wait` → `WAIT`
- `explain` → `EXPLAIN`

## Визуализация

Playwright сервер обеспечивает:

1. **Плавное движение мыши** - курсор плавно движется к целевому элементу
2. **Подсветка элементов** - элементы подсвечиваются красной рамкой перед действием
3. **Замедление действий** - `slowMo: 100ms` для лучшей визуализации
4. **Скриншоты** - автоматические скриншоты после каждого действия
5. **Видео** - запись видео выполнения (опционально)

## API Endpoints

### `GET /health`
Проверка доступности сервера.

**Ответ:**
```json
{
  "status": "ok",
  "browser": true
}
```

### `POST /initialize`
Инициализация браузера.

**Тело запроса:**
```json
{
  "baseUrl": "http://localhost:8080",
  "headless": false
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Browser initialized",
  "data": {
    "baseUrl": "http://localhost:8080",
    "headless": false
  },
  "executionTimeMs": 0
}
```

### `POST /execute`
Выполнение команды.

**Тело запроса:**
```json
{
  "type": "CLICK",
  "target": "#button-id",
  "explanation": "Кликаю на кнопку",
  "parameters": {}
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Кликаю на кнопку",
  "data": {
    "selector": "#button-id",
    "screenshot": "/path/to/screenshot.png"
  },
  "executionTimeMs": 250
}
```

### `POST /close`
Закрытие браузера.

## Конфигурация

### Переменные окружения

- `PORT` - порт сервера (по умолчанию 3000)
- `HEADLESS` - запуск в headless режиме (`true`/`false`)
- `SCREENSHOTS_DIR` - директория для скриншотов

### Параметры AgentClient

```java
// С таймаутом по умолчанию
AgentClient client = new AgentClient("http://localhost:3000");

// С кастомным таймаутом
AgentClient client = new AgentClient(
    "http://localhost:3000", 
    Duration.ofMinutes(5)
);
```

## Интеграция с платформой

Agent интегрируется с остальными компонентами платформы:

1. **ExecutionEngine** создает `Plan` из `ExecutionRequest`
2. **AgentService** получает `Plan` и выполняет его через `AgentClient`
3. **AgentClient** отправляет команды в Playwright сервер
4. **Playwright сервер** выполняет действия в браузере

## Обработка ошибок

- Если элемент не найден, агент возвращает ошибку с описанием
- Если браузер не инициализирован, команды возвращают ошибку
- Все ошибки логируются и возвращаются в `StepExecutionResult`

## Будущие улучшения

- [ ] Retry механизм для неудачных шагов
- [ ] Fallback на vision-based поиск элементов
- [ ] Поддержка более сложных селекторов
- [ ] Интеграция с системой логирования платформы
- [ ] Метрики производительности


