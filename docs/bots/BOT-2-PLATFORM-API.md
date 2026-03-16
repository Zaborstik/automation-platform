# BOT 2: platform-api — REST API + Persistence

## Scope

**Module:** `platform-api/src/` only  
**Package base:** `com.zaborstik.platform.api`  
**Запрещено трогать:** любые файлы вне `platform-api/`

## Текущее состояние модуля

### Что уже есть:

| Пакет | Файлы | Описание |
|-------|-------|----------|
| `api/controller/` | `PlanController.java` | 5 эндпоинтов: POST create, GET by id, POST result, POST step-log, POST execute |
| `api/service/` | `PlanService.java`, `PlanExecutionService.java` | CRUD планов, оркестрация выполнения |
| `api/entity/` | 11 JPA entity | PlanEntity, PlanStepEntity, PlanStepActionEntity, PlanResultEntity, PlanStepLogEntryEntity, ActionEntity, ActionTypeEntity, EntityTypeEntity, AttachmentEntity, WorkflowEntity, WorkflowStepEntity |
| `api/repository/` | 9 repository | PlanRepository, PlanResultRepository, PlanStepLogEntryRepository, ActionRepository, ActionTypeRepository, AttachmentRepository, EntityTypeRepository, WorkflowRepository, WorkflowStepRepository |
| `api/dto/` | 8 DTO | CreatePlanRequest, PlanResponse, ExecutePlanResponse, CreatePlanResultRequest, CreatePlanStepLogEntryRequest, EntityDTO, AttachmentDTO, ErrorResponseDTO |
| `api/mapper/` | `PlanMapper.java` | Plan ↔ PlanEntity |
| `api/resolver/` | `DatabaseResolver.java` | Resolver по БД |
| `api/exception/` | `GlobalExceptionHandler.java` | Глобальная обработка ошибок |
| `api/config/` | `PlatformConfiguration.java`, `AgentExecutionConfiguration.java` | Spring конфигурация |

### Существующие тесты (11 файлов):
- `PlanControllerTest.java`, `PlanServiceTest.java`, `PlanExecutionServiceTest.java`
- `PlanMapperTest.java`, `ExecutionIntegrationTest.java`, `PlatformConfigurationTest.java`
- `DatabaseResolverTest.java`, `RepositoryIntegrationTest.java`
- `GlobalExceptionHandlerTest.java`, `DTOsSerializationTest.java`
- `DataJpaTestSchemaConfig.java`

### Flyway миграции:
- `V1__Create_schemas_and_tables.sql` — все таблицы
- `V2__Insert_initial_data.sql` — справочные данные
- `V3__Create_entities_table.sql` — закомментирована

---

## Что нужно сделать

### Задача 2.1: Добавить `PlanStepRepository` и `PlanStepActionRepository`

**Цель:** Отдельные репозитории для шагов и действий плана.

**Файлы:**
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/repository/PlanStepRepository.java`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/repository/PlanStepActionRepository.java`
- Изменить: `platform-api/src/test/java/com/zaborstik/platform/api/repository/RepositoryIntegrationTest.java`

**Что делать:**
1. `PlanStepRepository extends JpaRepository<PlanStepEntity, String>`:
   ```java
   List<PlanStepEntity> findByPlan_IdOrderBySortorder(String planId);
   Optional<PlanStepEntity> findByIdAndPlan_Id(String stepId, String planId);
   ```
2. `PlanStepActionRepository extends JpaRepository<PlanStepActionEntity, PlanStepActionId>`:
   ```java
   List<PlanStepActionEntity> findByPlanStep_Id(String planStepId);
   ```

**Тест:**
- Сохранить план с шагами → найти шаги по planId
- Найти шаг по id и planId
- Найти actions по planStepId
- Порядок шагов по sortorder

---

### Задача 2.2: Добавить `ActionService`

**Цель:** Сервис для CRUD операций над действиями.

**Файлы:**
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/service/ActionService.java`
- Создать: `platform-api/src/test/java/com/zaborstik/platform/api/service/ActionServiceTest.java`

**Что делать:**
1. `@Service` class с зависимостями `ActionRepository`, `ActionTypeRepository`, `EntityTypeRepository`
2. Методы:
   - `List<ActionEntity> listAll()` — все действия
   - `Optional<ActionEntity> getById(String id)` — по id
   - `ActionEntity create(String displayname, String internalname, String description, String actionTypeId, String metaValue)` — создать новое
   - `ActionEntity update(String id, String displayname, String description, String metaValue)` — обновить
   - `void delete(String id)` — удалить
   - `List<ActionEntity> findByEntityType(String entityTypeId)` — действия для типа сущности
3. `create` — проверяет существование ActionType, генерирует UUID
4. `update` — проверяет существование Action

**Тест (Mockito unit):**
- `listAll` — возвращает список
- `getById` — находит / не находит
- `create` — успех / ActionType not found → исключение
- `update` — успех / Action not found → исключение
- `delete` — вызывает deleteById
- `findByEntityType` — делегирует в repository

---

### Задача 2.3: Добавить `ActionController` и DTOs

**Цель:** REST эндпоинты для управления действиями.

**Файлы:**
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/controller/ActionController.java`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/dto/ActionResponse.java`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/dto/CreateActionRequest.java`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/dto/UpdateActionRequest.java`
- Создать: `platform-api/src/test/java/com/zaborstik/platform/api/controller/ActionControllerTest.java`

**Что делать:**
1. `@RestController @RequestMapping("/api/actions")`
2. Эндпоинты:
   - `GET /api/actions` — список всех (опционально `?entityTypeId=X`)
   - `GET /api/actions/{id}` — по id (404 если не найден)
   - `POST /api/actions` — создать (`@Valid @RequestBody CreateActionRequest`)
   - `PUT /api/actions/{id}` — обновить
   - `DELETE /api/actions/{id}` — удалить (204 No Content)
3. `ActionResponse`: `id`, `displayname`, `internalname`, `description`, `metaValue`, `actionTypeId`, `createdTime`, `updatedTime`
4. `CreateActionRequest`: `@NotBlank displayname`, `@NotBlank internalname`, `description`, `@NotBlank actionTypeId`, `metaValue`
5. `UpdateActionRequest`: `displayname`, `description`, `metaValue`

**Тест (@WebMvcTest):**
- GET all → 200 + JSON array
- GET by id → 200 / 404
- POST → 201 Created
- PUT → 200 / 404
- DELETE → 204
- POST с невалидным body → 400

---

### Задача 2.4: Добавить `EntityTypeService` и `EntityTypeController`

**Цель:** CRUD для типов сущностей.

**Файлы:**
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/service/EntityTypeService.java`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/controller/EntityTypeController.java`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/dto/EntityTypeResponse.java`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/dto/CreateEntityTypeRequest.java`
- Создать: `platform-api/src/test/java/com/zaborstik/platform/api/service/EntityTypeServiceTest.java`
- Создать: `platform-api/src/test/java/com/zaborstik/platform/api/controller/EntityTypeControllerTest.java`

**Что делать:**
1. `EntityTypeService`:
   - `listAll()`, `getById()`, `create(displayname, uiDescription, kmArticle)`, `update()`, `delete()`
2. `EntityTypeController @RequestMapping("/api/entity-types")`:
   - `GET /api/entity-types` — список
   - `GET /api/entity-types/{id}` — по id
   - `POST /api/entity-types` — создать
   - `PUT /api/entity-types/{id}` — обновить
   - `DELETE /api/entity-types/{id}` — удалить
3. `EntityTypeResponse`: `id`, `displayname`, `uiDescription`, `kmArticle`, `entityfieldlist`, `buttons`, `createdTime`, `updatedTime`
4. `CreateEntityTypeRequest`: `@NotBlank displayname`, `uiDescription`, `kmArticle`

**Тесты:**
- Unit тесты для `EntityTypeService` (Mockito)
- `@WebMvcTest` для `EntityTypeController`

---

### Задача 2.5: Добавить `WorkflowController` (read-only)

**Цель:** Эндпоинты для чтения жизненных циклов и шагов.

**Файлы:**
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/controller/WorkflowController.java`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/dto/WorkflowResponse.java`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/dto/WorkflowStepResponse.java`
- Создать: `platform-api/src/test/java/com/zaborstik/platform/api/controller/WorkflowControllerTest.java`

**Что делать:**
1. `@RestController @RequestMapping("/api/workflows")`
2. Эндпоинты (только чтение):
   - `GET /api/workflows` — список всех workflow
   - `GET /api/workflows/{id}` — workflow по id (404 если нет)
   - `GET /api/workflow-steps` — список всех шагов ЖЦ
   - `GET /api/workflow-steps/{id}` — шаг по id
3. `WorkflowResponse`: `id`, `displayname`, `firstStepId`
4. `WorkflowStepResponse`: `id`, `internalname`, `displayname`, `sortorder`

**Тест (@WebMvcTest):**
- GET all workflows → 200
- GET workflow by id → 200 / 404
- GET all steps → 200
- GET step by id → 200 / 404

---

### Задача 2.6: Добавить листинг и поиск планов

**Цель:** Эндпоинт `GET /api/plans` с фильтрацией и пагинацией.

**Файлы:**
- Изменить: `platform-api/src/main/java/com/zaborstik/platform/api/controller/PlanController.java`
- Изменить: `platform-api/src/main/java/com/zaborstik/platform/api/service/PlanService.java`
- Изменить: `platform-api/src/main/java/com/zaborstik/platform/api/repository/PlanRepository.java`
- Изменить: `platform-api/src/test/java/com/zaborstik/platform/api/controller/PlanControllerTest.java`

**Что делать:**
1. В `PlanRepository` добавить:
   ```java
   Page<PlanEntity> findByWorkflowStepInternalname(String status, Pageable pageable);
   Page<PlanEntity> findAll(Pageable pageable);
   ```
2. В `PlanService` добавить:
   ```java
   Page<PlanResponse> listPlans(String status, Pageable pageable);
   ```
3. В `PlanController` добавить:
   ```java
   @GetMapping
   public Page<PlanResponse> listPlans(
       @RequestParam(required = false) String status,
       @RequestParam(defaultValue = "0") int page,
       @RequestParam(defaultValue = "20") int size
   )
   ```

**Тест:**
- GET /api/plans → 200, возвращает Page
- GET /api/plans?status=new → фильтрация
- GET /api/plans?page=0&size=5 → пагинация

---

### Задача 2.7: Добавить эндпоинт перехода ЖЦ плана

**Цель:** `PATCH /api/plans/{id}/transition` для смены статуса плана.

**Файлы:**
- Изменить: `platform-api/src/main/java/com/zaborstik/platform/api/controller/PlanController.java`
- Изменить: `platform-api/src/main/java/com/zaborstik/platform/api/service/PlanService.java`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/dto/TransitionPlanRequest.java`

**Что делать:**
1. DTO `TransitionPlanRequest`: `@NotBlank String targetStep`
2. В `PlanService` добавить:
   ```java
   PlanResponse transitionPlan(String planId, String targetStep)
   ```
   - Загрузить PlanEntity
   - Использовать `LifecycleManager.validateTransition()` (из platform-core)
   - Обновить `workflowStepInternalname`
   - Сохранить
3. В `PlanController` добавить:
   ```java
   @PatchMapping("/{id}/transition")
   public ResponseEntity<PlanResponse> transitionPlan(@PathVariable String id, @Valid @RequestBody TransitionPlanRequest request)
   ```

**ВАЖНО:** Эта задача зависит от Бота 1 (задача 1.3 — LifecycleManager). Начинать только после того, как Бот 1 завершит 1.1–1.3.

**Тест:**
- PATCH с валидным переходом → 200
- PATCH с невалидным переходом → 400/409
- PATCH для несуществующего плана → 404

---

### Задача 2.8: Обновление статусов при выполнении плана

**Цель:** При выполнении плана автоматически обновлять ЖЦ статусы.

**Файлы:**
- Изменить: `platform-api/src/main/java/com/zaborstik/platform/api/service/PlanExecutionService.java`
- Изменить: `platform-api/src/test/java/com/zaborstik/platform/api/service/PlanExecutionServiceTest.java`

**Что делать:**
1. В начале `executePlan()`: перевести план в `in_progress`
2. По мере выполнения: обновлять `stoppedAtPlanStep` на текущий шаг
3. В конце: перевести план в `completed` или `failed`
4. Каждый PlanStep: `new` → `in_progress` → `completed`/`failed`
5. Использовать `PlanService.transitionPlan()` или прямое обновление entity

**ВАЖНО:** Зависит от задачи 2.7.

**Тест:**
- После успешного выполнения: план в статусе `completed`
- После неуспешного: план в статусе `failed`
- `stoppedAtPlanStep` обновлён на последний шаг

---

### Задача 2.9: Добавить OpenAPI/Swagger документацию

**Цель:** Автогенерация API документации.

**Файлы:**
- Изменить: `platform-api/pom.xml`
- Изменить: `platform-api/src/main/resources/application.properties`
- Изменить: все контроллеры (добавить аннотации)

**Что делать:**
1. Добавить зависимость в `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
       <version>2.3.0</version>
   </dependency>
   ```
2. В `application.properties`:
   ```properties
   springdoc.api-docs.path=/api-docs
   springdoc.swagger-ui.path=/swagger-ui.html
   ```
3. Добавить `@Tag(name = "Plans")`, `@Operation(summary = "...")`, `@ApiResponse` к эндпоинтам

**Тест:** Swagger UI доступен (проверяется интеграционным тестом или вручную).

---

### Задача 2.10: Обновить `DatabaseResolver` для поддержки ЖЦ переходов

**Цель:** `DatabaseResolver` должен реализовать новые методы `Resolver` для переходов.

**Файлы:**
- Создать: `platform-api/src/main/resources/db/migration/V4__Create_workflow_transition_table.sql`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/entity/WorkflowTransitionEntity.java`
- Создать: `platform-api/src/main/java/com/zaborstik/platform/api/repository/WorkflowTransitionRepository.java`
- Изменить: `platform-api/src/main/java/com/zaborstik/platform/api/resolver/DatabaseResolver.java`
- Изменить: `platform-api/src/test/java/com/zaborstik/platform/api/resolver/DatabaseResolverTest.java`

**Что делать:**
1. Миграция V4:
   ```sql
   CREATE TABLE IF NOT EXISTS system.workflow_transition (
       id VARCHAR(36) NOT NULL PRIMARY KEY,
       workflow VARCHAR(36) NOT NULL,
       from_step VARCHAR(255) NOT NULL,
       to_step VARCHAR(255) NOT NULL,
       FOREIGN KEY (workflow) REFERENCES system.workflow(id)
   );
   INSERT INTO system.workflow_transition (id, workflow, from_step, to_step) VALUES
       ('wft-1', 'wf-plan', 'new', 'in_progress'),
       ('wft-2', 'wf-plan', 'in_progress', 'paused'),
       ('wft-3', 'wf-plan', 'in_progress', 'completed'),
       ('wft-4', 'wf-plan', 'in_progress', 'failed'),
       ('wft-5', 'wf-plan', 'paused', 'in_progress'),
       ('wft-6', 'wf-plan', 'paused', 'cancelled'),
       ('wft-7', 'wf-plan', 'new', 'cancelled'),
       -- аналогично для wf-plan-step
       ('wft-8', 'wf-plan-step', 'new', 'in_progress'),
       ('wft-9', 'wf-plan-step', 'in_progress', 'completed'),
       ('wft-10', 'wf-plan-step', 'in_progress', 'failed'),
       ('wft-11', 'wf-plan-step', 'in_progress', 'paused'),
       ('wft-12', 'wf-plan-step', 'paused', 'in_progress'),
       ('wft-13', 'wf-plan-step', 'paused', 'cancelled'),
       ('wft-14', 'wf-plan-step', 'new', 'cancelled');
   ```
2. JPA Entity `WorkflowTransitionEntity` с полями: `id`, `workflow` (ManyToOne), `fromStep`, `toStep`
3. Repository с: `findByWorkflow_Id(String workflowId)`, `findByWorkflow_IdAndFromStepAndToStep(String workflowId, String from, String to)`
4. В `DatabaseResolver` реализовать `findTransitions()` и `findTransition()` через маппинг entity → domain

**ВАЖНО:** Зависит от Бота 1 (задача 1.2 — методы в Resolver).

**Тест:**
- `findTransitions("wf-plan")` — возвращает переходы
- `findTransition("wf-plan", "new", "in_progress")` — находит
- `findTransition("wf-plan", "completed", "new")` — empty

---

## Порядок выполнения

```
2.1 PlanStepRepository, PlanStepActionRepository (независимый)
  ↓
2.2 ActionService (независимый)
  ↓
2.3 ActionController + DTOs (зависит от 2.2)
  ↓
2.4 EntityTypeService + Controller (независимый, параллельно с 2.3)
  ↓
2.5 WorkflowController (независимый, параллельно с 2.4)
  ↓
2.6 Plan listing/search (независимый)
  ↓
2.9 OpenAPI/Swagger (независимый, можно параллельно с 2.6)
  ↓
--- ЖДЁМ БОТА 1 (задачи 1.1–1.3) ---
  ↓
2.10 DatabaseResolver для переходов ЖЦ (зависит от Bot 1)
  ↓
2.7 Endpoint перехода ЖЦ (зависит от 2.10)
  ↓
2.8 Обновление статусов при выполнении (зависит от 2.7)
```

## Зависимости от других ботов

- **Входящие от Бота 1:** Задачи 2.7, 2.8, 2.10 зависят от `LifecycleManager` и расширенного `Resolver` (Бот 1, задачи 1.1–1.3).
- **Задачи 2.1–2.6, 2.9 можно делать параллельно с Ботом 1** — они не зависят от новых классов.
- **Исходящие:** Нет. Другие боты не зависят от Бота 2.
