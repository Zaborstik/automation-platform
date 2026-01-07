# How Spring Works in Our Application

## 🎯 Core Concepts

### What is a Bean?

**Bean** is an object managed by Spring. Spring creates it, stores it in its container (ApplicationContext) and can pass it to other objects.

**Analogy:** Think of Spring as a factory, and Bean as parts that the factory produces and assembles into a finished product.

### What is Dependency Injection (DI)?

**Dependency Injection** is when Spring automatically passes dependencies (other beans) to constructor or class fields.

**Without Spring:**
```java
// You create all objects yourself
Resolver resolver = new InMemoryResolver();
ExecutionEngine engine = new ExecutionEngine(resolver);
ExecutionService service = new ExecutionService(engine);
ExecutionController controller = new ExecutionController(service);
```

**With Spring:**
```java
// Spring creates and links all objects itself
@RestController
public class ExecutionController {
    // Spring will automatically pass ExecutionService to constructor
    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }
}
```

---

## 🏗️ Our Application Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Application Context                │
│                    (Container for all beans)                 │
│                                                              │
│  ┌──────────────┐      ┌──────────────┐                    │
│  │   Resolver   │─────▶│ExecutionEngine│                    │
│  │   (Bean)     │      │    (Bean)     │                    │
│  └──────────────┘      └───────┬───────┘                    │
│                                │                             │
│                                ▼                             │
│                        ┌──────────────┐                     │
│                        │ExecutionService│                    │
│                        │    (Bean)     │                     │
│                        └───────┬───────┘                     │
│                                │                             │
│                                ▼                             │
│                        ┌──────────────┐                     │
│                        │ExecutionController│                 │
│                        │    (Bean)     │                     │
│                        └──────────────┘                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 Component Breakdown

### 1. Main Application Class

```java
@SpringBootApplication
public class PlatformApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }
}
```

**What happens:**

1. `@SpringBootApplication` is a "magic" annotation that includes:
   - `@Configuration` — tells Spring this is a configuration class
   - `@EnableAutoConfiguration` — enables Spring Boot auto-configuration
   - `@ComponentScan` — scans package and subpackages for components

2. `SpringApplication.run()` starts Spring:
   - Scans all classes in `com.zaborstik.platform.api` package and subpackages
   - Looks for classes with annotations: `@Component`, `@Service`, `@Controller`, `@RestController`, `@Configuration`, `@Bean`
   - Creates these objects (beans)
   - Links them through Dependency Injection
   - Starts embedded Tomcat server

---

### 2. Configuration (PlatformConfiguration)

```java
@Configuration
public class PlatformConfiguration {

    @Bean
    public Resolver resolver() {
        InMemoryResolver resolver = new InMemoryResolver();
        // ... fill with data
        return resolver;
    }

    @Bean
    public ExecutionEngine executionEngine(Resolver resolver) {
        return new ExecutionEngine(resolver);
    }
}
```

**What is `@Configuration`?**
- Tells Spring: "This class contains instructions on how to create beans"

**What is `@Bean`?**
- Tells Spring: "This method creates a bean"
- Spring will call this method and save result to container
- Bean name = method name (in our case `resolver` and `executionEngine`)

**How does Dependency Injection work here?**

```java
@Bean
public ExecutionEngine executionEngine(Resolver resolver) {
    //                                    ^^^^^^^^
    // Spring sees Resolver parameter and thinks:
    // "I have a bean named 'resolver', I'll pass it here!"
    return new ExecutionEngine(resolver);
}
```

**Bean creation order:**

1. Spring sees `@Bean resolver()` → creates `InMemoryResolver`
2. Spring sees `@Bean executionEngine(Resolver resolver)` → 
   - Looks for bean of type `Resolver` (finds one created above)
   - Passes it to method
   - Creates `ExecutionEngine`

**Important:** Spring creates each bean **once** (singleton by default). If `Resolver` is needed somewhere, Spring will pass the same instance.

---

### 3. Service (ExecutionService)

```java
@Service
public class ExecutionService {
    private final ExecutionEngine executionEngine;

    public ExecutionService(ExecutionEngine executionEngine) {
        this.executionEngine = executionEngine;
    }
}
```

**What is `@Service`?**
- It's just `@Component` with a more understandable name
- Tells Spring: "This class is a service, create it as a bean"
- Used for business logic

**How does Dependency Injection work?**

1. Spring sees `@Service` on class
2. Scans constructor: `ExecutionService(ExecutionEngine executionEngine)`
3. Looks for bean of type `ExecutionEngine` (finds one created in `PlatformConfiguration`)
4. Creates `ExecutionService`, passing `ExecutionEngine` to constructor

**Why through constructor?**
- This is called **Constructor Injection** (recommended way)
- `final` fields — object cannot be changed after creation
- Easy to test (can pass mock in tests)

---

### 4. Controller (ExecutionController)

```java
@RestController
@RequestMapping("/api/execution")
public class ExecutionController {
    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/plan")
    public ResponseEntity<PlanDTO> createPlan(@Valid @RequestBody ExecutionRequestDTO requestDTO) {
        PlanDTO plan = executionService.createPlan(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }
}
```

**What is `@RestController`?**
- `@Controller` + `@ResponseBody`
- Tells Spring: "This class handles HTTP requests"
- Methods automatically serialize result to JSON

**What is `@RequestMapping("/api/execution")`?**
- Base path for all controller methods
- `@PostMapping("/plan")` is added to base path
- Final path: `POST /api/execution/plan`

**How does Dependency Injection work?**
- Exactly the same as in `ExecutionService`
- Spring passes `ExecutionService` to constructor

**What happens on HTTP request?**

```
1. Client sends: POST /api/execution/plan
   {
     "entity": "Building",
     "entityId": "93939",
     "action": "order_egrn_extract"
   }

2. Spring receives request and looks for controller with method @PostMapping("/plan")

3. Finds ExecutionController.createPlan()

4. Spring:
   - Deserializes JSON to ExecutionRequestDTO
   - Validates (@Valid checks @NotBlank, etc.)
   - Calls method createPlan(requestDTO)

5. Method calls executionService.createPlan(requestDTO)

6. ExecutionService:
   - Converts DTO to domain object ExecutionRequest
   - Calls executionEngine.createPlan(request)
   - Converts result Plan to PlanDTO

7. Spring serializes PlanDTO to JSON and sends to client
```

---

## 🔄 Full Request Lifecycle

```
┌─────────┐
│ Client  │
└────┬────┘
     │ POST /api/execution/plan
     │ { "entity": "Building", ... }
     ▼
┌─────────────────────────────────────┐
│     Spring DispatcherServlet       │
│  (HTTP request router)              │
└────┬────────────────────────────────┘
     │ Finds ExecutionController.createPlan()
     ▼
┌─────────────────────────────────────┐
│   ExecutionController               │
│   (already created by Spring as bean)│
│                                     │
│   executionService.createPlan()     │
└────┬────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────┐
│   ExecutionService                  │
│   (already created by Spring as bean)│
│                                     │
│   executionEngine.createPlan()     │
└────┬────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────┐
│   ExecutionEngine                   │
│   (created in PlatformConfiguration)│
│                                     │
│   planner.createPlan()              │
└────┬────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────┐
│   Planner                           │
│   (created inside ExecutionEngine)  │
│                                     │
│   resolver.findEntityType()         │
│   resolver.findAction()             │
│   resolver.findUIBinding()         │
└────┬────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────┐
│   Resolver                          │
│   (created in PlatformConfiguration)│
│                                     │
│   Returns data from memory          │
└────┬────────────────────────────────┘
     │
     │ Plan Result
     │
     ▼
┌─────────────────────────────────────┐
│   ExecutionService                  │
│   Converts Plan → PlanDTO           │
└────┬────────────────────────────────┘
     │
     │ PlanDTO
     │
     ▼
┌─────────────────────────────────────┐
│   ExecutionController               │
│   Returns ResponseEntity            │
└────┬────────────────────────────────┘
     │
     │ JSON Response
     │
     ▼
┌─────────┐
│ Client  │
└─────────┘
```

---

## 🎓 Key Points

### 1. All Beans Created Once (Singleton)

```java
// Somewhere in code
ExecutionService service1 = ...; // Spring passes
ExecutionService service2 = ...; // the same object

// service1 == service2 (true)
```

### 2. Spring Manages Lifecycle

- Spring creates objects
- Spring stores them in container
- Spring passes dependencies
- Spring destroys on application shutdown

### 3. Annotations are Instructions for Spring

| Annotation | What it tells Spring |
|-----------|-------------------|
| `@Component` | "Create this class as a bean" |
| `@Service` | "This is a service, create as bean" |
| `@Controller` / `@RestController` | "This is a controller, handle HTTP" |
| `@Configuration` | "Here are instructions for creating beans" |
| `@Bean` | "This method creates a bean" |
| `@Autowired` | "Inject dependency" (not needed if constructor exists) |

### 4. Dependency Injection through Constructor (recommended)

```java
// ✅ Good
public ExecutionService(ExecutionEngine executionEngine) {
    this.executionEngine = executionEngine;
}

// ❌ Bad (field injection)
@Autowired
private ExecutionEngine executionEngine;
```

**Why constructor is better?**
- Required dependencies are visible immediately
- Can make fields `final`
- Easier to test
- No need for `@Autowired` annotation

---

## 🧪 Example: What Happens on Startup

```java
// 1. main() starts
SpringApplication.run(PlatformApiApplication.class, args);

// 2. Spring scans packages
// Finds: @Configuration, @Service, @RestController

// 3. Creates beans in correct order:

// 3.1. PlatformConfiguration.resolver()
Resolver resolver = new InMemoryResolver();
// ... fill with data
// Saves to container as bean "resolver"

// 3.2. PlatformConfiguration.executionEngine(resolver)
// Spring finds bean "resolver" and passes it
ExecutionEngine engine = new ExecutionEngine(resolver);
// Saves to container as bean "executionEngine"

// 3.3. ExecutionService(executionEngine)
// Spring finds bean "executionEngine" and passes it
ExecutionService service = new ExecutionService(engine);
// Saves to container as bean "executionService"

// 3.4. ExecutionController(executionService)
// Spring finds bean "executionService" and passes it
ExecutionController controller = new ExecutionController(service);
// Saves to container as bean "executionController"

// 4. Spring registers controllers in DispatcherServlet
// Now Spring knows which method to call for each URL

// 5. Embedded Tomcat starts
// Application is ready to accept HTTP requests!
```

---

## 💡 Practical Example

When request comes:

```bash
curl -X POST http://localhost:8080/api/execution/plan \
  -H "Content-Type: application/json" \
  -d '{"entity": "Building", "entityId": "93939", "action": "order_egrn_extract"}'
```

**What happens inside:**

1. **Tomcat** receives HTTP request
2. **DispatcherServlet** (Spring) determines this is `POST /api/execution/plan`
3. Finds `ExecutionController.createPlan()`
4. Spring:
   - Parses JSON → `ExecutionRequestDTO`
   - Validates (checks `@NotBlank`)
   - Calls `createPlan(requestDTO)`
5. `ExecutionController` calls `executionService.createPlan(requestDTO)`
6. `ExecutionService`:
   - Converts DTO → `ExecutionRequest`
   - Calls `executionEngine.createPlan(request)`
7. `ExecutionEngine` uses `planner` and `resolver` to create plan
8. Result returns back through chain
9. Spring serializes `PlanDTO` → JSON
10. Sends response to client

**All this happens automatically!** You don't create objects manually, Spring does it for you.

---

## 🎯 Summary

**Spring does:**
- ✅ Creates objects (beans)
- ✅ Manages their lifecycle
- ✅ Automatically passes dependencies (DI)
- ✅ Handles HTTP requests
- ✅ Serializes/deserializes JSON
- ✅ Validates data

**You write:**
- ✅ Business logic
- ✅ Annotations (`@Service`, `@RestController`, `@Bean`)
- ✅ Constructors for DI

**Result:**
- Less code
- Less coupling between components
- Easier to test
- Easier to maintain

