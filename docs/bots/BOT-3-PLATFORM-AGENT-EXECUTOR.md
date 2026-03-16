# BOT 3: platform-agent + platform-executor — Execution Layer

## Scope

**Модули:** `platform-agent/src/` и `platform-executor/src/`  
**Packages:**
- `com.zaborstik.platform.agent.*`
- `com.zaborstik.platform.executor.*`

**Запрещено трогать:** любые файлы вне `platform-agent/` и `platform-executor/`  
**Важно:** НЕ трогать `platform-agent/src/main/resources/playwright-server.js` (Node.js — отдельный проект, меняется вручную).

## Текущее состояние модулей

### platform-agent

| Пакет | Файлы | Описание |
|-------|-------|----------|
| `agent/client/` | `AgentClient.java`, `AgentException.java` | HTTP клиент к Playwright серверу |
| `agent/config/` | `AgentConfiguration.java` | Конфигурация агента (non-Spring) |
| `agent/dto/` | `AgentCommand.java`, `AgentResponse.java`, `StepExecutionResult.java` | DTO для общения с сервером |
| `agent/service/` | `AgentService.java` | Выполнение планов через UI-агент |
| `agent/example/` | `AgentExample.java` | Пример использования |

### platform-executor

| Пакет | Файлы | Описание |
|-------|-------|----------|
| `executor/` | `PlanExecutor.java` | Исполнитель планов (синхронный) |
| `executor/` | `PlanExecutionResult.java` | Результат выполнения |
| `executor/` | `ExecutionLogEntry.java` | Запись лога выполнения |

### Существующие тесты:
- `platform-executor/`: `PlanExecutorTest.java`, `PlanExecutionResultTest.java`, `ExecutionLogEntryTest.java`
- `platform-agent/`: тестов **нет** (!)

### Поддерживаемые команды AgentCommand.CommandType:
```
OPEN_PAGE, CLICK, CLICK_AT, TYPE, HOVER, RESOLVE_COORDS, WAIT, EXPLAIN, HIGHLIGHT, SCREENSHOT
```

### Поддерживаемые типы шагов в AgentService:
```
open_page, click, hover, type, wait, explain
```

### НЕ поддержано (но есть в БД как action):
- `select_option` — нет маппинга в AgentService
- `read_text` — нет маппинга в AgentService
- `take_screenshot` — нет маппинга в AgentService (есть только как CommandType)

---

## Что нужно сделать

### Задача 3.1: Добавить `RetryPolicy` и логику ретраев

**Цель:** Повторять упавшие шаги при транзиентных ошибках.

**Файлы:**
- Создать: `platform-agent/src/main/java/com/zaborstik/platform/agent/dto/RetryPolicy.java`
- Изменить: `platform-agent/src/main/java/com/zaborstik/platform/agent/service/AgentService.java`
- Создать: `platform-agent/src/test/java/com/zaborstik/platform/agent/dto/RetryPolicyTest.java`
- Создать: `platform-agent/src/test/java/com/zaborstik/platform/agent/service/AgentServiceTest.java`

**Что делать:**
1. Record `RetryPolicy(int maxRetries, long delayMs, List<String> retryableErrorPatterns)`:
   - `maxRetries` >= 0
   - `delayMs` >= 0
   - `retryableErrorPatterns` — подстроки ошибок, при которых ретрай допустим (например `"timeout"`, `"not found"`, `"not visible"`)
   - Метод `boolean isRetryable(String errorMessage)` — проверяет, содержит ли сообщение паттерн
   - Статический метод `RetryPolicy defaultPolicy()` — `maxRetries=2, delayMs=1000, patterns=["timeout", "not found", "not visible"]`
   - Статический метод `RetryPolicy noRetry()` — `maxRetries=0`
2. В `AgentService`:
   - Добавить поле `RetryPolicy retryPolicy`
   - В конструктор добавить `RetryPolicy` (с дефолтным значением для обратной совместимости)
   - В `executeStep()`: обернуть выполнение в цикл ретраев
   - Логировать номер попытки

**Тест RetryPolicy:**
- `defaultPolicy` — правильные значения
- `noRetry` — `maxRetries=0`
- `isRetryable("Element not found on page")` → true
- `isRetryable("Something unexpected")` → false

**Тест AgentService:**
- Шаг успешен с первой попытки — 1 вызов agentClient
- Шаг падает с retryable ошибкой — делает `maxRetries` + 1 попыток
- Шаг падает с не-retryable ошибкой — сразу failure без ретраев
- Шаг успешен со второй попытки — 2 вызова agentClient

---

### Задача 3.2: Добавить `StepExecutionCallback` интерфейс

**Цель:** Механизм оповещения о прогрессе выполнения.

**Файлы:**
- Создать: `platform-agent/src/main/java/com/zaborstik/platform/agent/service/StepExecutionCallback.java`
- Изменить: `platform-agent/src/main/java/com/zaborstik/platform/agent/service/AgentService.java`
- Создать: `platform-agent/src/test/java/com/zaborstik/platform/agent/service/StepExecutionCallbackTest.java`

**Что делать:**
1. Интерфейс `StepExecutionCallback`:
   ```java
   void onStepStarted(PlanStep step, int stepIndex, int totalSteps);
   void onStepCompleted(PlanStep step, StepExecutionResult result, int stepIndex);
   void onPlanStarted(Plan plan);
   void onPlanCompleted(Plan plan, List<StepExecutionResult> results, boolean success);
   ```
2. Добавить `NoOpCallback` — реализация по умолчанию (пустые методы)
3. В `AgentService.executePlan()`:
   - Добавить перегрузку `executePlan(Plan plan, StepExecutionCallback callback)`
   - Вызывать callback на каждом этапе
   - Существующий `executePlan(Plan plan)` делегирует с `NoOpCallback`

**Тест:**
- Callback вызывается в правильном порядке: planStarted → stepStarted → stepCompleted → ... → planCompleted
- При ошибке callback всё равно вызывается
- NoOpCallback не бросает исключений

---

### Задача 3.3: Добавить поддержку команды `select_option`

**Цель:** Маппинг PlanStep с типом `select_option` на агентскую команду.

**Файлы:**
- Изменить: `platform-agent/src/main/java/com/zaborstik/platform/agent/dto/AgentCommand.java`
- Изменить: `platform-agent/src/main/java/com/zaborstik/platform/agent/service/AgentService.java`
- Создать или изменить: `platform-agent/src/test/java/com/zaborstik/platform/agent/service/AgentServiceCommandMappingTest.java`

**Что делать:**
1. В `AgentCommand` добавить `SELECT_OPTION` в enum `CommandType` (если нет)
2. Добавить фабричный метод:
   ```java
   static AgentCommand selectOption(String selector, String value, String explanation)
   ```
3. В `AgentService.convertToCommand()` добавить case `"select_option"`:
   - Selector из `step.entityId()` через `resolveSelector()`
   - Value из `step.actions().get(0).metaValue()`
4. Добавить `"select_option"` в `isCoordinateStep()` если нужно

**Тест:**
- Конвертация PlanStep(type=select_option) → AgentCommand(SELECT_OPTION)
- metaValue передаётся как параметр value
- Пустой metaValue → пустая строка

---

### Задача 3.4: Добавить поддержку команды `read_text`

**Цель:** Чтение текста элемента для валидации/анализа.

**Файлы:**
- Изменить: `platform-agent/src/main/java/com/zaborstik/platform/agent/dto/AgentCommand.java`
- Изменить: `platform-agent/src/main/java/com/zaborstik/platform/agent/service/AgentService.java`
- Изменить: `platform-agent/src/test/java/com/zaborstik/platform/agent/service/AgentServiceCommandMappingTest.java`

**Что делать:**
1. Добавить `READ_TEXT` в enum `CommandType` (если нет)
2. Фабричный метод:
   ```java
   static AgentCommand readText(String selector, String explanation)
   ```
3. В `convertToCommand()` добавить case `"read_text"`:
   - Selector из `step.entityId()` через `resolveSelector()`
4. Результат: текст элемента в `AgentResponse.data.get("text")`

**Тест:**
- Конвертация PlanStep(type=read_text) → AgentCommand(READ_TEXT)
- Selector корректно resolveится

---

### Задача 3.5: Добавить поддержку команды `take_screenshot`

**Цель:** Явное снятие скриншота как действие плана.

**Файлы:**
- Изменить: `platform-agent/src/main/java/com/zaborstik/platform/agent/service/AgentService.java`
- Изменить: `platform-agent/src/test/java/com/zaborstik/platform/agent/service/AgentServiceCommandMappingTest.java`

**Что делать:**
1. В `convertToCommand()` добавить case `"take_screenshot"`:
   ```java
   case "take_screenshot":
       return AgentCommand.screenshot(target != null ? target : "fullpage", explanation);
   ```
2. Скриншот путь из `AgentResponse.data.get("screenshot")` → `StepExecutionResult.screenshotPath`

**Тест:**
- Конвертация PlanStep(type=take_screenshot) → SCREENSHOT command
- screenshotPath корректно извлекается из response

---

### Задача 3.6: Расширить `StepExecutionResult` дополнительными полями

**Цель:** Более богатая метаинформация о выполнении шага.

**Файлы:**
- Изменить: `platform-agent/src/main/java/com/zaborstik/platform/agent/dto/StepExecutionResult.java`
- Изменить: `platform-agent/src/test/java/com/zaborstik/platform/agent/dto/StepExecutionResultTest.java` (создать если нет)

**Что делать:**
1. Добавить поля:
   - `int retryCount` — сколько ретраев было (0 = успех с первого раза)
   - `int stepIndex` — индекс шага в плане (0-based)
   - `String commandType` — тип агентской команды (CLICK, TYPE, etc.)
2. Обновить фабричные методы `success()` и `failure()` — добавить перегрузки с новыми полями
3. Сохранить обратную совместимость — старые методы ставят дефолтные значения (0, -1, null)

**Тест:**
- Создание с новыми полями
- Обратная совместимость: старые методы работают
- retryCount, stepIndex, commandType доступны через геттеры

---

### Задача 3.7: Добавить `AgentHealthCheck`

**Цель:** Проверка доступности Playwright сервера перед выполнением.

**Файлы:**
- Создать: `platform-agent/src/main/java/com/zaborstik/platform/agent/service/AgentHealthCheck.java`
- Создать: `platform-agent/src/test/java/com/zaborstik/platform/agent/service/AgentHealthCheckTest.java`

**Что делать:**
1. Класс `AgentHealthCheck`:
   ```java
   public AgentHealthCheck(AgentClient agentClient)
   boolean isHealthy()  // один вызов isAvailable()
   boolean waitForHealthy(long timeoutMs, long pollIntervalMs)  // ждёт с интервалом
   ```
2. `waitForHealthy`: опрашивает `agentClient.isAvailable()` каждые `pollIntervalMs` мс, до `timeoutMs`
3. Возвращает `true` как только сервер стал доступен, `false` по таймауту
4. Логирует попытки

**Тест (Mockito):**
- Сервер сразу доступен → `isHealthy()` = true
- Сервер недоступен → `isHealthy()` = false
- `waitForHealthy`: сервер становится доступен на 3-й попытке → true
- `waitForHealthy`: таймаут → false

---

### Задача 3.8: Расширить `PlanExecutor` пошаговым выполнением

**Цель:** Поддержка выполнения с callback и остановкой при критической ошибке.

**Файлы:**
- Изменить: `platform-executor/src/main/java/com/zaborstik/platform/executor/PlanExecutor.java`
- Изменить: `platform-executor/src/test/java/com/zaborstik/platform/executor/PlanExecutorTest.java`

**Что делать:**
1. Добавить метод:
   ```java
   public PlanExecutionResult execute(Plan plan, boolean stopOnFailure)
   ```
   - Если `stopOnFailure=true` и шаг упал — прекратить выполнение, оставшиеся шаги пометить как не выполненные
   - Если `stopOnFailure=false` — текущее поведение (выполнять все)
2. Существующий `execute(Plan plan)` делегирует в `execute(plan, false)` — обратная совместимость
3. Добавить перегрузку с `StepExecutionCallback`:
   ```java
   public PlanExecutionResult execute(Plan plan, boolean stopOnFailure, StepExecutionCallback callback)
   ```
   - Передавать callback в `agentService.executePlan(plan, callback)`

**Тест:**
- `stopOnFailure=true`: при падении 2-го из 4 шагов — 3-й и 4-й не выполнены
- `stopOnFailure=false`: все 4 шага выполнены, даже если 2-й упал
- Обратная совместимость: `execute(plan)` работает как раньше
- С callback: callback вызывается

---

### Задача 3.9: Добавить `AsyncPlanExecutor`

**Цель:** Неблокирующее выполнение планов.

**Файлы:**
- Создать: `platform-executor/src/main/java/com/zaborstik/platform/executor/AsyncPlanExecutor.java`
- Создать: `platform-executor/src/test/java/com/zaborstik/platform/executor/AsyncPlanExecutorTest.java`

**Что делать:**
1. Класс `AsyncPlanExecutor`:
   ```java
   public AsyncPlanExecutor(PlanExecutor planExecutor, ExecutorService executorService)
   
   CompletableFuture<PlanExecutionResult> executeAsync(Plan plan)
   CompletableFuture<PlanExecutionResult> executeAsync(Plan plan, boolean stopOnFailure)
   ```
2. Внутри — `CompletableFuture.supplyAsync(() -> planExecutor.execute(plan, stopOnFailure), executorService)`
3. Добавить удобный конструктор `AsyncPlanExecutor(PlanExecutor planExecutor)` — с пулом по умолчанию
4. Метод `void shutdown()` — для корректного завершения

**Тест:**
- Выполнение возвращает Future, `get()` возвращает результат
- Несколько планов выполняются параллельно
- После `shutdown()` новые задачи отклоняются

---

## Порядок выполнения

```
3.1 RetryPolicy + ретраи в AgentService
  ↓
3.6 Расширение StepExecutionResult (параллельно с 3.1 или после)
  ↓
3.2 StepExecutionCallback (параллельно с 3.6)
  ↓
3.3 select_option command (независимый)
  ↓
3.4 read_text command (независимый, параллельно с 3.3)
  ↓
3.5 take_screenshot command (независимый, параллельно с 3.4)
  ↓
3.7 AgentHealthCheck (независимый)
  ↓
3.8 PlanExecutor stepByStep (зависит от 3.2 — callback)
  ↓
3.9 AsyncPlanExecutor (зависит от 3.8)
```

## Зависимости от других ботов

- **Нет входящих зависимостей.** Бот 3 работает полностью автономно.
- **Исходящие:** Бот 2 (задача 2.8) будет использовать `StepExecutionCallback` и `AsyncPlanExecutor` для интеграции — но только после завершения Бота 3.
- **Важно:** Бот 3 НЕ меняет `playwright-server.js`. Если нужна поддержка `SELECT_OPTION` или `READ_TEXT` на стороне Node.js — это делается отдельно, вручную.
