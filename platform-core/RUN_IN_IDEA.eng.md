# Running platform-core in IntelliJ IDEA

## Quick Start Example

### Method 1: Through Context Menu

1. Open file `ExampleUsage.java`:
   ```
   platform-core/src/main/java/com/zaborstik/platform/core/example/ExampleUsage.java
   ```

2. Find `main` method (line 23)

3. Click green arrow to the left of `public static void main` or right-click → **Run 'ExampleUsage.main()'**

4. Result will appear in Run console at the bottom of IDE

### Method 2: Through Run Configuration

1. Open `ExampleUsage.java`

2. Menu: **Run** → **Edit Configurations...**

3. Click **+** → **Application**

4. Configure:
   - **Name:** `ExampleUsage`
   - **Main class:** `com.zaborstik.platform.core.example.ExampleUsage`
   - **Module:** `platform-core`
   - **Working directory:** `$MODULE_DIR$`

5. Click **OK** and run via **Run** → **Run 'ExampleUsage'**

### Method 3: Through Maven

1. Open terminal in IntelliJ (Alt+F12 or View → Tool Windows → Terminal)

2. Execute:
   ```bash
   cd platform-core
   mvn exec:java -Dexec.mainClass="com.zaborstik.platform.core.example.ExampleUsage"
   ```

## What You'll See in Console

```
Created execution plan:
Plan{id=..., entityTypeId='Building', entityId='93939', actionId='order_egrn_extract', status=CREATED, steps=[...]}

Plan steps:
  - PlanStep{type='open_page', target='/buildings/93939', explanation='Opening Building card #93939', parameters={}}
  - PlanStep{type='explain', target='null', explanation='Orders EGRN extract for building', parameters={}}
  - PlanStep{type='hover', target='action(order_egrn_extract)', explanation='Hovering over action element 'Order EGRN Extract'', parameters={}}
  - PlanStep{type='click', target='action(order_egrn_extract)', explanation='Executing action 'Order EGRN Extract'', parameters={}}
  - PlanStep{type='wait', target='result', explanation='Waiting for action 'Order EGRN Extract' completion', parameters={}}
```

## Running Tests in IntelliJ

### Run All Tests

1. Open folder `platform-core/src/test/java`

2. Right-click on folder `com.zaborstik.platform.core` → **Run 'All Tests'**

Or via Maven:
```bash
cd platform-core
mvn test
```

### Run Specific Test

1. Open test file, e.g. `ExecutionEngineTest.java`

2. Find needed test method (e.g. `shouldCreatePlanSuccessfully`)

3. Click green arrow to the left of method or right-click → **Run 'shouldCreatePlanSuccessfully()'**

## Creating Your Own Example

You can create your own class for experiments:

1. Create new class in `platform-core/src/main/java/com/zaborstik/platform/core/example/`:
   ```java
   package com.zaborstik.platform.core.example;
   
   import com.zaborstik.platform.core.ExecutionEngine;
   import com.zaborstik.platform.core.domain.Action;
   import com.zaborstik.platform.core.domain.EntityType;
   import com.zaborstik.platform.core.domain.UIBinding;
   import com.zaborstik.platform.core.execution.ExecutionRequest;
   import com.zaborstik.platform.core.plan.Plan;
   import com.zaborstik.platform.core.resolver.InMemoryResolver;
   
   import java.util.Map;
   import java.util.Set;
   
   public class MyExample {
       public static void main(String[] args) {
           // Your code here
       }
   }
   ```

2. Run via green arrow next to `main`

## Debugging

For debugging:

1. Set breakpoint (click to the left of line number - red dot will appear)

2. Run via **Debug** (Shift+F9) instead of Run

3. Program will stop at breakpoint

4. Use:
   - **F8** - Step Over (next line)
   - **F7** - Step Into (enter method)
   - **F9** - Resume (continue execution)

## Project Setup in IntelliJ

If project is not recognized as Maven project:

1. Right-click on root `pom.xml` → **Add as Maven Project**

2. Wait for indexing (progress will be shown at bottom right)

3. If needed, refresh dependencies: **File** → **Invalidate Caches / Restart**

## Useful Shortcuts

- **Ctrl+Shift+F10** - Run current class
- **Shift+F10** - Run last configuration
- **Shift+F9** - Debug current class
- **Alt+Enter** - Quick Fix / Import
- **Ctrl+Space** - Autocomplete

## Troubleshooting

### Problem: "Cannot resolve symbol"

**Solution:**
1. **File** → **Invalidate Caches / Restart**
2. Make sure project is recognized as Maven: right-click on `pom.xml` → **Add as Maven Project**
3. **File** → **Project Structure** → **Modules** → check that `platform-core` is added

### Problem: "Main class not found"

**Solution:**
1. Make sure class compiles: **Build** → **Rebuild Project**
2. Check that Run Configuration has correct **Main class** and **Module**

### Problem: "Module not specified"

**Solution:**
In Run Configuration specify:
- **Module:** `platform-core`
- **Working directory:** `$MODULE_DIR$`

