# BOT 1: platform-core — Domain Logic

## Scope

**Module:** `platform-core/src/` only  
**Package base:** `com.zaborstik.platform.core`  
**Запрещено трогать:** любые файлы вне `platform-core/`

## Текущее состояние модуля

### Что уже есть:

| Пакет | Файлы | Описание |
|-------|-------|----------|
| `core/domain/` | `Action.java`, `ActionType.java`, `Attachment.java`, `EntityType.java`, `State.java`, `UIBinding.java`, `Workflow.java`, `WorkflowStep.java` | Domain records — неизменяемые модели |
| `core/plan/` | `Plan.java`, `PlanStep.java`, `PlanStepAction.java`, `PlanResult.java`, `PlanStepLogEntry.java` | Модель плана выполнения |
| `core/planner/` | `Planner.java` | Построение плана (только одношаговый!) |
| `core/execution/` | `ExecutionRequest.java` | Запрос на выполнение |
| `core/resolver/` | `Resolver.java` (interface), `InMemoryResolver.java` | Поиск сущностей |
| `core/logging/` | `CustomLevelConverter.java` | Логирование |
| `core/` | `ExecutionEngine.java` | Координация Resolver + Planner |
| `core/example/` | `ExampleUsage.java` | Пример использования |

### Существующие тесты (10 файлов):
- `ActionTest.java`, `EntityTypeTest.java`, `StateTest.java`, `UIBindingTest.java`
- `ExecutionRequestTest.java`, `PlanTest.java`, `PlanStepTest.java`, `PlanStepActionTest.java`
- `PlannerTest.java`, `InMemoryResolverTest.java`, `ExecutionEngineTest.java`

---

## Что нужно сделать

### Задача 1.1: Добавить `WorkflowTransition` record

**Цель:** Модель допустимых переходов ЖЦ (например, `new` → `in_progress`, `in_progress` → `completed`).

**Файлы:**
- Создать: `platform-core/src/main/java/com/zaborstik/platform/core/domain/WorkflowTransition.java`
- Создать: `platform-core/src/test/java/com/zaborstik/platform/core/domain/WorkflowTransitionTest.java`

**Что делать:**
1. Создать record `WorkflowTransition(String workflowId, String fromStepInternalName, String toStepInternalName)`
2. `workflowId` — обязательный, not null
3. `fromStepInternalName` — обязательный, not null
4. `toStepInternalName` — обязательный, not null
5. Валидация в compact constructor: `Objects.requireNonNull` для всех полей
6. Переопределить `equals`/`hashCode` по всем трём полям
7. Переопределить `toString`

**Тест:**
- Успешное создание
- NPE при null полях
- equals/hashCode: одинаковые и разные объекты
- toString содержит все поля

---

### Задача 1.2: Расширить `Resolver` интерфейс методами для переходов ЖЦ

**Цель:** Resolver должен уметь возвращать допустимые переходы ЖЦ.

**Файлы:**
- Изменить: `platform-core/src/main/java/com/zaborstik/platform/core/resolver/Resolver.java`
- Изменить: `platform-core/src/main/java/com/zaborstik/platform/core/resolver/InMemoryResolver.java`
- Изменить: `platform-core/src/test/java/com/zaborstik/platform/core/resolver/InMemoryResolverTest.java`

**Что делать:**
1. Добавить в `Resolver`:
   ```java
   List<WorkflowTransition> findTransitions(String workflowId);
   Optional<WorkflowTransition> findTransition(String workflowId, String fromStep, String toStep);
   ```
2. В `InMemoryResolver`:
   - Добавить поле `Map<String, List<WorkflowTransition>> transitions` (ключ — workflowId)
   - Добавить метод `registerTransition(WorkflowTransition transition)`
   - Реализовать `findTransitions` и `findTransition`

**Тест:**
- Зарегистрировать переходы: `new→in_progress`, `in_progress→completed`, `in_progress→failed`
- `findTransitions("wf-plan")` — возвращает все переходы
- `findTransition("wf-plan", "new", "in_progress")` — находит
- `findTransition("wf-plan", "completed", "new")` — empty (запрещённый переход)
- `findTransitions("nonexistent")` — пустой список

---

### Задача 1.3: Добавить `LifecycleManager`

**Цель:** Класс для валидации и выполнения переходов ЖЦ плана и шага.

**Файлы:**
- Создать: `platform-core/src/main/java/com/zaborstik/platform/core/lifecycle/LifecycleManager.java`
- Создать: `platform-core/src/test/java/com/zaborstik/platform/core/lifecycle/LifecycleManagerTest.java`

**Что делать:**
1. Конструктор `LifecycleManager(Resolver resolver)`
2. Метод `boolean canTransition(String workflowId, String currentStep, String targetStep)` — проверяет, допустим ли переход
3. Метод `void validateTransition(String workflowId, String currentStep, String targetStep)` — бросает `IllegalStateException` если переход недопустим
4. Метод `String getNextStep(String workflowId, String currentStep)` — возвращает первый допустимый следующий шаг (или бросает исключение)

**Тест:**
- `canTransition` возвращает `true` для допустимого перехода
- `canTransition` возвращает `false` для недопустимого
- `validateTransition` не бросает исключение для допустимого
- `validateTransition` бросает `IllegalStateException` для недопустимого
- `getNextStep` возвращает корректный следующий шаг
- `getNextStep` бросает исключение если переходов нет
- NPE при null аргументах

---

### Задача 1.4: Добавить `PlanValidator`

**Цель:** Валидация целостности плана перед выполнением.

**Файлы:**
- Создать: `platform-core/src/main/java/com/zaborstik/platform/core/planner/PlanValidator.java`
- Создать: `platform-core/src/test/java/com/zaborstik/platform/core/planner/PlanValidatorTest.java`

**Что делать:**
1. Конструктор `PlanValidator(Resolver resolver)`
2. Метод `List<String> validate(Plan plan)` — возвращает список ошибок (пустой = валидный)
3. Проверки:
   - План не null, id не пустой
   - У плана есть хотя бы один шаг
   - Каждый шаг имеет хотя бы одно действие
   - Все действия применимы к entity_type шага (через `resolver.isActionApplicable`)
   - sortOrder уникальный и последовательный
   - workflowId существует (через `resolver.findWorkflow`)
4. Метод `void validateOrThrow(Plan plan)` — бросает `IllegalArgumentException` с объединёнными ошибками

**Тест:**
- Валидный план — пустой список ошибок
- План без шагов — ошибка
- Шаг без действий — ошибка
- Действие не применимо к entity_type — ошибка
- Несколько ошибок одновременно — все в списке
- `validateOrThrow` бросает исключение с перечнем ошибок

---

### Задача 1.5: Расширить `Planner` для многошаговых планов

**Цель:** Planner должен строить планы с несколькими шагами.

**Файлы:**
- Изменить: `platform-core/src/main/java/com/zaborstik/platform/core/planner/Planner.java`
- Изменить: `platform-core/src/test/java/com/zaborstik/platform/core/planner/PlannerTest.java`

**Что делать:**
1. Добавить метод `Plan createMultiStepPlan(String target, String explanation, List<ExecutionRequest> requests)`:
   - Для каждого `ExecutionRequest` — проверить entity_type и action через Resolver
   - Создать `PlanStep` с `sortOrder = i + 1`
   - Объединить все шаги в один `Plan`
   - `stoppedAtPlanStepId` = id первого шага
2. Не ломать существующий `createPlan(ExecutionRequest)` — он остаётся для одношаговых
3. Валидировать: если список пуст — бросить `IllegalArgumentException`

**Тест:**
- Многошаговый план с 3 шагами — проверить sortOrder (1, 2, 3)
- Каждый шаг имеет правильный entity_type и action
- `stoppedAtPlanStepId` = id первого шага
- Пустой список → исключение
- Один из шагов с неприменимым action → исключение

---

### Задача 1.6: Добавить `PlanPathFinder`

**Цель:** Поиск последовательности действий для достижения цели через граф `action_applicable_entity_type`.

**Файлы:**
- Создать: `platform-core/src/main/java/com/zaborstik/platform/core/planner/PlanPathFinder.java`
- Создать: `platform-core/src/test/java/com/zaborstik/platform/core/planner/PlanPathFinderTest.java`

**Что делать:**
1. Конструктор `PlanPathFinder(Resolver resolver)`
2. Метод `List<Action> findApplicableActions(String entityTypeId)` — обёртка над `resolver.findActionsApplicableToEntityType`
3. Метод `List<PlanStepAction> buildActionSequence(List<String> entityTypeIds, List<String> actionIds)`:
   - Для каждой пары (entityType, action) — проверить применимость
   - Вернуть упорядоченный список `PlanStepAction`
   - Бросить исключение если action не применим
4. Метод `boolean canExecute(String actionId, String entityTypeId)` — обёртка над `resolver.isActionApplicable`

**Тест:**
- `findApplicableActions` для `ent-page` — возвращает `open_page`, `wait_element`, `read_text`, `take_screenshot`
- `findApplicableActions` для `ent-button` — `click`
- `buildActionSequence` для валидной последовательности — OK
- `buildActionSequence` с невалидной парой — исключение
- `canExecute` true/false

---

### Задача 1.7: Добавить `PlanUpdateRequest` record

**Цель:** DTO для обновления состояния плана.

**Файлы:**
- Создать: `platform-core/src/main/java/com/zaborstik/platform/core/plan/PlanUpdateRequest.java`
- Создать: `platform-core/src/test/java/com/zaborstik/platform/core/plan/PlanUpdateRequestTest.java`

**Что делать:**
1. Создать record `PlanUpdateRequest(String planId, String newWorkflowStepInternalName, String stoppedAtPlanStepId)`
2. `planId` — обязательный, not null
3. `newWorkflowStepInternalName` — обязательный, not null
4. `stoppedAtPlanStepId` — может быть null
5. Валидация в compact constructor

**Тест:**
- Успешное создание со всеми полями
- Успешное создание с null `stoppedAtPlanStepId`
- NPE при null `planId`
- NPE при null `newWorkflowStepInternalName`

---

## Порядок выполнения

```
1.1 WorkflowTransition record
  ↓
1.2 Resolver + InMemoryResolver (зависит от 1.1)
  ↓
1.3 LifecycleManager (зависит от 1.2)
  ↓
1.4 PlanValidator (независимый, можно параллельно с 1.3)
  ↓
1.5 Planner multi-step (независимый)
  ↓
1.6 PlanPathFinder (независимый)
  ↓
1.7 PlanUpdateRequest (независимый)
```

## Зависимости от других ботов

- **Нет входящих зависимостей.** Бот 1 работает полностью автономно.
- **Исходящие:** Бот 2 (`platform-api`) будет использовать `LifecycleManager`, `PlanValidator`, `PlanPathFinder`, `PlanUpdateRequest` — но только после того, как Бот 1 закончит.
