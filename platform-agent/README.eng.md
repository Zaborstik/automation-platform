# Platform Agent

UI agent for executing plans through browser using Playwright.

## Architecture

Platform Agent consists of two parts:

1. **Java components** (`platform-agent`):
   - `AgentClient` - HTTP client for interacting with Playwright server
   - `AgentService` - service for executing plans through agent
   - DTOs for commands and results

2. **Node.js server** (`playwright-server.js`):
   - HTTP API for managing Playwright
   - Action visualization (highlighting, smooth movements)
   - Video and screenshot recording

## Installation

### 1. Install Node.js Dependencies

```bash
cd platform-agent/src/main/resources
npm install
```

### 2. Start Playwright Server

```bash
# In visible mode (default)
node playwright-server.js

# In headless mode
node playwright-server.js --headless

# With custom port
PORT=3001 node playwright-server.js
```

Server will be available at `http://localhost:3000` (or specified port).

## Usage

### Basic Example

```java
import org.example.agent.client.AgentClient;
import org.example.agent.service.AgentService;
import org.example.core.resolver.InMemoryResolver;
import org.example.core.plan.Plan;

// Create client
AgentClient client = new AgentClient("http://localhost:3000");

// Create service
Resolver resolver = new InMemoryResolver();
AgentService agentService = new AgentService(
    client, 
    resolver, 
    "http://localhost:8080",  // application base URL
    false  // headless mode
);

// Execute plan
Plan plan = ...; // obtained from ExecutionEngine
List<StepExecutionResult> results = agentService.executePlan(plan);

// Close browser
agentService.close();
```

### Agent Commands

Agent supports the following commands:

- **OPEN_PAGE** - open page
- **CLICK** - click element
- **HOVER** - hover over element
- **TYPE** - type text
- **WAIT** - wait (by selector or networkidle)
- **EXPLAIN** - log action explanation
- **HIGHLIGHT** - highlight element
- **SCREENSHOT** - take screenshot

### PlanStep to Commands Conversion

`AgentService` automatically converts `PlanStep` to `AgentCommand`:

- `open_page` → `OPEN_PAGE`
- `click` → `CLICK` (with `action(actionId)` resolution through Resolver)
- `hover` → `HOVER` (with `action(actionId)` resolution through Resolver)
- `type` → `TYPE`
- `wait` → `WAIT`
- `explain` → `EXPLAIN`

## Visualization

Playwright server provides:

1. **Smooth mouse movement** - cursor smoothly moves to target element
2. **Element highlighting** - elements are highlighted with red border before action
3. **Action slowdown** - `slowMo: 100ms` for better visualization
4. **Screenshots** - automatic screenshots after each action
5. **Video** - execution video recording (optional)

## API Endpoints

### `GET /health`
Check server availability.

**Response:**
```json
{
  "status": "ok",
  "browser": true
}
```

### `POST /initialize`
Initialize browser.

**Request body:**
```json
{
  "baseUrl": "http://localhost:8080",
  "headless": false
}
```

**Response:**
```json
{
  "success": true,
  "message": "Browser initialized",
  "data": {
    "baseUrl": "http://localhost:8080",
    "headless": false
  },
  "executionTimeMs": 0
}
```

### `POST /execute`
Execute command.

**Request body:**
```json
{
  "type": "CLICK",
  "target": "#button-id",
  "explanation": "Clicking button",
  "parameters": {}
}
```

**Response:**
```json
{
  "success": true,
  "message": "Clicking button",
  "data": {
    "selector": "#button-id",
    "screenshot": "/path/to/screenshot.png"
  },
  "executionTimeMs": 250
}
```

### `POST /close`
Close browser.

## Configuration

### Environment Variables

- `PORT` - server port (default 3000)
- `HEADLESS` - run in headless mode (`true`/`false`)
- `SCREENSHOTS_DIR` - screenshots directory

### AgentClient Parameters

```java
// With default timeout
AgentClient client = new AgentClient("http://localhost:3000");

// With custom timeout
AgentClient client = new AgentClient(
    "http://localhost:3000", 
    Duration.ofMinutes(5)
);
```

## Platform Integration

Agent integrates with other platform components:

1. **ExecutionEngine** creates `Plan` from `ExecutionRequest`
2. **AgentService** receives `Plan` and executes it through `AgentClient`
3. **AgentClient** sends commands to Playwright server
4. **Playwright server** executes actions in browser

## Error Handling

- If element not found, agent returns error with description
- If browser not initialized, commands return error
- All errors are logged and returned in `StepExecutionResult`

## Future Improvements

- [ ] Retry mechanism for failed steps
- [ ] Fallback to vision-based element search
- [ ] Support for more complex selectors
- [ ] Integration with platform logging system
- [ ] Performance metrics

