# Build and Test Instructions

This document describes how to compile and test all components of the execution platform.

## Contents

1. [Requirements](#requirements)
2. [Project Compilation](#project-compilation)
3. [Component Testing](#component-testing)
   - [Core Testing (platform-core)](#core-testing-platform-core)
   - [API Testing (platform-api)](#api-testing-platform-api)
   - [Agent Testing (platform-agent)](#agent-testing-platform-agent)
   - [Executor Testing (platform-executor)](#executor-testing-platform-executor)
4. [Minimal Work Verification](#minimal-work-verification)
5. [Integration Testing](#integration-testing)

---

## Requirements

### Required

- **Java 21** (JDK 21)
- **Maven 3.6+**
- **Node.js 18+** (for Playwright server)
- **npm** or **yarn**

### Installation Check

```bash
# Check Java
java -version
# Should be: openjdk version "21" or higher

# Check Maven
mvn -version
# Should be: Apache Maven 3.6.x or higher

# Check Node.js
node -v
# Should be: v18.x.x or higher

npm -v
# Should be: 9.x.x or higher
```

---

## Project Compilation

### Compile Entire Project

From project root directory:

```bash
# Clean and compile all modules
mvn clean compile

# Compile with tests skipped (faster)
mvn clean compile -DskipTests

# Full build (compile + tests + package)
mvn clean package

# Install to local Maven repository
mvn clean install
```

### Compile Individual Modules

```bash
# Compile only core
cd platform-core
mvn clean compile

# Compile only API
cd platform-api
mvn clean compile

# Compile only agent
cd platform-agent
mvn clean compile

# Compile only executor
cd platform-executor
mvn clean compile
```

### Build JAR Files

```bash
# Build entire project
mvn clean package

# JAR files will be in:
# - platform-api/target/platform-api-1.0-SNAPSHOT.jar
# - platform-agent/target/platform-agent-1.0-SNAPSHOT.jar
# - platform-executor/target/platform-executor-1.0-SNAPSHOT.jar
```

---

## Component Testing

### Core Testing (platform-core)

**Core** contains basic abstractions and plan building logic. This is the most important component for testing.

#### Run All Core Tests

```bash
cd platform-core
mvn test
```

#### Run Specific Test

```bash
# ExecutionEngine test
mvn test -Dtest=ExecutionEngineTest

# Specific test method
mvn test -Dtest=ExecutionEngineTest#shouldCreatePlanSuccessfully
```

#### What's Tested

- ✅ `ExecutionEngine` - execution plan creation
- ✅ `Planner` - linear plan building
- ✅ `InMemoryResolver` - finding EntityType, Action, UIBinding
- ✅ Domain models (`EntityType`, `Action`, `UIBinding`, `Plan`, `PlanStep`)
- ✅ Validation and error handling

#### Test Examples

```bash
# Plan creation test
mvn test -Dtest=ExecutionEngineTest#shouldCreatePlanSuccessfully

# Error handling test
mvn test -Dtest=ExecutionEngineTest#shouldThrowExceptionWhenEntityTypeNotFound

# Plan structure test
mvn test -Dtest=ExecutionEngineTest#shouldCreatePlanWithCorrectSteps
```

#### Run Usage Example

```bash
cd platform-core
mvn exec:java -Dexec.mainClass="com.zaborstik.platform.core.example.ExampleUsage"
```

This example:
1. Registers EntityType, Action, UIBinding
2. Creates ExecutionRequest
3. Builds Plan through ExecutionEngine
4. Prints plan to console

---

### API Testing (platform-api)

**API** provides REST interface for working with execution engine.

#### Run All API Tests

```bash
cd platform-api
mvn test
```

#### API Test Types

1. **Unit tests** (fast, isolated):
   ```bash
   # Controller test
   mvn test -Dtest=ExecutionControllerTest
   
   # Service test
   mvn test -Dtest=ExecutionServiceTest
   
   # Error handler test
   mvn test -Dtest=GlobalExceptionHandlerTest
   
   # Configuration test
   mvn test -Dtest=PlatformConfigurationTest
   
   # DTO serialization test
   mvn test -Dtest=DTOsSerializationTest
   ```

2. **Integration tests** (full stack):
   ```bash
   mvn test -Dtest=ExecutionIntegrationTest
   ```

#### What's Tested

- ✅ REST API endpoints (`POST /api/execution/plan`)
- ✅ Input data validation
- ✅ Error handling
- ✅ DTO ↔ domain objects conversion
- ✅ Spring configuration (Resolver, ExecutionEngine)
- ✅ Full plan creation cycle through API

#### Start API Server for Manual Testing

```bash
cd platform-api
mvn spring-boot:run
```

API will be available at `http://localhost:8080`

**Testing via cURL:**

```bash
# Create plan
curl -X POST http://localhost:8080/api/execution/plan \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "Building",
    "entityId": "93939",
    "action": "order_egrn_extract",
    "parameters": {}
  }'
```

**Expected response:**
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
    ...
  ]
}
```

---

### Agent Testing (platform-agent)

**Agent** executes plans through UI using Playwright.

#### Playwright Server Setup

Before testing agent, you need to install dependencies and start Playwright server:

```bash
cd platform-agent/src/main/resources

# Install Node.js dependencies
npm install

# Start Playwright server (in separate terminal)
node playwright-server.js

# Or in headless mode
node playwright-server.js --headless

# With custom port
PORT=3001 node playwright-server.js
```

Server will be available at `http://localhost:3000` (or specified port).

**Server availability check:**

```bash
curl http://localhost:3000/health
```

Expected response:
```json
{"status":"ok","browser":false}
```

#### Run Agent Tests

```bash
cd platform-agent
mvn test
```

**Note:** Agent tests require running Playwright server. If server is not running, tests may fail.

#### Run Agent Usage Example

```bash
cd platform-agent

# Make sure Playwright server is running on localhost:3000
# Then run example:
mvn exec:java -Dexec.mainClass="com.zaborstik.platform.agent.example.AgentExample"
```

This example:
1. Configures Resolver with test data
2. Creates plan through ExecutionEngine
3. Initializes browser through AgentService
4. Executes plan step by step
5. Prints execution results

**Important:** For example to work, you need running Playwright server and accessible web application at `http://localhost:8080` (or change URL in code).

---

### Executor Testing (platform-executor)

**Executor** orchestrates plan execution and collects execution log.

#### Run Executor Tests

```bash
cd platform-executor
mvn test
```

**Note:** Executor depends on platform-agent, so for full testing may require running Playwright server.

#### Run Executor Usage Example

```bash
cd platform-executor

# Make sure Playwright server is running
# Then run example:
mvn exec:java -Dexec.mainClass="com.zaborstik.platform.Main"
```

This example:
1. Creates plan through ExecutionEngine
2. Configures AgentService
3. Executes plan through PlanExecutor
4. Prints execution log with results

---

## Minimal Work Verification

### Quick Check (without UI)

This method checks core and API work without need to start browser.

#### Step 1: Project Compilation

```bash
# From project root
mvn clean compile
```

#### Step 2: Run Core Tests

```bash
cd platform-core
mvn test
```

**Expected result:** All tests should pass successfully.

#### Step 3: Run API Tests

```bash
cd platform-api
mvn test
```

**Expected result:** All tests should pass successfully.

#### Step 4: Start API and Check via REST

```bash
# Terminal 1: Start API
cd platform-api
mvn spring-boot:run

# Terminal 2: Check API
curl -X POST http://localhost:8080/api/execution/plan \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "Building",
    "entityId": "93939",
    "action": "order_egrn_extract",
    "parameters": {}
  }'
```

**Expected result:** JSON with execution plan (5 steps).

---

### Full Check (with UI)

This method checks entire stack, including execution through browser.

#### Step 1: Install Playwright Dependencies

```bash
cd platform-agent/src/main/resources
npm install
```

#### Step 2: Start Playwright Server

```bash
# Terminal 1: Start Playwright server
cd platform-agent/src/main/resources
node playwright-server.js
```

#### Step 3: Start API

```bash
# Terminal 2: Start API
cd platform-api
mvn spring-boot:run
```

#### Step 4: Run Agent Example

```bash
# Terminal 3: Run agent example
cd platform-agent
mvn exec:java -Dexec.mainClass="com.zaborstik.platform.agent.example.AgentExample"
```

**Expected result:**
- Browser will open (if not headless)
- Plan will be executed step by step
- Console will show results of each step

---

## Integration Testing

### Full Cycle: API → Engine → Executor → Agent

#### Setup

1. **Start Playwright server:**
   ```bash
   cd platform-agent/src/main/resources
   node playwright-server.js
   ```

2. **Start API:**
   ```bash
   cd platform-api
   mvn spring-boot:run
   ```

#### Testing via API

```bash
# 1. Create plan via API
curl -X POST http://localhost:8080/api/execution/plan \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "Building",
    "entityId": "93939",
    "action": "order_egrn_extract",
    "parameters": {}
  }' > plan.json

# 2. Check plan structure
cat plan.json | jq '.steps | length'
# Should be: 5

cat plan.json | jq '.steps[].type'
# Should be: ["open_page", "explain", "hover", "click", "wait"]
```

#### Testing Plan Execution

To execute plan through executor, you can use example from `platform-executor` or write your own test.

---

## Troubleshooting

### Problem: Maven can't find dependencies

**Solution:**
```bash
# Clean and rebuild
mvn clean install -DskipTests
```

### Problem: Tests fail with "EntityType not found" error

**Cause:** Resolver not configured with test data.

**Solution:** Make sure tests use `PlatformConfiguration` or configure `InMemoryResolver` manually.

### Problem: Playwright server doesn't start

**Causes:**
- Dependencies not installed: `npm install` in `platform-agent/src/main/resources`
- Port busy: change port via `PORT=3001 node playwright-server.js`
- Node.js version too old: requires Node.js 18+

**Solution:**
```bash
cd platform-agent/src/main/resources
npm install
node playwright-server.js
```

### Problem: Browser doesn't open

**Cause:** Playwright requires browser installation.

**Solution:**
```bash
cd platform-agent/src/main/resources
npx playwright install chromium
```

### Problem: API doesn't start

**Causes:**
- Port 8080 busy: change `server.port` in `application.properties`
- Wrong Java version: requires Java 21

**Solution:**
```bash
# Check Java version
java -version

# Change port in application.properties
# server.port=8081
```

---

## Useful Commands

### View Project Structure

```bash
# Module tree
mvn dependency:tree

# List all tests
find . -name "*Test.java" -type f
```

### Clean Project

```bash
# Clean all target directories
mvn clean

# Clean + remove installed artifacts
mvn clean install -DskipTests
```

### Run with Verbose Output

```bash
# Verbose Maven output
mvn test -X

# Errors only output
mvn test -q
```

### Code Coverage (requires JaCoCo setup)

```bash
# Add JaCoCo plugin to pom.xml, then:
mvn test jacoco:report
# Report will be in target/site/jacoco/index.html
```

---

## Summary

### Minimal Work Check (5 minutes)

```bash
# 1. Compilation
mvn clean compile

# 2. Core tests
cd platform-core && mvn test && cd ..

# 3. API tests
cd platform-api && mvn test && cd ..
```

### Full Check (15 minutes)

```bash
# 1. Compilation
mvn clean package

# 2. Install Playwright dependencies
cd platform-agent/src/main/resources && npm install && cd ../../../../..

# 3. Start Playwright server (in separate terminal)
cd platform-agent/src/main/resources
node playwright-server.js

# 4. Start API (in separate terminal)
cd platform-api
mvn spring-boot:run

# 5. Test via cURL or examples
```

---

## Additional Resources

- [README platform-core](../platform-core/README.md) - core documentation
- [README platform-api](../platform-api/README.md) - API documentation
- [README platform-agent](../platform-agent/README.md) - agent documentation
- [README platform-executor](../platform-executor/README.md) - executor documentation
- [TESTING.md](../platform-api/TESTING.md) - detailed API test documentation

