# Архитектура Platform Agent

## Обзор

Platform Agent - это мост между Java-платформой и браузером через Playwright. Он обеспечивает выполнение планов действий через UI с визуализацией и наблюдаемостью.

## Компоненты

### Java компоненты

#### 1. DTO (`org.example.agent.dto`)

- **AgentCommand** - команда для выполнения агентом
  - Типы: OPEN_PAGE, CLICK, TYPE, HOVER, WAIT, EXPLAIN, HIGHLIGHT, SCREENSHOT
  - Содержит target, explanation, parameters

- **AgentResponse** - ответ от агента
  - success/failure статус
  - message/error
  - data (результаты выполнения)
  - executionTimeMs

- **StepExecutionResult** - результат выполнения шага плана
  - Информация о шаге
  - Успех/ошибка
  - Скриншоты
  - Время выполнения

#### 2. Client (`org.example.agent.client`)

- **AgentClient** - HTTP клиент для взаимодействия с Playwright сервером
  - `execute(AgentCommand)` - выполнение команды
  - `initialize(baseUrl, headless)` - инициализация браузера
  - `close()` - закрытие браузера
  - `isAvailable()` - проверка доступности

- **AgentException** - исключение при работе с агентом

#### 3. Service (`org.example.agent.service`)

- **AgentService** - сервис для выполнения планов
  - `executePlan(Plan)` - выполнение плана целиком
  - Преобразует `PlanStep` в `AgentCommand`
  - Разрешает `action(actionId)` через `Resolver`
  - Собирает результаты выполнения

#### 4. Config (`org.example.agent.config`)

- **AgentConfiguration** - конфигурация агента
  - Создание `AgentClient` и `AgentService`
  - Проверка доступности агента
  - Настройки: URL, timeout, headless режим

### Node.js компоненты

#### Playwright Server (`playwright-server.js`)

HTTP сервер на Express, предоставляющий API для управления Playwright:

**Endpoints:**
- `GET /health` - проверка доступности
- `POST /initialize` - инициализация браузера
- `POST /execute` - выполнение команды
- `POST /close` - закрытие браузера

**Возможности:**
- Плавное движение мыши (smoothMove)
- Подсветка элементов (highlightElement)
- Автоматические скриншоты
- Запись видео (опционально)
- Замедление действий для визуализации

## Поток выполнения

```
ExecutionRequest
    ↓
ExecutionEngine.createPlan()
    ↓
Plan (с PlanStep[])
    ↓
AgentService.executePlan()
    ↓
Для каждого PlanStep:
    convertToCommand() → AgentCommand
    ↓
AgentClient.execute()
    ↓
HTTP POST /execute → Playwright Server
    ↓
Playwright выполняет действие в браузере
    ↓
AgentResponse
    ↓
StepExecutionResult
```

## Преобразование PlanStep → AgentCommand

| PlanStep.type | AgentCommand.type | Особенности |
|--------------|-------------------|-------------|
| `open_page` | `OPEN_PAGE` | Прямое преобразование |
| `click` | `CLICK` | Разрешение `action(actionId)` через Resolver |
| `hover` | `HOVER` | Разрешение `action(actionId)` через Resolver |
| `type` | `TYPE` | Извлечение `text` из parameters |
| `wait` | `WAIT` | Извлечение `timeout` из parameters |
| `explain` | `EXPLAIN` | Логирование объяснения |

## Разрешение action(actionId)

Когда `PlanStep.target` имеет формат `action(actionId)`:

1. `AgentService` извлекает `actionId`
2. Вызывает `resolver.findUIBinding(actionId)`
3. Получает `selector` из `UIBinding`
4. Использует `selector` в `AgentCommand`

Это позволяет платформе работать с семантическими действиями, а не координатами.

## Визуализация

Playwright сервер обеспечивает:

1. **Плавное движение мыши** - курсор плавно движется к целевому элементу (easing функция)
2. **Подсветка** - элементы подсвечиваются красной рамкой перед действием
3. **Замедление** - `slowMo: 100ms` для лучшей визуализации
4. **Скриншоты** - автоматические скриншоты после каждого действия
5. **Видео** - запись видео выполнения (опционально)

## Обработка ошибок

- Если элемент не найден → ошибка с описанием
- Если браузер не инициализирован → ошибка с инструкцией
- Все ошибки логируются и возвращаются в `StepExecutionResult`
- Можно добавить retry механизм в будущем

## Интеграция

Agent интегрируется с:

- **platform-core** - использует `Plan`, `PlanStep`, `Resolver`, `UIBinding`
- **platform-api** - может быть использован в `ExecutionService` для выполнения планов
- **platform-executor** - может быть частью executor модуля

## Будущие улучшения

- [ ] Retry механизм для неудачных шагов
- [ ] Fallback на vision-based поиск элементов
- [ ] Поддержка более сложных селекторов (XPath, text-based)
- [ ] Интеграция с системой логирования платформы
- [ ] Метрики производительности
- [ ] Поддержка параллельного выполнения нескольких планов
- [ ] Конфигурируемые стратегии визуализации


