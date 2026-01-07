# Domain Layer

## Architecture

```mermaid
sequenceDiagram
    actor Клиент
    participant ExecutionEngine
    participant Resolver
    participant EntityTypeRepo as Repository: EntityType
    participant ActionRepo as Repository: Action  
    participant UIBindingRepo as Repository: UIBinding
    participant Plan

    Note over Клиент,Resolver: Фаза 1: Разрешение метаданных
    
    Клиент->>ExecutionEngine: createPlan(request)
    Note right of Клиент: ExecutionRequest {<br/>  entityType: "Building",<br/>  entityId: "93939",<br/>  action: "order_egrn_extract"<br/>}
    
    ExecutionEngine->>Resolver: resolveEntityType("Building")
    Resolver->>EntityTypeRepo: findById("Building")
    EntityTypeRepo-->>Resolver: EntityType{id="Building"}
    Resolver-->>ExecutionEngine: EntityType
    
    ExecutionEngine->>Resolver: resolveAction("order_egrn_extract")
    Resolver->>ActionRepo: findById("order_egrn_extract")
    ActionRepo-->>Resolver: Action{id="order_egrn_extract"}
    Resolver-->>ExecutionEngine: Action
    
    ExecutionEngine->>ExecutionEngine: action.isApplicableTo("Building")
    Note right of ExecutionEngine: Проверка: применимо ли действие<br/>к данному типу сущности
    
    ExecutionEngine->>Resolver: resolveUIBinding("order_egrn_extract")
    Resolver->>UIBindingRepo: findByActionId("order_egrn_extract")
    UIBindingRepo-->>Resolver: UIBinding{actionId="order_egrn_extract"}
    Resolver-->>ExecutionEngine: UIBinding
    
    Note over ExecutionEngine,Plan: Фаза 2: Построение плана
    
    ExecutionEngine->>ExecutionEngine: buildSteps()
    Note right of ExecutionEngine: Генерация шагов:<br/>1. OPEN_PAGE(/buildings/93939)<br/>2. FIND_ELEMENT(селектор)<br/>3. CLICK()<br/>4. WAIT_RESULT()
    
    ExecutionEngine->>Plan: new Plan(steps)
    Plan-->>ExecutionEngine: Plan объект
    
    ExecutionEngine-->>Клиент: Plan{id, steps, status}
    Note left of ExecutionEngine: План готов к выполнению
