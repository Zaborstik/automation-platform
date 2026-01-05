# Platform Executor

Исполнитель планов выполнения действий. Берёт `Plan` из `platform-core`, превращает его шаги в UI-команды через `platform-agent`, управляет агентом и собирает `execution_log` с результатами выполнения.

## Архитектура

Platform Executor находится в середине execution-пайплайна:

```
[ ExecutionEngine (core) ]
         |
         v
    [ Plan ]
         |
         v
[ PlanExecutor (executor) ]
         |
         v
[ AgentService (agent) ]
         |
         v
[ UI Agent (Playwright) ]
```

### Роль Executor

Executor **не знает**:
- Как строить планы (это делает `ExecutionEngine`)
- Как работать с UI напрямую (это делает `AgentService`)

Executor **знает**:
- Как оркестрировать выполнение плана
- Как собирать execution_log
- Как агрегировать результаты выполнения

## Компоненты

### 1. PlanExecutor

Главный класс исполнителя. Принимает `Plan`, передаёт его в `AgentService`, собирает результаты и формирует `PlanExecutionResult`.

**Основные методы:**

```java
PlanExecutionResult execute(Plan plan)
```

Выполняет план синхронно и возвращает агрегированный результат с execution_log.

### 2. ExecutionLogEntry

Одна запись в execution_log. Связывает:
- Шаг плана (`PlanStep`) - что планировалось выполнить
- Результат выполнения (`StepExecutionResult`) - что реально произошло
- Метаданные (planId, stepIndex, timestamp)

**Структура:**

```java
ExecutionLogEntry {
    String planId;           // ID плана
    int stepIndex;           // Индекс шага в плане
    PlanStep step;           // Шаг плана (что планировалось)
    StepExecutionResult result; // Результат выполнения (что произошло)
    Instant loggedAt;        // Время логирования
}
```

### 3. PlanExecutionResult

Агрегированный результат выполнения плана. Содержит:
- Статус выполнения (success/failure)
- Временные метки (startedAt, finishedAt)
- Полный execution_log (список `ExecutionLogEntry`)

**Структура:**

```java
PlanExecutionResult {
    String planId;                    // ID плана
    boolean success;                  // Общий статус (true если все шаги успешны)
    Instant startedAt;                // Время начала выполнения
    Instant finishedAt;               // Время окончания выполнения
    List<ExecutionLogEntry> logEntries; // Execution log
}
```

## Как это работает

### Поток выполнения

1. **Вход**: `PlanExecutor.execute(Plan plan)`
   - План уже создан `ExecutionEngine` и содержит список `PlanStep`

2. **Оркестрация**: `PlanExecutor` передаёт план в `AgentService`
   ```java
   List<StepExecutionResult> results = agentService.executePlan(plan);
   ```

3. **Выполнение**: `AgentService` выполняет каждый шаг через UI-агента
   - Преобразует `PlanStep` → `AgentCommand`
   - Отправляет команды в Playwright сервер
   - Получает результаты выполнения

4. **Сборка лога**: `PlanExecutor` связывает шаги с результатами
   ```java
   for (int i = 0; i < steps.size(); i++) {
       ExecutionLogEntry entry = new ExecutionLogEntry(
           plan.getId(),
           i,
           steps.get(i),
           results.get(i),
           Instant.now()
       );
       logEntries.add(entry);
   }
   ```

5. **Агрегация**: Формируется `PlanExecutionResult`
   - Проверяется успешность всех шагов
   - Вычисляется общее время выполнения
   - Возвращается результат с полным execution_log

### Execution Log

Execution log - это наблюдаемый след выполнения плана. Каждая запись содержит:

- **Что планировалось**: `PlanStep` с типом, целевым элементом и объяснением
- **Что произошло**: `StepExecutionResult` с успешностью, сообщением, ошибкой, временем выполнения и скриншотом
- **Контекст**: ID плана, индекс шага, временная метка

**Пример execution_log:**

```
Plan: 550e8400-e29b-41d4-a716-446655440000
Started: 2024-01-15T10:30:00Z

[0] open_page /buildings/93939
    -> SUCCESS: Page opened in 1200ms
    -> Screenshot: /screenshots/plan-xxx-step-0.png

[1] explain "Открываю карточку здания"
    -> SUCCESS: Message logged in 5ms

[2] hover action(order_egrn_extract)
    -> SUCCESS: Element hovered in 150ms
    -> Screenshot: /screenshots/plan-xxx-step-2.png

[3] click action(order_egrn_extract)
    -> SUCCESS: Element clicked in 200ms
    -> Screenshot: /screenshots/plan-xxx-step-3.png

[4] wait result
    -> SUCCESS: Condition met in 3000ms
    -> Screenshot: /screenshots/plan-xxx-step-4.png

Finished: 2024-01-15T10:30:05Z
Status: SUCCESS
Total time: 4555ms
```

## Использование

### Базовый пример

```java
import org.example.executor.PlanExecutor;
import org.example.executor.PlanExecutionResult;
import org.example.agent.client.AgentClient;
import org.example.agent.service.AgentService;
import org.example.core.plan.Plan;
import org.example.core.resolver.InMemoryResolver;

// 1. Настраиваем зависимости
InMemoryResolver resolver = new InMemoryResolver();
// ... регистрируем EntityType, Action, UIBinding ...

AgentClient agentClient = new AgentClient("http://localhost:3000");
AgentService agentService = new AgentService(
    agentClient,
    resolver,
    "http://localhost:8080",  // базовый URL приложения
    false  // headless режим
);

// 2. Создаём executor
PlanExecutor executor = new PlanExecutor(agentService);

// 3. Получаем план (из ExecutionEngine или создаём вручную)
Plan plan = ...; // создан через ExecutionEngine.createPlan(request)

// 4. Выполняем план
PlanExecutionResult result = executor.execute(plan);

// 5. Анализируем результаты
System.out.println("Plan " + result.getPlanId() + " executed: " + 
    (result.isSuccess() ? "SUCCESS" : "FAILED"));
System.out.println("Execution time: " + 
    Duration.between(result.getStartedAt(), result.getFinishedAt()).toMillis() + "ms");

// 6. Просматриваем execution log
result.getLogEntries().forEach(entry -> {
    System.out.printf(
        "[%d] %s -> %s (%.0fms)%n",
        entry.getStepIndex(),
        entry.getStep().getType(),
        entry.getResult().isSuccess() ? "SUCCESS" : "FAILED",
        (double) entry.getResult().getExecutionTimeMs()
    );
    if (!entry.getResult().isSuccess()) {
        System.out.println("  Error: " + entry.getResult().getError());
    }
});
```

### Интеграция с ExecutionEngine

```java
import org.example.core.ExecutionEngine;
import org.example.core.execution.ExecutionRequest;
import org.example.executor.PlanExecutor;

// 1. Создаём план через ExecutionEngine
ExecutionEngine engine = new ExecutionEngine(resolver);
ExecutionRequest request = new ExecutionRequest(
    "Building",
    "93939",
    "order_egrn_extract",
    Map.of()
);
Plan plan = engine.createPlan(request);

// 2. Выполняем план через PlanExecutor
PlanExecutor executor = new PlanExecutor(agentService);
PlanExecutionResult result = executor.execute(plan);

// 3. Используем результат
if (result.isSuccess()) {
    // Действие выполнено успешно
    // Можно сохранить execution_log в БД для аудита
} else {
    // Обработка ошибок
    result.getLogEntries().stream()
        .filter(e -> !e.getResult().isSuccess())
        .forEach(e -> {
            log.error("Step {} failed: {}", 
                e.getStepIndex(), 
                e.getResult().getError()
            );
        });
}
```

## Интеграция с другими модулями

### Зависимости

Executor зависит от:
- **platform-core**: использует `Plan`, `PlanStep` для входных данных
- **platform-agent**: использует `AgentService` для выполнения планов

Executor **не зависит** от:
- **platform-api**: API может использовать executor, но не наоборот

### Типичный поток в системе

```
1. Client → POST /api/execution/plan
   ↓
2. ExecutionController → ExecutionService
   ↓
3. ExecutionService → ExecutionEngine.createPlan(request)
   ↓
4. ExecutionEngine → Planner → Plan
   ↓
5. ExecutionService → PlanExecutor.execute(plan)
   ↓
6. PlanExecutor → AgentService.executePlan(plan)
   ↓
7. AgentService → AgentClient → Playwright Server
   ↓
8. PlanExecutor ← StepExecutionResult[]
   ↓
9. PlanExecutor → PlanExecutionResult (с execution_log)
   ↓
10. ExecutionService ← PlanExecutionResult
    ↓
11. Client ← PlanExecutionResult (через API)
```

## Execution Log как источник истины

Execution log - это **наблюдаемый след** выполнения плана. Он позволяет:

1. **Аудит**: понять, что реально произошло при выполнении действия
2. **Отладка**: найти шаг, на котором произошла ошибка
3. **Воспроизведение**: повторить выполнение с теми же шагами
4. **Аналитика**: собрать метрики о производительности и успешности действий

### Формат лога

Каждая запись в execution_log содержит:

```java
{
    "planId": "550e8400-e29b-41d4-a716-446655440000",
    "stepIndex": 2,
    "step": {
        "type": "click",
        "target": "action(order_egrn_extract)",
        "explanation": "Выполняю действие 'Заказать выписку ЕГРН'",
        "parameters": {}
    },
    "result": {
        "stepType": "click",
        "stepTarget": "button[data-action='order_egrn_extract']",
        "success": true,
        "message": "Element clicked successfully",
        "error": null,
        "executedAt": "2024-01-15T10:30:03.123Z",
        "executionTimeMs": 200,
        "screenshotPath": "/screenshots/plan-xxx-step-2.png"
    },
    "loggedAt": "2024-01-15T10:30:03.125Z"
}
```

## Обработка ошибок

### Частичное выполнение

Если один из шагов плана завершился с ошибкой:

1. `PlanExecutor` продолжает выполнение остальных шагов
2. В execution_log добавляется запись с `success: false`
3. `PlanExecutionResult.success` будет `false`, если хотя бы один шаг неудачен

### Несоответствие шагов и результатов

Если `AgentService` вернул меньше результатов, чем шагов в плане:

- `PlanExecutor` создаёт синтетические failure-записи для недостающих шагов
- Это гарантирует, что execution_log всегда соответствует плану

### Инициализация агента

Если инициализация браузера не удалась:

- `AgentService` возвращает failure-результат для первого шага
- `PlanExecutor` не выполняет остальные шаги
- `PlanExecutionResult.success` будет `false`

## Будущие улучшения

- [ ] Асинхронное выполнение планов
- [ ] Retry механизм для неудачных шагов
- [ ] Сохранение execution_log в БД
- [ ] Метрики производительности (Prometheus)
- [ ] Интеграция с системой уведомлений
- [ ] Поддержка отмены выполнения плана
- [ ] Параллельное выполнение независимых шагов

## Принципы

1. **Executor не знает UI**: он работает только с абстракциями (`Plan`, `PlanStep`)
2. **Execution log - источник истины**: полный наблюдаемый след выполнения
3. **Детерминированность**: одинаковый план → одинаковый execution_log (при одинаковых условиях)
4. **Разделение ответственности**: executor оркестрирует, agent выполняет

