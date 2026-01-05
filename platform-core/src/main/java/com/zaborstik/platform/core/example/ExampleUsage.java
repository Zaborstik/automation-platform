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

/**
 * Пример использования Execution Engine.
 * 
 * Демонстрирует:
 * 1. Регистрацию EntityType, Action, UIBinding
 * 2. Создание ExecutionRequest
 * 3. Построение плана выполнения
 */
public class ExampleUsage {
    public static void main(String[] args) {
        // 1. Создаем Resolver и регистрируем метаданные
        InMemoryResolver resolver = new InMemoryResolver();
        
        // Регистрируем EntityType
        EntityType buildingType = new EntityType(
            "Building",
            "Здание",
            Map.of("description", "Тип сущности для работы со зданиями")
        );
        resolver.registerEntityType(buildingType);
        
        // Регистрируем Action
        Action orderEgrnExtractAction = new Action(
            "order_egrn_extract",
            "Заказать выписку из ЕГРН",
            "Заказывает выписку из ЕГРН для здания",
            Set.of("Building"),
            Map.of("category", "egrn")
        );
        resolver.registerAction(orderEgrnExtractAction);
        
        // Регистрируем UIBinding
        UIBinding uiBinding = new UIBinding(
            "order_egrn_extract",
            "[data-action='order_egrn_extract']",
            UIBinding.SelectorType.CSS,
            Map.of("fallback", "vision")
        );
        resolver.registerUIBinding(uiBinding);
        
        // 2. Создаем Execution Engine
        ExecutionEngine engine = new ExecutionEngine(resolver);
        
        // 3. Создаем запрос на выполнение
        ExecutionRequest request = new ExecutionRequest(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );
        
        // 4. Создаем план
        Plan plan = engine.createPlan(request);
        
        // 5. Выводим результат
        System.out.println("Создан план выполнения:");
        System.out.println(plan);
        System.out.println("\nШаги плана:");
        plan.getSteps().forEach(step -> {
            System.out.println("  - " + step);
        });
    }
}

