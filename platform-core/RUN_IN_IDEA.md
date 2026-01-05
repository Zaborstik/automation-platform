, # Запуск platform-core в IntelliJ IDEA

## Быстрый запуск примера

### Способ 1: Через контекстное меню

1. Откройте файл `ExampleUsage.java`:
   ```
   platform-core/src/main/java/org/example/core/example/ExampleUsage.java
   ```

2. Найдите метод `main` (строка 23)

3. Нажмите на зеленую стрелку слева от `public static void main` или правой кнопкой мыши → **Run 'ExampleUsage.main()'**

4. Результат появится в консоли Run внизу IDE

### Способ 2: Через Run Configuration

1. Откройте `ExampleUsage.java`

2. Меню: **Run** → **Edit Configurations...**

3. Нажмите **+** → **Application**

4. Настройте:
   - **Name:** `ExampleUsage`
   - **Main class:** `com.zaborstik.platform.core.example.ExampleUsage`
   - **Module:** `platform-core`
   - **Working directory:** `$MODULE_DIR$`

5. Нажмите **OK** и запустите через **Run** → **Run 'ExampleUsage'**

### Способ 3: Через Maven

1. Откройте терминал в IntelliJ (Alt+F12 или View → Tool Windows → Terminal)

2. Выполните:
   ```bash
   cd platform-core
   mvn exec:java -Dexec.mainClass="com.zaborstik.platform.core.example.ExampleUsage"
   ```

## Что вы увидите в консоли

```
Создан план выполнения:
Plan{id=..., entityTypeId='Building', entityId='93939', actionId='order_egrn_extract', status=CREATED, steps=[...]}

Шаги плана:
  - PlanStep{type='open_page', target='/buildings/93939', explanation='Открываю карточку Здание #93939', parameters={}}
  - PlanStep{type='explain', target='null', explanation='Заказывает выписку из ЕГРН для здания', parameters={}}
  - PlanStep{type='hover', target='action(order_egrn_extract)', explanation='Навожу курсор на элемент действия 'Заказать выписку из ЕГРН'', parameters={}}
  - PlanStep{type='click', target='action(order_egrn_extract)', explanation='Выполняю действие 'Заказать выписку из ЕГРН'', parameters={}}
  - PlanStep{type='wait', target='result', explanation='Ожидаю завершения действия 'Заказать выписку из ЕГРН'', parameters={}}
```

## Запуск тестов в IntelliJ

### Запуск всех тестов

1. Откройте папку `platform-core/src/test/java`

2. Правой кнопкой на папку `org.example.core` → **Run 'All Tests'**

Или через Maven:
```bash
cd platform-core
mvn test
```

### Запуск конкретного теста

1. Откройте файл теста, например `ExecutionEngineTest.java`

2. Найдите нужный тест-метод (например, `shouldCreatePlanSuccessfully`)

3. Нажмите зеленую стрелку слева от метода или правой кнопкой → **Run 'shouldCreatePlanSuccessfully()'**

## Создание своего примера

Вы можете создать свой класс для экспериментов:

1. Создайте новый класс в `platform-core/src/main/java/org/example/core/example/`:
   ```java
   package org.example.core.example;
   
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
           // Ваш код здесь
       }
   }
   ```

2. Запустите через зеленую стрелку рядом с `main`

## Отладка (Debug)

Для отладки:

1. Установите breakpoint (кликните слева от номера строки - появится красная точка)

2. Запустите через **Debug** (Shift+F9) вместо Run

3. Программа остановится на breakpoint

4. Используйте:
   - **F8** - Step Over (следующая строка)
   - **F7** - Step Into (войти в метод)
   - **F9** - Resume (продолжить выполнение)

## Настройка проекта в IntelliJ

Если проект не распознан как Maven проект:

1. Правой кнопкой на корневой `pom.xml` → **Add as Maven Project**

2. Дождитесь индексации (внизу справа будет прогресс)

3. Если нужно, обновите зависимости: **File** → **Invalidate Caches / Restart**

## Полезные горячие клавиши

- **Ctrl+Shift+F10** - Run текущего класса
- **Shift+F10** - Run последней конфигурации
- **Shift+F9** - Debug текущего класса
- **Alt+Enter** - Quick Fix / Import
- **Ctrl+Space** - Автодополнение

## Решение проблем

### Проблема: "Cannot resolve symbol"

**Решение:**
1. **File** → **Invalidate Caches / Restart**
2. Убедитесь, что проект распознан как Maven: правой кнопкой на `pom.xml` → **Add as Maven Project**
3. **File** → **Project Structure** → **Modules** → проверьте, что `platform-core` добавлен

### Проблема: "Main class not found"

**Решение:**
1. Убедитесь, что класс компилируется: **Build** → **Rebuild Project**
2. Проверьте, что в Run Configuration указан правильный **Main class** и **Module**

### Проблема: "Module not specified"

**Решение:**
В Run Configuration укажите:
- **Module:** `platform-core`
- **Working directory:** `$MODULE_DIR$`

