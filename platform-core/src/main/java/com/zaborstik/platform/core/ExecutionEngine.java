package com.zaborstik.platform.core;

import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.planner.Planner;
import com.zaborstik.platform.core.resolver.Resolver;

/**
 * Execution Engine - главный компонент ядра.
 * Координирует работу Resolver и Planner для создания планов выполнения.
 * 
 * Использование:
 * 1. Настроить Resolver (зарегистрировать EntityType, Action, UIBinding)
 * 2. Создать ExecutionEngine с Planner
 * 3. Вызвать createPlan() с ExecutionRequest
 */
public class ExecutionEngine {
    private final Planner planner;

    public ExecutionEngine(Resolver resolver) {
        this.planner = new Planner(resolver);
    }

    public ExecutionEngine(Planner planner) {
        this.planner = planner;
    }

    /**
     * Создает план выполнения для запроса.
     * 
     * @param request запрос на выполнение действия
     * @return план выполнения
     * @throws IllegalArgumentException если не найдены необходимые компоненты
     */
    public Plan createPlan(ExecutionRequest request) {
        return planner.createPlan(request);
    }
}

