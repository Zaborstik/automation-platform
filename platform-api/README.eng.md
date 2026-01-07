# Platform API

REST API for universal execution platform. Provides endpoints for creating action execution plans through UI.

## Architecture

API is built on Spring Boot and provides REST interface for working with execution engine:

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

## Running the Application

```bash
mvn spring-boot:run
```

Or build and run JAR:

```bash
mvn clean package
java -jar target/platform-api-1.0-SNAPSHOT.jar
```

Application will start on port `8080` (configurable in `application.properties`).

## API Endpoints

### POST /api/execution/plan

Creates execution plan for the specified action.

**Request:**
```json
{
  "entity": "Building",
  "entityId": "93939",
  "action": "order_egrn_extract",
  "parameters": {}
}
```

**Response (201 Created):**
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
      "explanation": "Opening Building card #93939",
      "parameters": {}
    },
    {
      "type": "explain",
      "target": null,
      "explanation": "Orders EGRN extract for the specified building",
      "parameters": {}
    },
    {
      "type": "hover",
      "target": "action(order_egrn_extract)",
      "explanation": "Hovering over action element 'Order EGRN Extract'",
      "parameters": {}
    },
    {
      "type": "click",
      "target": "action(order_egrn_extract)",
      "explanation": "Executing action 'Order EGRN Extract'",
      "parameters": {}
    },
    {
      "type": "wait",
      "target": "result",
      "explanation": "Waiting for action 'Order EGRN Extract' completion",
      "parameters": {}
    }
  ]
}
```

**Errors:**

- `400 Bad Request` - invalid input data or EntityType/Action/UIBinding not found
- `500 Internal Server Error` - internal server error

### GET /api/execution/plan/{id}

Gets plan by identifier.

**Status:** `501 Not Implemented` (plans are not stored yet)

## Usage Examples

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

## Preconfigured Data

MVP uses preconfigured EntityType, Action and UIBinding:

### EntityType
- `Building` - Building
- `Contract` - Contract

### Actions
- `order_egrn_extract` - Order EGRN Extract (applicable to Building)
- `close_contract` - Close Contract (applicable to Contract)
- `assign_owner` - Assign Owner (applicable to Building)

### UIBindings
Each action has binding to UI element through CSS/XPath selectors.

## Project Structure

```
platform-api/
├── src/main/java/org/example/api/
│   ├── PlatformApiApplication.java    # Main Spring Boot class
│   ├── controller/
│   │   └── ExecutionController.java   # REST controller
│   ├── service/
│   │   └── ExecutionService.java      # Service layer
│   ├── dto/
│   │   ├── ExecutionRequestDTO.java   # Request DTO
│   │   ├── PlanDTO.java              # Plan DTO
│   │   ├── PlanStepDTO.java          # Plan step DTO
│   │   └── ErrorResponseDTO.java     # Error DTO
│   ├── config/
│   │   └── PlatformConfiguration.java # Spring configuration
│   └── exception/
│       └── GlobalExceptionHandler.java # Exception handler
└── src/main/resources/
    └── application.properties         # Application configuration
```

## Extending Functionality

### Adding New EntityType/Action/UIBinding

Edit `PlatformConfiguration.java` and add registration of new components in methods:
- `registerExampleEntityTypes()`
- `registerExampleActions()`
- `registerExampleUIBindings()`

### Database Integration

In the future `InMemoryResolver` can be replaced with DB repository. For this:
1. Create repository interfaces (JPA/Spring Data)
2. Implement `Resolver` through repositories
3. Replace bean in `PlatformConfiguration`

### Plan Storage

To implement `GET /api/execution/plan/{id}`:
1. Add repository for `Plan`
2. Save plans in `ExecutionService.createPlan()`
3. Implement retrieval in controller

## Dependencies

- Spring Boot 3.2.1
- Spring Web
- Spring Validation
- Jackson (for JSON)
- platform-core (platform core)

