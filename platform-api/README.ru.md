# Platform API

REST API для универсальной execution-платформы. Предоставляет эндпоинты для создания планов выполнения действий через UI.

## Архитектура

API построен на Spring Boot и предоставляет REST интерфейс для работы с execution engine:

```
[ Client ] 
    |
    v
[ REST API (Spring Boot) ]
    |
    v
[ ExecutionService ]
    |
    v
[ ExecutionEngine (platform-core) ]
    |
    v
[ Planner + Resolver ]
```

## Запуск приложения

```bash
mvn spring-boot:run
```

Или соберите и запустите JAR:

```bash
mvn clean package
java -jar target/platform-api-1.0-SNAPSHOT.jar
```

Приложение запустится на порту `8080` (настраивается в `application.properties`).

## API Эндпоинты

### POST /api/execution/plan

Создает план выполнения для указанного действия.

**Запрос:**
```json
{
  "entity": "Building",
  "entityId": "93939",
  "action": "order_egrn_extract",
  "parameters": {}
}
```

**Ответ (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "entityType": "Building",
  "entityId": "93939",
  "action": "order_egrn_extract",
  "status": "CREATED",
  "steps": [
    {
      "type": "open_page",
      "target": "/buildings/93939",
      "explanation": "Открываю карточку Здание #93939",
      "parameters": {}
    },
    {
      "type": "explain",
      "target": null,
      "explanation": "Заказывает выписку из ЕГРН для указанного здания",
      "parameters": {}
    },
    {
      "type": "hover",
      "target": "action(order_egrn_extract)",
      "explanation": "Навожу курсор на элемент действия 'Заказать выписку из ЕГРН'",
      "parameters": {}
    },
    {
      "type": "click",
      "target": "action(order_egrn_extract)",
      "explanation": "Выполняю действие 'Заказать выписку из ЕГРН'",
      "parameters": {}
    },
    {
      "type": "wait",
      "target": "result",
      "explanation": "Ожидаю завершения действия 'Заказать выписку из ЕГРН'",
      "parameters": {}
    }
  ]
}
```

**Ошибки:**

- `400 Bad Request` - невалидные входные данные или не найдены EntityType/Action/UIBinding
- `500 Internal Server Error` - внутренняя ошибка сервера

### GET /api/execution/plan/{id}

Получает план по идентификатору.

**Статус:** `501 Not Implemented` (планы пока не сохраняются)

## Примеры использования

### cURL

```bash
curl -X POST http://localhost:8080/api/execution/plan \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "Building",
    "entityId": "93939",
    "action": "order_egrn_extract",
    "parameters": {}
  }'
```

### JavaScript (fetch)

```javascript
const response = await fetch('http://localhost:8080/api/execution/plan', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    entity: 'Building',
    entityId: '93939',
    action: 'order_egrn_extract',
    parameters: {}
  })
});

const plan = await response.json();
console.log('Plan created:', plan);
```

## Предустановленные данные

В MVP используются предустановленные EntityType, Action и UIBinding:

### EntityType
- `Building` - Здание
- `Contract` - Договор

### Actions
- `order_egrn_extract` - Заказать выписку из ЕГРН (применимо к Building)
- `close_contract` - Закрыть договор (применимо к Contract)
- `assign_owner` - Назначить владельца (применимо к Building)

### UIBindings
Каждое действие имеет привязку к UI-элементу через CSS/XPath селекторы.

## Структура проекта

```
platform-api/
├── src/main/java/org/example/api/
│   ├── PlatformApiApplication.java    # Главный класс Spring Boot
│   ├── controller/
│   │   └── ExecutionController.java   # REST контроллер
│   ├── service/
│   │   └── ExecutionService.java      # Сервисный слой
│   ├── dto/
│   │   ├── ExecutionRequestDTO.java   # DTO для запроса
│   │   ├── PlanDTO.java              # DTO для плана
│   │   ├── PlanStepDTO.java          # DTO для шага плана
│   │   └── ErrorResponseDTO.java     # DTO для ошибок
│   ├── config/
│   │   └── PlatformConfiguration.java # Конфигурация Spring
│   └── exception/
│       └── GlobalExceptionHandler.java # Обработчик исключений
└── src/main/resources/
    └── application.properties         # Конфигурация приложения
```

## Расширение функциональности

### Добавление новых EntityType/Action/UIBinding

Отредактируйте `PlatformConfiguration.java` и добавьте регистрацию новых компонентов в методы:
- `registerExampleEntityTypes()`
- `registerExampleActions()`
- `registerExampleUIBindings()`

### Интеграция с БД

В будущем `InMemoryResolver` может быть заменен на репозиторий с БД. Для этого:
1. Создайте интерфейсы репозиториев (JPA/Spring Data)
2. Реализуйте `Resolver` через репозитории
3. Замените бин в `PlatformConfiguration`

### Хранение планов

Для реализации `GET /api/execution/plan/{id}`:
1. Добавьте репозиторий для `Plan`
2. Сохраняйте планы в `ExecutionService.createPlan()`
3. Реализуйте получение в контроллере

## Зависимости

- Spring Boot 3.2.1
- Spring Web
- Spring Validation
- Jackson (для JSON)
- platform-core (ядро платформы)

