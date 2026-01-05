# Инструкции по компиляции и тестированию

Данный документ описывает как скомпилировать и протестировать все компоненты execution-платформы.

## Содержание

1. [Требования](#требования)
2. [Компиляция проекта](#компиляция-проекта)
3. [Тестирование компонентов](#тестирование-компонентов)
   - [Тестирование ядра (platform-core)](#тестирование-ядра-platform-core)
   - [Тестирование API (platform-api)](#тестирование-api-platform-api)
   - [Тестирование агента (platform-agent)](#тестирование-агента-platform-agent)
   - [Тестирование executor (platform-executor)](#тестирование-executor-platform-executor)
4. [Минимальная проверка работы](#минимальная-проверка-работы)
5. [Интеграционное тестирование](#интеграционное-тестирование)

---

## Требования

### Обязательные

- **Java 21** (JDK 21)
- **Maven 3.6+**
- **Node.js 18+** (для Playwright сервера)
- **npm** или **yarn**

### Проверка установки

```bash
# Проверка Java
java -version
# Должно быть: openjdk version "21" или выше

# Проверка Maven
mvn -version
# Должно быть: Apache Maven 3.6.x или выше

# Проверка Node.js
node -v
# Должно быть: v18.x.x или выше

npm -v
# Должно быть: 9.x.x или выше
```

---

## Компиляция проекта

### Компиляция всего проекта

Из корневой директории проекта:

```bash
# Очистка и компиляция всех модулей
mvn clean compile

# Компиляция с пропуском тестов (быстрее)
mvn clean compile -DskipTests

# Полная сборка (компиляция + тесты + упаковка)
mvn clean package

# Установка в локальный Maven репозиторий
mvn clean install
```

### Компиляция отдельных модулей

```bash
# Компиляция только ядра
cd platform-core
mvn clean compile

# Компиляция только API
cd platform-api
mvn clean compile

# Компиляция только агента
cd platform-agent
mvn clean compile

# Компиляция только executor
cd platform-executor
mvn clean compile
```

### Сборка JAR файлов

```bash
# Сборка всего проекта
mvn clean package

# JAR файлы будут в:
# - platform-api/target/platform-api-1.0-SNAPSHOT.jar
# - platform-agent/target/platform-agent-1.0-SNAPSHOT.jar
# - platform-executor/target/platform-executor-1.0-SNAPSHOT.jar
```

---

## Тестирование компонентов

### Тестирование ядра (platform-core)

**Ядро** содержит базовые абстракции и логику построения планов. Это самый важный компонент для тестирования.

#### Запуск всех тестов ядра

```bash
cd platform-core
mvn test
```

#### Запуск конкретного теста

```bash
# Тест ExecutionEngine
mvn test -Dtest=ExecutionEngineTest

# Конкретный метод теста
mvn test -Dtest=ExecutionEngineTest#shouldCreatePlanSuccessfully
```

#### Что тестируется

- ✅ `ExecutionEngine` - создание планов выполнения
- ✅ `Planner` - построение линейных планов
- ✅ `InMemoryResolver` - поиск EntityType, Action, UIBinding
- ✅ Доменные модели (`EntityType`, `Action`, `UIBinding`, `Plan`, `PlanStep`)
- ✅ Валидация и обработка ошибок

#### Примеры тестов

```bash
# Тест создания плана
mvn test -Dtest=ExecutionEngineTest#shouldCreatePlanSuccessfully

# Тест обработки ошибок
mvn test -Dtest=ExecutionEngineTest#shouldThrowExceptionWhenEntityTypeNotFound

# Тест структуры плана
mvn test -Dtest=ExecutionEngineTest#shouldCreatePlanWithCorrectSteps
```

#### Запуск примера использования

```bash
cd platform-core
mvn exec:java -Dexec.mainClass="org.example.core.example.ExampleUsage"
```

Этот пример:
1. Регистрирует EntityType, Action, UIBinding
2. Создаёт ExecutionRequest
3. Строит Plan через ExecutionEngine
4. Выводит план в консоль

---

### Тестирование API (platform-api)

**API** предоставляет REST интерфейс для работы с execution engine.

#### Запуск всех тестов API

```bash
cd platform-api
mvn test
```

#### Типы тестов в API

1. **Unit тесты** (быстрые, изолированные):
   ```bash
   # Тест контроллера
   mvn test -Dtest=ExecutionControllerTest
   
   # Тест сервиса
   mvn test -Dtest=ExecutionServiceTest
   
   # Тест обработчика ошибок
   mvn test -Dtest=GlobalExceptionHandlerTest
   
   # Тест конфигурации
   mvn test -Dtest=PlatformConfigurationTest
   
   # Тест сериализации DTO
   mvn test -Dtest=DTOsSerializationTest
   ```

2. **Интеграционные тесты** (полный стек):
   ```bash
   mvn test -Dtest=ExecutionIntegrationTest
   ```

#### Что тестируется

- ✅ REST API эндпоинты (`POST /api/execution/plan`)
- ✅ Валидация входных данных
- ✅ Обработка ошибок
- ✅ Преобразование DTO ↔ доменные объекты
- ✅ Конфигурация Spring (Resolver, ExecutionEngine)
- ✅ Полный цикл создания плана через API

#### Запуск API сервера для ручного тестирования

```bash
cd platform-api
mvn spring-boot:run
```

API будет доступен на `http://localhost:8080`

**Тестирование через cURL:**

```bash
# Создание плана
curl -X POST http://localhost:8080/api/execution/plan \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "Building",
    "entityId": "93939",
    "action": "order_egrn_extract",
    "parameters": {}
  }'
```

**Ожидаемый ответ:**

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
    ...
  ]
}
```

---

### Тестирование агента (platform-agent)

**Агент** выполняет планы через UI с использованием Playwright.

#### Подготовка Playwright сервера

Перед тестированием агента необходимо установить зависимости и запустить Playwright сервер:

```bash
cd platform-agent/src/main/resources

# Установка зависимостей Node.js
npm install

# Запуск Playwright сервера (в отдельном терминале)
node playwright-server.js

# Или в headless режиме
node playwright-server.js --headless

# С кастомным портом
PORT=3001 node playwright-server.js
```

Сервер будет доступен на `http://localhost:3000` (или указанном порте).

**Проверка доступности сервера:**

```bash
curl http://localhost:3000/health
```

Ожидаемый ответ:
```json
{"status":"ok","browser":false}
```

#### Запуск тестов агента

```bash
cd platform-agent
mvn test
```

**Примечание:** Тесты агента требуют запущенного Playwright сервера. Если сервер не запущен, тесты могут падать.

#### Запуск примера использования агента

```bash
cd platform-agent

# Убедитесь, что Playwright сервер запущен на localhost:3000
# Затем запустите пример:
mvn exec:java -Dexec.mainClass="org.example.agent.example.AgentExample"
```

Этот пример:
1. Настраивает Resolver с тестовыми данными
2. Создаёт план через ExecutionEngine
3. Инициализирует браузер через AgentService
4. Выполняет план шаг за шагом
5. Выводит результаты выполнения

**Важно:** Для работы примера нужен запущенный Playwright сервер и доступное веб-приложение на `http://localhost:8080` (или измените URL в коде).

---

### Тестирование executor (platform-executor)

**Executor** оркестрирует выполнение планов и собирает execution log.

#### Запуск тестов executor

```bash
cd platform-executor
mvn test
```

**Примечание:** Executor зависит от platform-agent, поэтому для полного тестирования может потребоваться запущенный Playwright сервер.

#### Запуск примера использования executor

```bash
cd platform-executor

# Убедитесь, что Playwright сервер запущен
# Затем запустите пример:
mvn exec:java -Dexec.mainClass="org.example.Main"
```

Этот пример:
1. Создаёт план через ExecutionEngine
2. Настраивает AgentService
3. Выполняет план через PlanExecutor
4. Выводит execution log с результатами

---

## Минимальная проверка работы

### Быстрая проверка (без UI)

Этот способ проверяет работу ядра и API без необходимости запуска браузера.

#### Шаг 1: Компиляция проекта

```bash
# Из корня проекта
mvn clean compile
```

#### Шаг 2: Запуск тестов ядра

```bash
cd platform-core
mvn test
```

**Ожидаемый результат:** Все тесты должны пройти успешно.

#### Шаг 3: Запуск тестов API

```bash
cd platform-api
mvn test
```

**Ожидаемый результат:** Все тесты должны пройти успешно.

#### Шаг 4: Запуск API и проверка через REST

```bash
# Терминал 1: Запуск API
cd platform-api
mvn spring-boot:run

# Терминал 2: Проверка API
curl -X POST http://localhost:8080/api/execution/plan \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "Building",
    "entityId": "93939",
    "action": "order_egrn_extract",
    "parameters": {}
  }'
```

**Ожидаемый результат:** JSON с планом выполнения (5 шагов).

---

### Полная проверка (с UI)

Этот способ проверяет весь стек, включая выполнение через браузер.

#### Шаг 1: Установка зависимостей Playwright

```bash
cd platform-agent/src/main/resources
npm install
```

#### Шаг 2: Запуск Playwright сервера

```bash
# Терминал 1: Запуск Playwright сервера
cd platform-agent/src/main/resources
node playwright-server.js
```

#### Шаг 3: Запуск API

```bash
# Терминал 2: Запуск API
cd platform-api
mvn spring-boot:run
```

#### Шаг 4: Запуск примера агента

```bash
# Терминал 3: Запуск примера агента
cd platform-agent
mvn exec:java -Dexec.mainClass="org.example.agent.example.AgentExample"
```

**Ожидаемый результат:**
- Браузер откроется (если не headless)
- План будет выполнен шаг за шагом
- В консоли будут выведены результаты каждого шага

---

## Интеграционное тестирование

### Полный цикл: API → Engine → Executor → Agent

#### Подготовка

1. **Запустите Playwright сервер:**
   ```bash
   cd platform-agent/src/main/resources
   node playwright-server.js
   ```

2. **Запустите API:**
   ```bash
   cd platform-api
   mvn spring-boot:run
   ```

#### Тестирование через API

```bash
# 1. Создание плана через API
curl -X POST http://localhost:8080/api/execution/plan \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "Building",
    "entityId": "93939",
    "action": "order_egrn_extract",
    "parameters": {}
  }' > plan.json

# 2. Проверка структуры плана
cat plan.json | jq '.steps | length'
# Должно быть: 5

cat plan.json | jq '.steps[].type'
# Должно быть: ["open_page", "explain", "hover", "click", "wait"]
```

#### Тестирование выполнения плана

Для выполнения плана через executor можно использовать пример из `platform-executor` или написать свой тест.

---

## Устранение проблем

### Проблема: Maven не находит зависимости

**Решение:**
```bash
# Очистка и пересборка
mvn clean install -DskipTests
```

### Проблема: Тесты падают с ошибкой "EntityType not found"

**Причина:** Не настроен Resolver с тестовыми данными.

**Решение:** Убедитесь, что тесты используют `PlatformConfiguration` или настраивают `InMemoryResolver` вручную.

### Проблема: Playwright сервер не запускается

**Причины:**
- Не установлены зависимости: `npm install` в `platform-agent/src/main/resources`
- Порт занят: измените порт через `PORT=3001 node playwright-server.js`
- Node.js версия слишком старая: требуется Node.js 18+

**Решение:**
```bash
cd platform-agent/src/main/resources
npm install
node playwright-server.js
```

### Проблема: Браузер не открывается

**Причина:** Playwright требует установки браузеров.

**Решение:**
```bash
cd platform-agent/src/main/resources
npx playwright install chromium
```

### Проблема: API не запускается

**Причины:**
- Порт 8080 занят: измените `server.port` в `application.properties`
- Java версия неправильная: требуется Java 21

**Решение:**
```bash
# Проверка Java версии
java -version

# Изменение порта в application.properties
# server.port=8081
```

---

## Полезные команды

### Просмотр структуры проекта

```bash
# Дерево модулей
mvn dependency:tree

# Список всех тестов
find . -name "*Test.java" -type f
```

### Очистка проекта

```bash
# Очистка всех target директорий
mvn clean

# Очистка + удаление установленных артефактов
mvn clean install -DskipTests
```

### Запуск с подробным выводом

```bash
# Подробный вывод Maven
mvn test -X

# Вывод только ошибок
mvn test -q
```

### Покрытие кода (требует настройки JaCoCo)

```bash
# Добавьте плагин JaCoCo в pom.xml, затем:
mvn test jacoco:report
# Отчет будет в target/site/jacoco/index.html
```

---

## Резюме

### Минимальная проверка работы (5 минут)

```bash
# 1. Компиляция
mvn clean compile

# 2. Тесты ядра
cd platform-core && mvn test && cd ..

# 3. Тесты API
cd platform-api && mvn test && cd ..
```

### Полная проверка (15 минут)

```bash
# 1. Компиляция
mvn clean package

# 2. Установка Playwright зависимостей
cd platform-agent/src/main/resources && npm install && cd ../../../../..

# 3. Запуск Playwright сервера (в отдельном терминале)
cd platform-agent/src/main/resources
node playwright-server.js

# 4. Запуск API (в отдельном терминале)
cd platform-api
mvn spring-boot:run

# 5. Тестирование через cURL или примеры
```

---

## Дополнительные ресурсы

- [README platform-core](../platform-core/README.md) - документация ядра
- [README platform-api](../platform-api/README.md) - документация API
- [README platform-agent](../platform-agent/README.md) - документация агента
- [README platform-executor](../platform-executor/README.md) - документация executor
- [TESTING.md](../platform-api/TESTING.md) - подробная документация по тестам API

