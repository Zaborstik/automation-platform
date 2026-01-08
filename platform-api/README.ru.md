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
[ DatabaseResolver (JPA) ]
    |
    v
[ PostgreSQL / H2 Database ]
```

## Персистентность данных

Платформа использует JPA (Hibernate) для работы с базой данных:

- **Development**: H2 in-memory database
- **Production**: PostgreSQL

Все данные сохраняются в БД:
- EntityType, Action, UIBinding (метаданные)
- Plans (планы выполнения)
- PlanSteps (шаги планов)
- ExecutionResults (результаты выполнения)
- ExecutionLogEntries (записи лога выполнения)

### Миграции базы данных

Используется Flyway для управления миграциями:
- `V1__Create_base_tables.sql` - создание всех таблиц
- `V2__Insert_initial_data.sql` - начальные данные

## Запуск приложения

### Development (H2)

```bash
mvn spring-boot:run
# или
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Приложение запустится на порту `8080` с H2 in-memory БД.
H2 Console доступен по адресу: http://localhost:8080/h2-console

### Production (PostgreSQL)

```bash
# Установите переменные окружения
export DATABASE_URL=jdbc:postgresql://localhost:5432/platformdb
export DATABASE_USERNAME=platform
export DATABASE_PASSWORD=platform

# Запустите с профилем prod
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

Или соберите и запустите JAR:

```bash
mvn clean package
java -jar target/platform-api-1.0-SNAPSHOT.jar
```

## API Эндпоинты

### POST /api/execution/plan

Создает план выполнения для указанного действия и сохраняет его в БД.

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

Получает план по идентификатору из БД.

**Ответ (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "entityType": "Building",
  "entityId": "93939",
  "action": "order_egrn_extract",
  "status": "CREATED",
  "steps": [...]
}
```

**Ошибки:**

- `404 Not Found` - план с указанным ID не найден

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

## Структура базы данных

### Основные таблицы

- **entity_types** - типы сущностей (Building, Contract и т.д.)
- **actions** - действия (order_egrn_extract, close_contract и т.д.)
- **ui_bindings** - привязки действий к UI-элементам
- **plans** - планы выполнения
- **plan_steps** - шаги планов
- **execution_results** - результаты выполнения планов
- **execution_log_entries** - записи лога выполнения

### Связи

- `actions` → `action_applicable_entity_types` (многие-ко-многим с entity_types)
- `plans` → `plan_steps` (один-ко-многим)
- `execution_results` → `execution_log_entries` (один-ко-многим)

## Предустановленные данные

При первом запуске Flyway миграции создают начальные данные:

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
├── src/main/java/com/zaborstik/platform/api/
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
│   ├── entity/
│   │   ├── EntityTypeEntity.java     # JPA Entity для EntityType
│   │   ├── ActionEntity.java         # JPA Entity для Action
│   │   ├── UIBindingEntity.java      # JPA Entity для UIBinding
│   │   ├── PlanEntity.java          # JPA Entity для Plan
│   │   ├── PlanStepEntity.java       # JPA Entity для PlanStep
│   │   ├── ExecutionResultEntity.java # JPA Entity для ExecutionResult
│   │   └── ExecutionLogEntryEntity.java # JPA Entity для ExecutionLogEntry
│   ├── repository/
│   │   ├── EntityTypeRepository.java  # Spring Data JPA репозиторий
│   │   ├── ActionRepository.java      # Spring Data JPA репозиторий
│   │   ├── UIBindingRepository.java   # Spring Data JPA репозиторий
│   │   ├── PlanRepository.java        # Spring Data JPA репозиторий
│   │   └── ExecutionResultRepository.java # Spring Data JPA репозиторий
│   ├── resolver/
│   │   └── DatabaseResolver.java      # Реализация Resolver через БД
│   ├── mapper/
│   │   └── PlanMapper.java           # Маппер Plan ↔ PlanEntity
│   ├── config/
│   │   └── PlatformConfiguration.java # Конфигурация Spring
│   └── exception/
│       └── GlobalExceptionHandler.java # Обработчик исключений
└── src/main/resources/
    ├── application.properties         # Основная конфигурация
    ├── application-dev.properties     # Конфигурация для dev (H2)
    ├── application-prod.properties    # Конфигурация для prod (PostgreSQL)
    └── db/migration/
        ├── V1__Create_base_tables.sql # Миграция создания таблиц
        └── V2__Insert_initial_data.sql # Миграция начальных данных
```

## Тестирование

### Запуск всех тестов

```bash
mvn test
```

### Типы тестов

- **Unit тесты** - изолированные тесты компонентов с моками
- **Интеграционные тесты** - тесты с реальной БД (H2 in-memory)
- **Repository тесты** - тесты репозиториев с @DataJpaTest
- **Controller тесты** - тесты REST API с MockMvc

### Тестовые профили

Тесты используют H2 in-memory БД автоматически через `@DataJpaTest` или `@SpringBootTest`.

## Расширение функциональности

### Добавление новых EntityType/Action/UIBinding

1. Добавьте данные через SQL миграцию Flyway
2. Или используйте репозитории программно:

```java
@Autowired
private EntityTypeRepository entityTypeRepository;

EntityTypeEntity entity = new EntityTypeEntity("NewType", "Новый тип", Map.of());
entityTypeRepository.save(entity);
```

### Настройка подключения к БД

Отредактируйте `application-prod.properties` или установите переменные окружения:
- `DATABASE_URL` - URL подключения к PostgreSQL
- `DATABASE_USERNAME` - имя пользователя
- `DATABASE_PASSWORD` - пароль

### Создание новых миграций

1. Создайте файл в `src/main/resources/db/migration/`
2. Имя файла: `V{номер}__{описание}.sql`
3. Пример: `V3__Add_new_table.sql`

## Зависимости

- Spring Boot 3.2.1
- Spring Web
- Spring Data JPA
- Spring Validation
- Hibernate (JPA implementation)
- H2 Database (для development)
- PostgreSQL Driver (для production)
- Flyway (для миграций)
- Jackson (для JSON)
- platform-core (ядро платформы)

## Мониторинг и отладка

### H2 Console (только для dev)

При запуске с профилем `dev`, H2 Console доступен по адресу:
http://localhost:8080/h2-console

Настройки подключения:
- JDBC URL: `jdbc:h2:mem:platformdb`
- User Name: `sa`
- Password: (пусто)

### Логирование SQL

В dev профиле SQL запросы логируются автоматически. Для prod отключите:
```properties
spring.jpa.show-sql=false
```

## Production Deployment

### Требования

- PostgreSQL 12+
- Java 21+
- Минимум 512MB RAM

### Шаги развертывания

1. Создайте БД PostgreSQL:
   ```sql
   CREATE DATABASE platformdb;
   CREATE USER platform WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE platformdb TO platform;
   ```

2. Установите переменные окружения:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export DATABASE_URL=jdbc:postgresql://localhost:5432/platformdb
   export DATABASE_USERNAME=platform
   export DATABASE_PASSWORD=your_password
   ```

3. Запустите приложение:
   ```bash
   java -jar platform-api-1.0-SNAPSHOT.jar
   ```

4. Flyway автоматически применит миграции при первом запуске.

## Troubleshooting

### Проблема: Миграции не применяются

**Решение:** Проверьте, что Flyway включен в `application.properties`:
```properties
spring.flyway.enabled=true
```

### Проблема: Ошибка подключения к PostgreSQL

**Решение:** Проверьте:
- PostgreSQL запущен
- Правильность URL, username, password
- Доступность БД из сети (если не localhost)

### Проблема: H2 Console не открывается

**Решение:** Убедитесь, что используете профиль `dev`:
```properties
spring.profiles.active=dev
spring.h2.console.enabled=true
```
