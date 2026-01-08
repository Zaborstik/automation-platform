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
[ DatabaseResolver (JPA) ]
    |
    v
[ PostgreSQL / H2 Database ]
```

## Data Persistence

Platform uses JPA (Hibernate) for database operations:

- **Development**: H2 in-memory database
- **Production**: PostgreSQL

All data is persisted in database:
- EntityType, Action, UIBinding (metadata)
- Plans (execution plans)
- PlanSteps (plan steps)
- ExecutionResults (execution results)
- ExecutionLogEntries (execution log entries)

### Database Migrations

Flyway is used for migration management:
- `V1__Create_base_tables.sql` - creates all tables
- `V2__Insert_initial_data.sql` - initial data

## Running the Application

### Development (H2)

```bash
mvn spring-boot:run
# or
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Application will start on port `8080` with H2 in-memory database.
H2 Console is available at: http://localhost:8080/h2-console

### Production (PostgreSQL)

```bash
# Set environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/platformdb
export DATABASE_USERNAME=platform
export DATABASE_PASSWORD=platform

# Run with prod profile
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

Or build and run JAR:

```bash
mvn clean package
java -jar target/platform-api-1.0-SNAPSHOT.jar
```

## API Endpoints

### POST /api/execution/plan

Creates execution plan for the specified action and saves it to database.

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

Gets plan by identifier from database.

**Response (200 OK):**
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

**Errors:**

- `404 Not Found` - plan with specified ID not found

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

## Database Structure

### Main Tables

- **entity_types** - entity types (Building, Contract, etc.)
- **actions** - actions (order_egrn_extract, close_contract, etc.)
- **ui_bindings** - action bindings to UI elements
- **plans** - execution plans
- **plan_steps** - plan steps
- **execution_results** - plan execution results
- **execution_log_entries** - execution log entries

### Relationships

- `actions` → `action_applicable_entity_types` (many-to-many with entity_types)
- `plans` → `plan_steps` (one-to-many)
- `execution_results` → `execution_log_entries` (one-to-many)

## Preconfigured Data

On first startup, Flyway migrations create initial data:

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
├── src/main/java/com/zaborstik/platform/api/
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
│   ├── entity/
│   │   ├── EntityTypeEntity.java     # JPA Entity for EntityType
│   │   ├── ActionEntity.java         # JPA Entity for Action
│   │   ├── UIBindingEntity.java      # JPA Entity for UIBinding
│   │   ├── PlanEntity.java          # JPA Entity for Plan
│   │   ├── PlanStepEntity.java       # JPA Entity for PlanStep
│   │   ├── ExecutionResultEntity.java # JPA Entity for ExecutionResult
│   │   └── ExecutionLogEntryEntity.java # JPA Entity for ExecutionLogEntry
│   ├── repository/
│   │   ├── EntityTypeRepository.java  # Spring Data JPA repository
│   │   ├── ActionRepository.java      # Spring Data JPA repository
│   │   ├── UIBindingRepository.java   # Spring Data JPA repository
│   │   ├── PlanRepository.java        # Spring Data JPA repository
│   │   └── ExecutionResultRepository.java # Spring Data JPA repository
│   ├── resolver/
│   │   └── DatabaseResolver.java      # Resolver implementation via DB
│   ├── mapper/
│   │   └── PlanMapper.java           # Mapper Plan ↔ PlanEntity
│   ├── config/
│   │   └── PlatformConfiguration.java # Spring configuration
│   └── exception/
│       └── GlobalExceptionHandler.java # Exception handler
└── src/main/resources/
    ├── application.properties         # Main configuration
    ├── application-dev.properties     # Configuration for dev (H2)
    ├── application-prod.properties    # Configuration for prod (PostgreSQL)
    └── db/migration/
        ├── V1__Create_base_tables.sql # Migration for creating tables
        └── V2__Insert_initial_data.sql # Migration for initial data
```

## Testing

### Running All Tests

```bash
mvn test
```

### Test Types

- **Unit tests** - isolated component tests with mocks
- **Integration tests** - tests with real database (H2 in-memory)
- **Repository tests** - repository tests with @DataJpaTest
- **Controller tests** - REST API tests with MockMvc

### Test Profiles

Tests use H2 in-memory database automatically through `@DataJpaTest` or `@SpringBootTest`.

## Extending Functionality

### Adding New EntityType/Action/UIBinding

1. Add data through Flyway SQL migration
2. Or use repositories programmatically:

```java
@Autowired
private EntityTypeRepository entityTypeRepository;

EntityTypeEntity entity = new EntityTypeEntity("NewType", "New Type", Map.of());
entityTypeRepository.save(entity);
```

### Database Connection Configuration

Edit `application-prod.properties` or set environment variables:
- `DATABASE_URL` - PostgreSQL connection URL
- `DATABASE_USERNAME` - username
- `DATABASE_PASSWORD` - password

### Creating New Migrations

1. Create file in `src/main/resources/db/migration/`
2. File name: `V{number}__{description}.sql`
3. Example: `V3__Add_new_table.sql`

## Dependencies

- Spring Boot 3.2.1
- Spring Web
- Spring Data JPA
- Spring Validation
- Hibernate (JPA implementation)
- H2 Database (for development)
- PostgreSQL Driver (for production)
- Flyway (for migrations)
- Jackson (for JSON)
- platform-core (platform core)

## Monitoring and Debugging

### H2 Console (dev only)

When running with `dev` profile, H2 Console is available at:
http://localhost:8080/h2-console

Connection settings:
- JDBC URL: `jdbc:h2:mem:platformdb`
- User Name: `sa`
- Password: (empty)

### SQL Logging

In dev profile SQL queries are logged automatically. For prod disable:
```properties
spring.jpa.show-sql=false
```

## Production Deployment

### Requirements

- PostgreSQL 12+
- Java 21+
- Minimum 512MB RAM

### Deployment Steps

1. Create PostgreSQL database:
   ```sql
   CREATE DATABASE platformdb;
   CREATE USER platform WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE platformdb TO platform;
   ```

2. Set environment variables:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export DATABASE_URL=jdbc:postgresql://localhost:5432/platformdb
   export DATABASE_USERNAME=platform
   export DATABASE_PASSWORD=your_password
   ```

3. Run application:
   ```bash
   java -jar platform-api-1.0-SNAPSHOT.jar
   ```

4. Flyway will automatically apply migrations on first startup.

## Troubleshooting

### Issue: Migrations not applied

**Solution:** Check that Flyway is enabled in `application.properties`:
```properties
spring.flyway.enabled=true
```

### Issue: PostgreSQL connection error

**Solution:** Check:
- PostgreSQL is running
- Correct URL, username, password
- Database accessibility from network (if not localhost)

### Issue: H2 Console not opening

**Solution:** Make sure you're using `dev` profile:
```properties
spring.profiles.active=dev
spring.h2.console.enabled=true
```
