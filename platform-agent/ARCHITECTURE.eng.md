# Platform Agent Architecture

## Overview

Platform Agent is a bridge between Java platform and browser through Playwright. It provides execution of action plans through UI with visualization and observability.

## Components

### Java Components

#### 1. DTO (`org.example.agent.dto`)

- **AgentCommand** - command for agent execution
  - Types: OPEN_PAGE, CLICK, TYPE, HOVER, WAIT, EXPLAIN, HIGHLIGHT, SCREENSHOT
  - Contains target, explanation, parameters

- **AgentResponse** - response from agent
  - success/failure status
  - message/error
  - data (execution results)
  - executionTimeMs

- **StepExecutionResult** - plan step execution result
  - Step information
  - Success/error
  - Screenshots
  - Execution time

#### 2. Client (`org.example.agent.client`)

- **AgentClient** - HTTP client for interacting with Playwright server
  - `execute(AgentCommand)` - execute command
  - `initialize(baseUrl, headless)` - initialize browser
  - `close()` - close browser
  - `isAvailable()` - check availability

- **AgentException** - exception when working with agent

#### 3. Service (`org.example.agent.service`)

- **AgentService** - service for executing plans
  - `executePlan(Plan)` - execute plan completely
  - Converts `PlanStep` to `AgentCommand`
  - Resolves `action(actionId)` through `Resolver`
  - Collects execution results

#### 4. Config (`org.example.agent.config`)

- **AgentConfiguration** - agent configuration
  - Creates `AgentClient` and `AgentService`
  - Checks agent availability
  - Settings: URL, timeout, headless mode

### Node.js Components

#### Playwright Server (`playwright-server.js`)

HTTP server on Express, providing API for managing Playwright:

**Endpoints:**
- `GET /health` - check availability
- `POST /initialize` - initialize browser
- `POST /execute` - execute command
- `POST /close` - close browser

**Features:**
- Smooth mouse movement (smoothMove)
- Element highlighting (highlightElement)
- Automatic screenshots
- Video recording (optional)
- Action slowdown for visualization

## Execution Flow

```
ExecutionRequest
    ↓
ExecutionEngine.createPlan()
    ↓
Plan (with PlanStep[])
    ↓
AgentService.executePlan()
    ↓
For each PlanStep:
    convertToCommand() → AgentCommand
    ↓
AgentClient.execute()
    ↓
HTTP POST /execute → Playwright Server
    ↓
Playwright executes action in browser
    ↓
AgentResponse
    ↓
StepExecutionResult
```

## PlanStep → AgentCommand Conversion

| PlanStep.type | AgentCommand.type | Features |
|--------------|-------------------|----------|
| `open_page` | `OPEN_PAGE` | Direct conversion |
| `click` | `CLICK` | Resolves `action(actionId)` through Resolver |
| `hover` | `HOVER` | Resolves `action(actionId)` through Resolver |
| `type` | `TYPE` | Extracts `text` from parameters |
| `wait` | `WAIT` | Extracts `timeout` from parameters |
| `explain` | `EXPLAIN` | Logs explanation |

## Resolving action(actionId)

When `PlanStep.target` has format `action(actionId)`:

1. `AgentService` extracts `actionId`
2. Calls `resolver.findUIBinding(actionId)`
3. Gets `selector` from `UIBinding`
4. Uses `selector` in `AgentCommand`

This allows platform to work with semantic actions, not coordinates.

## Visualization

Playwright server provides:

1. **Smooth mouse movement** - cursor smoothly moves to target element (easing function)
2. **Highlighting** - elements are highlighted with red border before action
3. **Slowdown** - `slowMo: 100ms` for better visualization
4. **Screenshots** - automatic screenshots after each action
5. **Video** - execution video recording (optional)

## Error Handling

- If element not found → error with description
- If browser not initialized → error with instruction
- All errors are logged and returned in `StepExecutionResult`
- Retry mechanism can be added in the future

## Integration

Agent integrates with:

- **platform-core** - uses `Plan`, `PlanStep`, `Resolver`, `UIBinding`
- **platform-api** - can be used in `ExecutionService` for executing plans
- **platform-executor** - can be part of executor module

## Future Improvements

- [ ] Retry mechanism for failed steps
- [ ] Fallback to vision-based element search
- [ ] Support for more complex selectors (XPath, text-based)
- [ ] Integration with platform logging system
- [ ] Performance metrics
- [ ] Support for parallel execution of multiple plans
- [ ] Configurable visualization strategies

