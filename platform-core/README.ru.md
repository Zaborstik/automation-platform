# Platform Core

Ядро execution-платформы. Содержит базовые абстракции и логику построения планов выполнения действий.

## Архитектура

### Доменные модели

- **EntityType** - тип сущности в системе (Building, Contract, Extract и т.д.)
- **Action** - атом системы, минимальная осмысленная операция (order_egrn_extract, close_contract и т.д.)
- **State** - состояние сущности или системы (для будущих preconditions/postconditions)
- **UIBinding** - привязка действия к UI-элементу (селекторы, метаданные)

### Plan DSL

План выполнения - это данные, не код:
- Хранится в БД
- Сериализуется
- Редактируется
- Генерируется ИИ

**Plan** содержит:
- Метаданные (entityType, entityId, actionId)
- Список шагов (PlanStep)
- Статус выполнения

**PlanStep** типы:
- `open_page` - открытие страницы
- `explain` - объяснение действия
- `hover` - наведение на элемент
- `click` - клик по элементу
- `wait` - ожидание результата
- `type` - ввод текста

### Компоненты

1. **Resolver** - находит EntityType, Action, UIBinding по идентификаторам
   - Интерфейс: `Resolver`
   - Реализация MVP: `InMemoryResolver`

2. **Planner** - строит линейные планы выполнения
   - Принимает `ExecutionRequest`
   - Использует `Resolver` для поиска компонентов
   - Возвращает `Plan` с шагами выполнения

3. **ExecutionEngine** - главный компонент ядра
   - Координирует работу Resolver и Planner
   - Создает планы выполнения

## Использование

```java
// 1. Создаем и настраиваем Resolver
InMemoryResolver resolver = new InMemoryResolver();

// Регистрируем метаданные
EntityType buildingType = new EntityType("Building", "Здание", Map.of());
resolver.registerEntityType(buildingType);

Action action = new Action(
    "order_egrn_extract",
    "Заказать выписку из ЕГРН",
    "Описание действия",
    Set.of("Building"),
    Map.of()
);
resolver.registerAction(action);

UIBinding uiBinding = new UIBinding(
    "order_egrn_extract",
    "[data-action='order_egrn_extract']",
    UIBinding.SelectorType.CSS,
    Map.of()
);
resolver.registerUIBinding(uiBinding);

// 2. Создаем Execution Engine
ExecutionEngine engine = new ExecutionEngine(resolver);

// 3. Создаем запрос
ExecutionRequest request = new ExecutionRequest(
    "Building",
    "93939",
    "order_egrn_extract",
    Map.of()
);

// 4. Создаем план
Plan plan = engine.createPlan(request);
```

## Пример плана

```json
{
  "id": "uuid",
  "entityTypeId": "Building",
  "entityId": "93939",
  "actionId": "order_egrn_extract",
  "status": "CREATED",
  "steps": [
    {
      "type": "open_page",
      "target": "/buildings/93939",
      "explanation": "Открываю карточку Здание #93939"
    },
    {
      "type": "explain",
      "explanation": "Заказывает выписку из ЕГРН для здания"
    },
    {
      "type": "hover",
      "target": "action(order_egrn_extract)",
      "explanation": "Навожу курсор на элемент действия"
    },
    {
      "type": "click",
      "target": "action(order_egrn_extract)",
      "explanation": "Выполняю действие"
    },
    {
      "type": "wait",
      "target": "result",
      "explanation": "Ожидаю завершения действия"
    }
  ]
}
```

## Принципы

1. **UI - источник истины, но не модель**
   - Исполняем через UI
   - Моделируем через доменные абстракции

2. **Платформа не знает предметную область**
   - Любая предметка - это данные + метаданные, а не код

3. **Действие - атом системы**
   - Action - минимальная осмысленная операция
   - UI - лишь проекция действия

## Эволюция

**Этап 1 (MVP)** - текущая реализация:
- Линейные планы
- Ручная регистрация действий
- UI-first подход

**Этап 2 (будущее)** - Reasoning over Actions:
- Preconditions/postconditions
- Проверки допустимости
- State transitions

**Этап 3 (будущее)** - Configuration Engine:
- Правила и политики
- Ограничения
- Симуляции

