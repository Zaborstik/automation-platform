# BOT 4: platform-knowledge — Intelligence Layer

## Scope

**Module:** `platform-knowledge/src/` only  
**Package base:** `com.zaborstik.platform.knowledge`  
**Зависимости:** `platform-core` (read-only, только использует интерфейсы `Resolver`, records из `core/domain/` и `core/plan/`)  
**Запрещено трогать:** любые файлы вне `platform-knowledge/`

## Текущее состояние модуля

### Что уже есть:

| Файл | Описание |
|------|----------|
| `platform-knowledge/pom.xml` | Минимальный POM **без parent**, без зависимостей |
| `platform-knowledge/src/main/java/com/zaborstik/platform/Main.java` | Заглушка "Hello and welcome!" |

**Тестов:** 0  
**Полезного кода:** 0  
**Модуль фактически пустой.**

---

## Что нужно сделать

### Задача 4.1: Настроить модуль `platform-knowledge`

**Цель:** Корректная структура Maven модуля, зависимости, пакеты.

**Файлы:**
- Изменить: `platform-knowledge/pom.xml`
- Удалить: `platform-knowledge/src/main/java/com/zaborstik/platform/Main.java`
- Создать пакеты (пустые, заполнятся дальше):
  - `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/model/`
  - `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/service/`
  - `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/llm/`
  - `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/scanner/`
  - `platform-knowledge/src/main/resources/prompts/`
  - `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/`

**Что делать:**
1. Обновить `pom.xml`:
   ```xml
   <parent>
       <groupId>com.zaborstik.corporation</groupId>
       <artifactId>automation-platform</artifactId>
       <version>0.1.0-SNAPSHOT</version>
   </parent>

   <artifactId>platform-knowledge</artifactId>
   <version>1.0-SNAPSHOT</version>
   <name>platform-knowledge</name>

   <dependencies>
       <dependency>
           <groupId>com.zaborstik.corporation</groupId>
           <artifactId>platform-core</artifactId>
           <version>1.0-SNAPSHOT</version>
       </dependency>
       <dependency>
           <groupId>com.fasterxml.jackson.core</groupId>
           <artifactId>jackson-databind</artifactId>
           <version>2.16.1</version>
       </dependency>
       <dependency>
           <groupId>org.jsoup</groupId>
           <artifactId>jsoup</artifactId>
           <version>1.17.2</version>
       </dependency>
       <dependency>
           <groupId>org.slf4j</groupId>
           <artifactId>slf4j-api</artifactId>
       </dependency>
       <dependency>
           <groupId>ch.qos.logback</groupId>
           <artifactId>logback-classic</artifactId>
       </dependency>
       <dependency>
           <groupId>org.junit.jupiter</groupId>
           <artifactId>junit-jupiter</artifactId>
           <version>5.10.1</version>
           <scope>test</scope>
       </dependency>
   </dependencies>
   ```
2. Удалить `Main.java`
3. Создать `logback.xml` в resources (скопировать формат из `platform-core`)
4. Проверить `mvn compile` — должен пройти

**Тест:** `mvn compile -pl platform-knowledge` — без ошибок.

---

### Задача 4.2: Добавить доменную модель базы знаний

**Цель:** Records для хранения знаний о целевом приложении.

**Файлы:**
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/model/AppKnowledge.java`
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/model/PageKnowledge.java`
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/model/UIElement.java`
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/model/ParsedUserRequest.java`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/model/AppKnowledgeTest.java`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/model/PageKnowledgeTest.java`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/model/UIElementTest.java`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/model/ParsedUserRequestTest.java`

**Что делать:**
1. `AppKnowledge`:
   ```java
   public record AppKnowledge(
       String appId,          // UUID
       String appName,        // "DuckDuckGo"
       String baseUrl,        // "https://duckduckgo.com"
       List<PageKnowledge> pages,
       Instant discoveredAt
   )
   ```
   - `appId` not null, `appName` not null, `baseUrl` not null
   - `pages` — immutable copy, default empty

2. `PageKnowledge`:
   ```java
   public record PageKnowledge(
       String pageUrl,        // "/search"
       String pageTitle,      // "Search Results"
       List<UIElement> elements,
       Instant scannedAt
   )
   ```
   - `pageUrl` not null
   - `elements` — immutable copy

3. `UIElement`:
   ```java
   public record UIElement(
       String selector,       // "input#search_form_input"
       String selectorType,   // "CSS" / "XPATH" / "TEXT"
       String elementType,    // maps to entity_type: "input", "button", "link", "form", "table", "page"
       String label,          // видимый текст / placeholder / aria-label
       Map<String, String> attributes  // дополнительные атрибуты (name, type, role, etc.)
   )
   ```
   - `selector` not null, `elementType` not null
   - `attributes` — immutable copy

4. `ParsedUserRequest`:
   ```java
   public record ParsedUserRequest(
       String rawInput,           // исходный текст пользователя
       String entityTypeId,       // определённый тип сущности
       List<String> actionIds,    // определённые действия
       Map<String, String> parameters,  // параметры (meta_value, target, etc.)
       boolean clarificationNeeded,     // нужно ли уточнение
       String clarificationQuestion     // вопрос для уточнения (если нужно)
   )
   ```
   - `rawInput` not null
   - `actionIds` — immutable copy
   - `parameters` — immutable copy

**Тесты для каждого record:**
- Создание с валидными данными
- NPE при null обязательных полей
- Immutability коллекций (попытка модификации → UnsupportedOperationException)
- toString содержит ключевые поля

---

### Задача 4.3: Добавить `KnowledgeRepository` (in-memory)

**Цель:** Хранилище базы знаний о приложениях.

**Файлы:**
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/service/KnowledgeRepository.java`
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/service/InMemoryKnowledgeRepository.java`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/service/InMemoryKnowledgeRepositoryTest.java`

**Что делать:**
1. Интерфейс `KnowledgeRepository`:
   ```java
   void save(AppKnowledge knowledge);
   Optional<AppKnowledge> findByAppId(String appId);
   Optional<AppKnowledge> findByBaseUrl(String baseUrl);
   List<AppKnowledge> listAll();
   void deleteByAppId(String appId);
   ```
2. `InMemoryKnowledgeRepository`:
   - `Map<String, AppKnowledge>` по appId
   - Индекс `Map<String, String>` по baseUrl → appId для быстрого поиска
   - Thread-safe (ConcurrentHashMap)

**Тест:**
- save + findByAppId — находит
- save + findByBaseUrl — находит
- listAll — возвращает все
- deleteByAppId — удаляет
- findByAppId для несуществующего — empty
- Несколько приложений — все сохраняются

---

### Задача 4.4: Добавить `LLMClient` интерфейс и заглушку

**Цель:** Абстракция LLM клиента для тестирования без реального API.

**Файлы:**
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/llm/LLMClient.java`
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/llm/LLMResponse.java`
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/llm/StubLLMClient.java`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/llm/StubLLMClientTest.java`

**Что делать:**
1. Интерфейс `LLMClient`:
   ```java
   LLMResponse complete(String systemPrompt, String userMessage);
   LLMResponse complete(String prompt);
   boolean isAvailable();
   ```
2. Record `LLMResponse(String content, boolean success, String error, long latencyMs)`
3. `StubLLMClient`:
   - Конструктор с `Map<String, String> responses` — маппинг входных фраз → ответы
   - Метод `addResponse(String inputContains, String response)` — добавить маппинг
   - `complete()`: ищет первый ключ, который содержится в prompt, и возвращает ответ
   - Если ничего не нашлось → дефолтный ответ (задаётся в конструкторе)
   - `isAvailable()` — всегда true

**Тест:**
- Добавить маппинг "поиск" → JSON ответ, отправить prompt "выполни поиск" → правильный ответ
- Нет маппинга → дефолтный ответ
- `isAvailable()` → true
- `complete` возвращает LLMResponse с `success=true`

---

### Задача 4.5: Добавить `PromptTemplate` утилиту

**Цель:** Загрузка и заполнение шаблонов промптов из ресурсов.

**Файлы:**
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/llm/PromptTemplate.java`
- Создать: `platform-knowledge/src/main/resources/prompts/parse-user-request.txt`
- Создать: `platform-knowledge/src/main/resources/prompts/generate-plan-steps.txt`
- Создать: `platform-knowledge/src/main/resources/prompts/clarify-request.txt`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/llm/PromptTemplateTest.java`

**Что делать:**
1. Класс `PromptTemplate`:
   ```java
   static String load(String resourcePath)  // загрузить шаблон из classpath
   static String fill(String template, Map<String, String> variables)  // заменить {{key}} на value
   static String loadAndFill(String resourcePath, Map<String, String> variables)
   ```
2. Шаблон `parse-user-request.txt`:
   ```
   Ты — ассистент платформы автоматизации.
   
   Доступные типы сущностей: {{entity_types}}
   Доступные действия: {{actions}}
   
   Пользователь просит: {{user_input}}
   
   Определи:
   1. Тип сущности (entity_type_id) из списка
   2. Нужные действия (action_id) из списка
   3. Параметры (meta_value, target)
   4. Нужно ли уточнение
   
   Ответь в JSON формате:
   {"entityTypeId": "...", "actionIds": [...], "parameters": {...}, "clarificationNeeded": false, "clarificationQuestion": null}
   ```
3. Шаблон `generate-plan-steps.txt`:
   ```
   Ты — планировщик автоматизации.
   
   Задача пользователя: {{user_task}}
   Доступные действия для типа {{entity_type}}: {{applicable_actions}}
   Текущая база знаний: {{knowledge}}
   
   Построй последовательность шагов (plan_steps) для выполнения задачи.
   Каждый шаг:
   - entity_type_id
   - action_id
   - meta_value (если нужен)
   - display_name (описание шага)
   
   Ответь в JSON: {"steps": [{...}, ...]}
   ```
4. Шаблон `clarify-request.txt`:
   ```
   Пользователь просит: {{user_input}}
   
   Контекст: {{context}}
   
   Задай один уточняющий вопрос, чтобы понять, что именно нужно сделать.
   Ответь только текстом вопроса, без JSON.
   ```

**Тест:**
- `load` загружает файл, не null, не пустой
- `fill` заменяет `{{key}}` на значение
- `fill` оставляет `{{unknown}}` как есть (или бросает исключение — выбрать поведение)
- `loadAndFill` — комбинация
- Несколько переменных — все заменяются

---

### Задача 4.6: Добавить `UserRequestParser`

**Цель:** Парсинг пользовательского запроса через LLM в структурированный `ParsedUserRequest`.

**Файлы:**
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/service/UserRequestParser.java`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/service/UserRequestParserTest.java`

**Что делать:**
1. Класс `UserRequestParser`:
   ```java
   public UserRequestParser(LLMClient llmClient, Resolver resolver)
   
   ParsedUserRequest parse(String userInput)
   ```
2. Метод `parse`:
   - Собрать список entity_types из Resolver (или хардкод на первом этапе)
   - Собрать список actions из Resolver
   - Загрузить промпт `parse-user-request.txt`
   - Заполнить переменные через `PromptTemplate.fill()`
   - Отправить в `llmClient.complete()`
   - Распарсить JSON ответ (Jackson ObjectMapper)
   - Вернуть `ParsedUserRequest`
3. Обработка ошибок: если LLM вернул невалидный JSON → `clarificationNeeded=true` с дефолтным вопросом

**Тест (с StubLLMClient):**
- Stub возвращает валидный JSON → корректный ParsedUserRequest
- Stub возвращает невалидный JSON → clarificationNeeded=true
- Правильные entity_type/action передаются в промпт

---

### Задача 4.7: Добавить `PlanGenerator`

**Цель:** Генерация многошагового плана из `ParsedUserRequest`.

**Файлы:**
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/service/PlanGenerator.java`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/service/PlanGeneratorTest.java`

**Что делать:**
1. Класс `PlanGenerator`:
   ```java
   public PlanGenerator(Resolver resolver)
   
   Plan generate(ParsedUserRequest request)
   Plan generate(ParsedUserRequest request, String target, String explanation)
   ```
2. Метод `generate`:
   - Для каждого `actionId` в `request.actionIds()`:
     - Проверить применимость через `resolver.isActionApplicable(actionId, request.entityTypeId())`
     - Создать `PlanStep` с `sortOrder` по порядку
     - `metaValue` из `request.parameters().get("meta_value")`
   - Собрать `Plan` с workflow `wf-plan`, step `new`
   - Если ни один action не применим → бросить `IllegalArgumentException`
3. Использовать UUIDs для id плана и шагов

**Тест (с InMemoryResolver):**
- Зарегистрировать entity_types и actions → сгенерировать план → проверить шаги
- Неприменимый action → исключение
- Пустой список actions → исключение
- Несколько actions → несколько шагов с правильным sortOrder

---

### Задача 4.8: Добавить `AppScanner` для сканирования страниц

**Цель:** Парсинг HTML для обнаружения UI элементов (кнопок, форм, инпутов и т.д.).

**Файлы:**
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/scanner/AppScanner.java`
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/scanner/BasicAppScanner.java`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/scanner/BasicAppScannerTest.java`
- Создать: `platform-knowledge/src/test/resources/sample-pages/simple-form.html`
- Создать: `platform-knowledge/src/test/resources/sample-pages/search-page.html`

**Что делать:**
1. Интерфейс `AppScanner`:
   ```java
   PageKnowledge scanPage(String html, String pageUrl);
   ```
2. `BasicAppScanner` (использует JSoup):
   - Парсить HTML через `Jsoup.parse(html)`
   - Обнаруживать элементы:
     - `<input>` → elementType = "input", selector = CSS (по id/name/type)
     - `<button>` → elementType = "button"
     - `<a href>` → elementType = "link"
     - `<form>` → elementType = "form"
     - `<table>` → elementType = "table"
     - `<select>` → elementType = "input" (с атрибутом type=select)
     - `<textarea>` → elementType = "input"
   - Для каждого элемента: извлечь label (text content, placeholder, aria-label, title)
   - Для каждого элемента: построить CSS selector (предпочитать id > name > class > tag)
   - Attributes: `name`, `type`, `role`, `placeholder`, `value`
3. `pageTitle` из `<title>` тега

**Тестовые HTML:**
- `simple-form.html`: форма с 3 инпутами, кнопкой submit, заголовком
- `search-page.html`: поисковая строка, кнопка поиска, несколько ссылок, таблица результатов

**Тест:**
- Парсинг `simple-form.html` → 1 form, 3 input, 1 button
- Парсинг `search-page.html` → input, button, links, table
- Пустой HTML → 0 элементов
- Title извлекается корректно
- CSS selectors корректные

---

### Задача 4.9: Добавить `EntityTypeDiscovery`

**Цель:** Маппинг обнаруженных UI элементов на entity_type и определение применимых действий.

**Файлы:**
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/service/EntityTypeDiscovery.java`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/service/EntityTypeDiscoveryTest.java`

**Что делать:**
1. Класс `EntityTypeDiscovery`:
   ```java
   public EntityTypeDiscovery(Resolver resolver)
   
   Map<String, List<String>> discoverApplicableActions(PageKnowledge page)
   // возвращает: entity_type_id → [action_id, ...]
   ```
2. Метод `discoverApplicableActions`:
   - Для каждого UIElement на странице:
     - Определить entity_type по `element.elementType()` (маппинг: "input" → "ent-input", "button" → "ent-button" и т.д.)
     - Через `resolver.findActionsApplicableToEntityType(entityTypeId)` — получить применимые действия
   - Вернуть map: entity_type → actions
3. Статический маппинг elementType → entity_type_id:
   ```java
   "input" → "ent-input"
   "button" → "ent-button"
   "link" → "ent-link"
   "form" → "ent-form"
   "table" → "ent-table"
   "page" → "ent-page"
   ```

**Тест (с InMemoryResolver):**
- Страница с input, button → map содержит "ent-input" и "ent-button" с правильными actions
- Пустая страница → пустой map
- Неизвестный elementType → пропускается (с логированием)

---

### Задача 4.10: Добавить `KnowledgeService` фасад

**Цель:** Оркестрация: сканирование → сохранение → обнаружение → генерация плана.

**Файлы:**
- Создать: `platform-knowledge/src/main/java/com/zaborstik/platform/knowledge/service/KnowledgeService.java`
- Создать: `platform-knowledge/src/test/java/com/zaborstik/platform/knowledge/service/KnowledgeServiceTest.java`

**Что делать:**
1. Класс `KnowledgeService`:
   ```java
   public KnowledgeService(
       KnowledgeRepository repository,
       AppScanner scanner,
       EntityTypeDiscovery discovery,
       UserRequestParser parser,
       PlanGenerator planGenerator
   )
   ```
2. Методы:
   - `AppKnowledge scanAndStore(String appName, String baseUrl, String html)`:
     - Сканировать HTML через `scanner.scanPage()`
     - Создать `AppKnowledge` с UUID
     - Сохранить через `repository.save()`
     - Вернуть сохранённый объект
   - `Optional<AppKnowledge> getKnowledge(String appId)`:
     - Делегировать в `repository.findByAppId()`
   - `Plan generatePlanFromRequest(String userInput)`:
     - Парсить запрос через `parser.parse()`
     - Если `clarificationNeeded` → бросить `ClarificationNeededException` с вопросом
     - Сгенерировать план через `planGenerator.generate()`
     - Вернуть `Plan`
3. Создать `ClarificationNeededException extends RuntimeException`:
   - Поле `String question`
   - Геттер `getQuestion()`

**Тест:**
- `scanAndStore` → сохраняет и возвращает AppKnowledge
- `getKnowledge` → находит ранее сохранённое
- `generatePlanFromRequest` при валидном запросе → Plan
- `generatePlanFromRequest` при clarificationNeeded → ClarificationNeededException
- `generatePlanFromRequest` при невалидном запросе → исключение

---

## Порядок выполнения

```
4.1 Setup модуля (pom.xml, пакеты)
  ↓
4.2 Domain model (AppKnowledge, PageKnowledge, UIElement, ParsedUserRequest)
  ↓
4.3 KnowledgeRepository (in-memory)
  ↓
4.4 LLMClient + StubLLMClient (параллельно с 4.3)
  ↓
4.5 PromptTemplate + шаблоны промптов (параллельно с 4.4)
  ↓
4.6 UserRequestParser (зависит от 4.4, 4.5)
  ↓
4.7 PlanGenerator (зависит от 4.2)
  ↓
4.8 AppScanner + BasicAppScanner (независимый, параллельно с 4.6)
  ↓
4.9 EntityTypeDiscovery (зависит от 4.8, 4.2)
  ↓
4.10 KnowledgeService фасад (зависит от всех предыдущих)
```

## Зависимости от других ботов

- **Нет входящих зависимостей.** Бот 4 работает полностью автономно.
- **Зависит от platform-core** (read-only): использует `Resolver`, `Plan`, `PlanStep`, `PlanStepAction`, `Action`, `EntityType` — эти классы уже существуют.
- **Не зависит от задач Бота 1:** Бот 4 использует существующий `Planner.createPlan()` и `Resolver` интерфейс. Новые методы Бота 1 (`createMultiStepPlan`, `LifecycleManager`) пока не нужны — интеграция произойдёт позже.
- **Исходящие:** После завершения, Бот 2 (`platform-api`) добавит dependency на `platform-knowledge` и создаст `KnowledgeController` для REST доступа — но это отдельная фаза интеграции.
