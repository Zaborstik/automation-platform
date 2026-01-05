# Документация по автотестам Platform API

Этот документ описывает структуру, подходы и принципы работы автотестов для Platform API.

## Содержание

1. [Обзор архитектуры тестирования](#обзор-архитектуры-тестирования)
2. [Типы тестов](#типы-тестов)
3. [Структура тестов](#структура-тестов)
4. [Детальное описание тестов](#детальное-описание-тестов)
5. [Запуск тестов](#запуск-тестов)
6. [Best Practices](#best-practices)

---

## Обзор архитектуры тестирования

Тесты организованы по принципу **пирамиды тестирования**:

```
        /\
       /  \     Интеграционные тесты (1)
      /____\    
     /      \   Unit тесты (5)
    /________\  
```

- **Unit тесты** (5 наборов) - быстрые, изолированные тесты отдельных компонентов
- **Интеграционные тесты** (1 набор) - тесты полного стека приложения

### Технологический стек

- **JUnit 5** - фреймворк для тестирования
- **Mockito** - мокирование зависимостей
- **Spring Boot Test** - тестирование Spring приложений
- **MockMvc** - тестирование REST контроллеров
- **Jackson ObjectMapper** - тестирование сериализации JSON

---

## Типы тестов

### 1. Unit тесты

Изолированные тесты отдельных компонентов с моками зависимостей.

**Преимущества:**
- Быстрые (миллисекунды)
- Изолированные (не зависят от внешних систем)
- Легко отлаживать
- Покрывают граничные случаи

**Примеры:**
- `ExecutionServiceTest` - тестирует сервис с моком ExecutionEngine
- `GlobalExceptionHandlerTest` - тестирует обработчик исключений

### 2. Интеграционные тесты

Тесты полного стека приложения с реальным Spring контекстом.

**Преимущества:**
- Проверяют реальную интеграцию компонентов
- Тестируют конфигурацию Spring
- Проверяют работу с реальными данными

**Примеры:**
- `ExecutionIntegrationTest` - полный цикл создания плана через REST API

### 3. Тесты конфигурации

Проверяют правильность настройки Spring бинов и их взаимодействие.

**Примеры:**
- `PlatformConfigurationTest` - проверка создания и настройки Resolver и ExecutionEngine

### 4. Тесты сериализации

Проверяют корректность преобразования объектов в JSON и обратно.

**Примеры:**
- `DTOsSerializationTest` - проверка работы Jackson с DTO классами

---

## Структура тестов

```
platform-api/src/test/java/org/example/api/
├── controller/
│   └── ExecutionControllerTest.java      # REST API контроллер
├── service/
│   └── ExecutionServiceTest.java         # Сервисный слой
├── config/
│   └── PlatformConfigurationTest.java    # Конфигурация Spring
├── exception/
│   └── GlobalExceptionHandlerTest.java   # Обработка ошибок
├── dto/
│   └── DTOsSerializationTest.java        # Сериализация DTO
└── integration/
    └── ExecutionIntegrationTest.java      # Интеграционные тесты
```

---

## Детальное описание тестов

### 1. ExecutionControllerTest

**Тип:** Unit тест с MockMvc  
**Назначение:** Тестирование REST API эндпоинтов

**Как работает:**

```java
@WebMvcTest(ExecutionController.class)  // Загружает только контроллер
class ExecutionControllerTest {
    @MockBean
    private ExecutionService executionService;  // Мок сервиса
    
    @Autowired
    private MockMvc mockMvc;  // Виртуальный HTTP клиент
}
```

**Ключевые тесты:**

1. **shouldCreatePlanSuccessfully** - успешное создание плана
   ```java
   // Мокаем сервис
   when(executionService.createPlan(any())).thenReturn(testPlan);
   
   // Выполняем HTTP запрос
   mockMvc.perform(post("/api/execution/plan")
       .contentType(MediaType.APPLICATION_JSON)
       .content(json))
       .andExpect(status().isCreated())
       .andExpect(jsonPath("$.id").value("test-plan-id"));
   ```

2. **shouldReturnBadRequestWhenEntityTypeIsMissing** - валидация входных данных
   - Проверяет, что Spring Validation работает
   - Возвращает 400 Bad Request при отсутствии обязательных полей

3. **shouldHandleServiceException** - обработка ошибок сервиса
   - Мокает исключение из сервиса
   - Проверяет, что оно корректно обрабатывается

**Особенности:**
- Использует `@WebMvcTest` - загружает только веб-слой, без полного контекста
- `@MockBean` - создает мок для ExecutionService
- `MockMvc` - симулирует HTTP запросы без реального сервера

---

### 2. ExecutionServiceTest

**Тип:** Unit тест с моками  
**Назначение:** Тестирование бизнес-логики сервиса

**Как работает:**

```java
@ExtendWith(MockitoExtension.class)  // Включает Mockito
class ExecutionServiceTest {
    @Mock
    private ExecutionEngine executionEngine;  // Мок зависимости
    
    @InjectMocks
    private ExecutionService executionService;  // Тестируемый класс
}
```

**Ключевые тесты:**

1. **shouldCreatePlanSuccessfully** - преобразование DTO
   ```java
   // Мокаем ExecutionEngine
   when(executionEngine.createPlan(any(ExecutionRequest.class)))
       .thenReturn(testPlan);
   
   // Вызываем сервис
   PlanDTO result = executionService.createPlan(requestDTO);
   
   // Проверяем преобразование
   assertEquals("Building", result.getEntityTypeId());
   ```

2. **shouldConvertPlanStepsCorrectly** - проверка преобразования шагов
   - Проверяет, что каждый PlanStep корректно преобразуется в PlanStepDTO
   - Проверяет все типы шагов: open_page, explain, hover, click, wait

3. **shouldPropagateExceptionFromEngine** - проброс исключений
   - Проверяет, что исключения из ExecutionEngine пробрасываются дальше

**Особенности:**
- `@Mock` - создает мок объекта
- `@InjectMocks` - автоматически инжектит моки в тестируемый класс
- Тестирует только логику преобразования, без реальных зависимостей

---

### 3. PlatformConfigurationTest

**Тип:** Unit тест конфигурации  
**Назначение:** Проверка правильности настройки Spring бинов

**Как работает:**

```java
class PlatformConfigurationTest {
    @BeforeEach
    void setUp() {
        // Создаем конфигурацию вручную
        configuration = new PlatformConfiguration();
        resolver = configuration.resolver();
        executionEngine = configuration.executionEngine(resolver);
    }
}
```

**Ключевые тесты:**

1. **shouldFindRegisteredEntityTypes** - проверка регистрации EntityType
   ```java
   assertTrue(resolver.findEntityType("Building").isPresent());
   EntityType building = resolver.findEntityType("Building").orElseThrow();
   assertEquals("Здание", building.getName());
   ```

2. **shouldCheckActionApplicability** - проверка применимости действий
   ```java
   // order_egrn_extract применимо только к Building
   assertTrue(resolver.isActionApplicable("order_egrn_extract", "Building"));
   assertFalse(resolver.isActionApplicable("order_egrn_extract", "Contract"));
   ```

3. **shouldCreatePlanWithConfiguredResolver** - проверка работы с реальным планом
   - Создает реальный ExecutionRequest
   - Проверяет, что план создается корректно

**Особенности:**
- Тестирует конфигурацию без Spring контекста
- Проверяет предустановленные данные (Building, Contract, actions)
- Верифицирует правильность связей между компонентами

---

### 4. GlobalExceptionHandlerTest

**Тип:** Unit тест обработчика исключений  
**Назначение:** Проверка корректной обработки всех типов ошибок

**Как работает:**

```java
class GlobalExceptionHandlerTest {
    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        webRequest = new ServletWebRequest(mock(HttpServletRequest.class));
    }
}
```

**Ключевые тесты:**

1. **shouldHandleMethodArgumentNotValidException** - валидация входных данных
   ```java
   // Создаем мок исключения валидации
   MethodArgumentNotValidException ex = mock(...);
   when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
   
   // Обрабатываем
   ResponseEntity<ErrorResponseDTO> response = 
       handler.handleValidationExceptions(ex, webRequest);
   
   // Проверяем структуру ответа
   assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
   assertEquals("Validation Failed", response.getBody().getError());
   ```

2. **shouldHandleIllegalArgumentException** - бизнес-логика ошибки
   - Проверяет обработку ошибок типа "EntityType not found"
   - Возвращает структурированный ErrorResponseDTO

3. **shouldHandleGenericException** - общие исключения
   - Обрабатывает все неожиданные исключения
   - Возвращает 500 Internal Server Error

**Особенности:**
- Тестирует каждый тип исключения отдельно
- Проверяет структуру ErrorResponseDTO
- Верифицирует HTTP статус коды

---

### 5. DTOsSerializationTest

**Тип:** Unit тест сериализации  
**Назначение:** Проверка корректности JSON сериализации/десериализации

**Как работает:**

```java
class DTOsSerializationTest {
    private ObjectMapper objectMapper;  // Jackson mapper
    
    @Test
    void shouldSerializeAndDeserializeExecutionRequestDTO() {
        // Создаем DTO
        ExecutionRequestDTO original = new ExecutionRequestDTO(...);
        
        // Сериализуем в JSON
        String json = objectMapper.writeValueAsString(original);
        
        // Десериализуем обратно
        ExecutionRequestDTO deserialized = 
            objectMapper.readValue(json, ExecutionRequestDTO.class);
        
        // Проверяем равенство
        assertEquals(original.getEntityType(), deserialized.getEntityType());
    }
}
```

**Ключевые тесты:**

1. **shouldSerializeAndDeserializeExecutionRequestDTO** - базовый DTO
   - Проверяет round-trip сериализацию
   - Проверяет работу с параметрами

2. **shouldHandleJsonPropertyAnnotations** - проверка аннотаций
   ```java
   // Проверяем, что JSON использует правильные имена полей
   assertTrue(json.contains("\"entity\":\"Building\""));
   assertTrue(json.contains("\"entityId\":\"93939\""));
   ```

3. **shouldSerializeAndDeserializePlanDTO** - сложный объект
   - Тестирует вложенные объекты (PlanDTO содержит List<PlanStepDTO>)
   - Проверяет сохранение структуры

**Особенности:**
- Использует реальный ObjectMapper (как в приложении)
- Проверяет работу @JsonProperty аннотаций
- Тестирует обработку null значений

---

### 6. ExecutionIntegrationTest

**Тип:** Интеграционный тест  
**Назначение:** Тестирование полного стека приложения

**Как работает:**

```java
@SpringBootTest(classes = PlatformApiApplication.class)  // Полный контекст
@AutoConfigureMockMvc  // Настраивает MockMvc
class ExecutionIntegrationTest {
    @Autowired
    private MockMvc mockMvc;  // Реальный HTTP клиент
    
    @Autowired
    private ObjectMapper objectMapper;  // Реальный mapper
}
```

**Ключевые тесты:**

1. **shouldCreatePlanForBuildingEntity** - полный цикл
   ```java
   // Создаем реальный запрос
   ExecutionRequestDTO request = new ExecutionRequestDTO(
       "Building", "93939", "order_egrn_extract", Map.of()
   );
   
   // Выполняем HTTP запрос (реальный контекст Spring)
   MvcResult result = mockMvc.perform(post("/api/execution/plan")
       .contentType(MediaType.APPLICATION_JSON)
       .content(objectMapper.writeValueAsString(request)))
       .andExpect(status().isCreated())
       .andReturn();
   
   // Проверяем реальный ответ
   PlanDTO plan = objectMapper.readValue(
       result.getResponse().getContentAsString(), 
       PlanDTO.class
   );
   
   // Верифицируем структуру плана
   assertEquals(5, plan.getSteps().size());
   assertEquals("open_page", plan.getSteps().get(0).getType());
   ```

2. **shouldReturnBadRequestForNonExistentEntityType** - обработка ошибок
   - Использует реальный ExecutionEngine
   - Проверяет, что ошибки корректно обрабатываются через весь стек

3. **shouldReturnBadRequestForNonApplicableAction** - бизнес-логика
   - Проверяет применимость действий
   - order_egrn_extract не применимо к Contract

**Особенности:**
- `@SpringBootTest` - загружает полный Spring контекст
- Использует реальные бины (не моки)
- Тестирует интеграцию всех слоев:
  - Controller → Service → ExecutionEngine → Planner → Resolver
- Проверяет реальную конфигурацию из PlatformConfiguration

**Отличия от unit тестов:**

| Аспект | Unit тест | Интеграционный тест |
|--------|-----------|---------------------|
| Контекст | Частичный (@WebMvcTest) | Полный (@SpringBootTest) |
| Зависимости | Моки (@MockBean) | Реальные бины |
| Скорость | Быстро (мс) | Медленнее (секунды) |
| Изоляция | Полная | Зависит от конфигурации |
| Назначение | Логика компонента | Интеграция компонентов |

---

## Запуск тестов

### Все тесты

```bash
# Из корня проекта
mvn test

# Из модуля platform-api
cd platform-api
mvn test
```

### Конкретный тест класс

```bash
mvn test -Dtest=ExecutionControllerTest
```

### Конкретный тест метод

```bash
mvn test -Dtest=ExecutionControllerTest#shouldCreatePlanSuccessfully
```

### С покрытием кода (JaCoCo)

```bash
# Добавьте в pom.xml плагин JaCoCo, затем:
mvn test jacoco:report

# Отчет будет в target/site/jacoco/index.html
```

### В IDE (IntelliJ IDEA)

1. Правый клик на файл теста → "Run 'TestClassName'"
2. Или используйте зеленую стрелку рядом с классом/методом
3. Для всех тестов: правый клик на `src/test` → "Run All Tests"

---

## Best Practices

### 1. Структура теста (AAA Pattern)

```java
@Test
void shouldCreatePlanSuccessfully() {
    // Arrange (Given) - подготовка данных
    ExecutionRequestDTO request = new ExecutionRequestDTO(...);
    when(service.createPlan(any())).thenReturn(testPlan);
    
    // Act (When) - выполнение действия
    PlanDTO result = service.createPlan(request);
    
    // Assert (Then) - проверка результата
    assertNotNull(result);
    assertEquals("Building", result.getEntityTypeId());
}
```

### 2. Именование тестов

Используйте описательные имена:
- ✅ `shouldCreatePlanSuccessfully`
- ✅ `shouldReturnBadRequestWhenEntityTypeIsMissing`
- ❌ `test1`
- ❌ `createPlan`

### 3. Изоляция тестов

Каждый тест должен быть независимым:

```java
@BeforeEach
void setUp() {
    // Каждый тест начинается с чистого состояния
    resolver = new InMemoryResolver();
    engine = new ExecutionEngine(resolver);
}
```

### 4. Моки vs Реальные объекты

**Используйте моки когда:**
- Тестируете изолированную логику
- Зависимость медленная (БД, сеть)
- Нужно проверить конкретное поведение

**Используйте реальные объекты когда:**
- Тестируете интеграцию
- Объект простой и быстрый
- Нужно проверить реальное взаимодействие

### 5. Покрытие граничных случаев

```java
// Нормальный случай
shouldCreatePlanSuccessfully()

// Граничные случаи
shouldHandleRequestWithNullParameters()
shouldHandleRequestWithEmptyParameters()
shouldReturnBadRequestForNonExistentEntityType()
```

### 6. Проверка структуры ответов

```java
// Проверяем не только статус, но и структуру
.andExpect(jsonPath("$.id").exists())
.andExpect(jsonPath("$.entityType").value("Building"))
.andExpect(jsonPath("$.steps").isArray())
.andExpect(jsonPath("$.steps.length()").value(5))
```

---

## Отладка тестов

### 1. Логирование

```java
@Test
void shouldCreatePlanSuccessfully() {
    // Включаем логирование
    System.out.println("Request: " + request);
    
    PlanDTO result = service.createPlan(request);
    
    System.out.println("Result: " + result);
    // или используйте logger
}
```

### 2. Отладка в IDE

- Установите breakpoint в тесте
- Запустите тест в режиме Debug
- Проверьте значения переменных

### 3. Проверка JSON

```java
String json = objectMapper.writeValueAsString(result);
System.out.println("JSON: " + json);
// Проверьте структуру вручную
```

---

## Расширение тестов

### Добавление нового теста

1. Создайте тест класс в соответствующей папке
2. Используйте существующие тесты как шаблон
3. Следуйте AAA паттерну
4. Добавьте проверку граничных случаев

### Пример добавления теста для нового эндпоинта

```java
@Test
void shouldExecutePlanSuccessfully() throws Exception {
    // Given
    String planId = "plan-123";
    
    // When & Then
    mockMvc.perform(post("/api/execution/execute/{id}", planId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EXECUTING"));
}
```

---

## Заключение

Тесты покрывают:

✅ **Контроллеры** - REST API эндпоинты  
✅ **Сервисы** - бизнес-логика  
✅ **Конфигурацию** - настройка Spring  
✅ **Обработку ошибок** - все типы исключений  
✅ **DTO** - сериализация/десериализация  
✅ **Интеграцию** - полный стек приложения  

Все тесты следуют единому стилю и best practices, что обеспечивает:
- Легкость поддержки
- Понятность для новых разработчиков
- Надежность проверок
- Быстрое обнаружение регрессий

