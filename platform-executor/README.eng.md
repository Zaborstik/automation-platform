# Platform Executor

Plan execution executor. Takes `Plan` from `platform-core`, converts its steps to UI commands through `platform-agent`, manages agent and collects `execution_log` with execution results.

## Architecture

Platform Executor is in the middle of execution pipeline:

```
[ ExecutionEngine (core) ]
         |
         v
    [ Plan ]
         |
         v
[ PlanExecutor (executor) ]
         |
         v
[ AgentService (agent) ]
         |
         v
[ UI Agent (Playwright) ]
```

### Executor Role

Executor **doesn't know**:
- How to build plans (this is done by `ExecutionEngine`)
- How to work with UI directly (this is done by `AgentService`)

Executor **knows**:
- How to orchestrate plan execution
- How to collect execution_log
- How to aggregate execution results

## Components

### 1. PlanExecutor

Main executor class. Accepts `Plan`, passes it to `AgentService`, collects results and forms `PlanExecutionResult`.

**Main methods:**

```java
PlanExecutionResult execute(Plan plan)
```

Executes plan synchronously and returns aggregated result with execution_log.

### 2. ExecutionLogEntry

One entry in execution_log. Links:
- Plan step (`PlanStep`) - what was planned to execute
- Execution result (`StepExecutionResult`) - what actually happened
- Metadata (planId, stepIndex, timestamp)

**Structure:**

```java
ExecutionLogEntry {
    String planId;           // Plan ID
    int stepIndex;           // Step index in plan
    PlanStep step;           // Plan step (what was planned)
    StepExecutionResult result; // Execution result (what happened)
    Instant loggedAt;        // Logging time
}
```

### 3. PlanExecutionResult

Aggregated plan execution result. Contains:
- Execution status (success/failure)
- Timestamps (startedAt, finishedAt)
- Full execution_log (list of `ExecutionLogEntry`)

**Structure:**

```java
PlanExecutionResult {
    String planId;                    // Plan ID
    boolean success;                  // Overall status (true if all steps successful)
    Instant startedAt;                // Execution start time
    Instant finishedAt;               // Execution finish time
    List<ExecutionLogEntry> logEntries; // Execution log
}
```

## How It Works

### Execution Flow

1. **Input**: `PlanExecutor.execute(Plan plan)`
   - Plan is already created by `ExecutionEngine` and contains list of `PlanStep`

2. **Orchestration**: `PlanExecutor` passes plan to `AgentService`
   ```java
   List<StepExecutionResult> results = agentService.executePlan(plan);
   ```

3. **Execution**: `AgentService` executes each step through UI agent
   - Converts `PlanStep` → `AgentCommand`
   - Sends commands to Playwright server
   - Receives execution results

4. **Log Assembly**: `PlanExecutor` links steps with results
   ```java
   for (int i = 0; i < steps.size(); i++) {
       ExecutionLogEntry entry = new ExecutionLogEntry(
           plan.getId(),
           i,
           steps.get(i),
           results.get(i),
           Instant.now()
       );
       logEntries.add(entry);
   }
   ```

5. **Aggregation**: `PlanExecutionResult` is formed
   - Checks success of all steps
   - Calculates total execution time
   - Returns result with full execution_log

### Execution Log

Execution log is observable trace of plan execution. Each entry contains:

- **What was planned**: `PlanStep` with type, target element and explanation
- **What happened**: `StepExecutionResult` with success, message, error, execution time and screenshot
- **Context**: plan ID, step index, timestamp

**Example execution_log:**

```
Plan: 550e8400-e29b-41d4-a716-446655440000
Started: 2024-01-15T10:30:00Z

[0] open_page /buildings/93939
    -> SUCCESS: Page opened in 1200ms
    -> Screenshot: /screenshots/plan-xxx-step-0.png

[1] explain "Opening building card"
    -> SUCCESS: Message logged in 5ms

[2] hover action(order_egrn_extract)
    -> SUCCESS: Element hovered in 150ms
    -> Screenshot: /screenshots/plan-xxx-step-2.png

[3] click action(order_egrn_extract)
    -> SUCCESS: Element clicked in 200ms
    -> Screenshot: /screenshots/plan-xxx-step-3.png

[4] wait result
    -> SUCCESS: Condition met in 3000ms
    -> Screenshot: /screenshots/plan-xxx-step-4.png

Finished: 2024-01-15T10:30:05Z
Status: SUCCESS
Total time: 4555ms
```

## Usage

### Basic Example

```java
import org.example.executor.PlanExecutor;
import org.example.executor.PlanExecutionResult;
import org.example.agent.client.AgentClient;
import org.example.agent.service.AgentService;
import org.example.core.plan.Plan;
import org.example.core.resolver.InMemoryResolver;

// 1. Configure dependencies
InMemoryResolver resolver = new InMemoryResolver();
// ... register EntityType, Action, UIBinding ...

AgentClient agentClient = new AgentClient("http://localhost:3000");
AgentService agentService = new AgentService(
    agentClient,
    resolver,
    "http://localhost:8080",  // application base URL
    false  // headless mode
);

// 2. Create executor
PlanExecutor executor = new PlanExecutor(agentService);

// 3. Get plan (from ExecutionEngine or create manually)
Plan plan = ...; // created via ExecutionEngine.createPlan(request)

// 4. Execute plan
PlanExecutionResult result = executor.execute(plan);

// 5. Analyze results
System.out.println("Plan " + result.getPlanId() + " executed: " + 
    (result.isSuccess() ? "SUCCESS" : "FAILED"));
System.out.println("Execution time: " + 
    Duration.between(result.getStartedAt(), result.getFinishedAt()).toMillis() + "ms");

// 6. View execution log
result.getLogEntries().forEach(entry -> {
    System.out.printf(
        "[%d] %s -> %s (%.0fms)%n",
        entry.getStepIndex(),
        entry.getStep().getType(),
        entry.getResult().isSuccess() ? "SUCCESS" : "FAILED",
        (double) entry.getResult().getExecutionTimeMs()
    );
    if (!entry.getResult().isSuccess()) {
        System.out.println("  Error: " + entry.getResult().getError());
    }
});
```

### Integration with ExecutionEngine

```java
import org.example.core.ExecutionEngine;
import org.example.core.execution.ExecutionRequest;
import org.example.executor.PlanExecutor;

// 1. Create plan through ExecutionEngine
ExecutionEngine engine = new ExecutionEngine(resolver);
ExecutionRequest request = new ExecutionRequest(
    "Building",
    "93939",
    "order_egrn_extract",
    Map.of()
);
Plan plan = engine.createPlan(request);

// 2. Execute plan through PlanExecutor
PlanExecutor executor = new PlanExecutor(agentService);
PlanExecutionResult result = executor.execute(plan);

// 3. Use result
if (result.isSuccess()) {
    // Action executed successfully
    // Can save execution_log to DB for audit
} else {
    // Error handling
    result.getLogEntries().stream()
        .filter(e -> !e.getResult().isSuccess())
        .forEach(e -> {
            log.error("Step {} failed: {}", 
                e.getStepIndex(), 
                e.getResult().getError()
            );
        });
}
```

## Integration with Other Modules

### Dependencies

Executor depends on:
- **platform-core**: uses `Plan`, `PlanStep` for input data
- **platform-agent**: uses `AgentService` for executing plans

Executor **doesn't depend** on:
- **platform-api**: API can use executor, but not vice versa

### Typical Flow in System

```
1. Client → POST /api/execution/plan
   ↓
2. ExecutionController → ExecutionService
   ↓
3. ExecutionService → ExecutionEngine.createPlan(request)
   ↓
4. ExecutionEngine → Planner → Plan
   ↓
5. ExecutionService → PlanExecutor.execute(plan)
   ↓
6. PlanExecutor → AgentService.executePlan(plan)
   ↓
7. AgentService → AgentClient → Playwright Server
   ↓
8. PlanExecutor ← StepExecutionResult[]
   ↓
9. PlanExecutor → PlanExecutionResult (with execution_log)
   ↓
10. ExecutionService ← PlanExecutionResult
    ↓
11. Client ← PlanExecutionResult (through API)
```

## Execution Log as Source of Truth

Execution log is **observable trace** of plan execution. It allows:

1. **Audit**: understand what actually happened during action execution
2. **Debugging**: find step where error occurred
3. **Reproduction**: repeat execution with same steps
4. **Analytics**: collect metrics about performance and action success

### Log Format

Each entry in execution_log contains:

```java
{
    "planId": "550e8400-e29b-41d4-a716-446655440000",
    "stepIndex": 2,
    "step": {
        "type": "click",
        "target": "action(order_egrn_extract)",
        "explanation": "Executing action 'Order EGRN Extract'",
        "parameters": {}
    },
    "result": {
        "stepType": "click",
        "stepTarget": "button[data-action='order_egrn_extract']",
        "success": true,
        "message": "Element clicked successfully",
        "error": null,
        "executedAt": "2024-01-15T10:30:03.123Z",
        "executionTimeMs": 200,
        "screenshotPath": "/screenshots/plan-xxx-step-2.png"
    },
    "loggedAt": "2024-01-15T10:30:03.125Z"
}
```

## Error Handling

### Partial Execution

If one of plan steps failed:

1. `PlanExecutor` continues executing remaining steps
2. Entry with `success: false` is added to execution_log
3. `PlanExecutionResult.success` will be `false` if at least one step failed

### Step and Result Mismatch

If `AgentService` returned fewer results than steps in plan:

- `PlanExecutor` creates synthetic failure entries for missing steps
- This guarantees that execution_log always matches plan

### Agent Initialization

If browser initialization failed:

- `AgentService` returns failure result for first step
- `PlanExecutor` doesn't execute remaining steps
- `PlanExecutionResult.success` will be `false`

## Future Improvements

- [ ] Asynchronous plan execution
- [ ] Retry mechanism for failed steps
- [ ] Saving execution_log to DB
- [ ] Performance metrics (Prometheus)
- [ ] Integration with notification system
- [ ] Support for plan execution cancellation
- [ ] Parallel execution of independent steps

## Principles

1. **Executor doesn't know UI**: it works only with abstractions (`Plan`, `PlanStep`)
2. **Execution log - source of truth**: full observable trace of execution
3. **Determinism**: same plan → same execution_log (under same conditions)
4. **Separation of concerns**: executor orchestrates, agent executes

