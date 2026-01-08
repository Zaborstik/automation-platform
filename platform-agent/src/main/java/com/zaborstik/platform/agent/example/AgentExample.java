package com.zaborstik.platform.agent.example;

import com.zaborstik.platform.agent.config.AgentConfiguration;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.agent.service.AgentService;
import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Пример использования Agent для выполнения планов.
 */
public class AgentExample {
    private static final Logger log = LoggerFactory.getLogger(AgentExample.class);

    public static void main(String[] args) {
        // 1. Настраиваем Resolver с тестовыми данными
        InMemoryResolver resolver = new InMemoryResolver();
        
        EntityType building = new EntityType(
            "Building",
            "Здание",
            null
        );
        resolver.registerEntityType(building);

        Action orderExtract = new Action(
            "order_egrn_extract",
            "Заказать выписку ЕГРН",
            "Заказывает выписку из ЕГРН для здания",
            Set.of("Building"),
            null
        );
        resolver.registerAction(orderExtract);

        UIBinding extractBinding = new UIBinding(
            "order_egrn_extract",
            "button[data-action='order_egrn_extract']",
            UIBinding.SelectorType.CSS,
            null
        );
        resolver.registerUIBinding(extractBinding);

        // 2. Создаем ExecutionEngine
        ExecutionEngine engine = new ExecutionEngine(resolver);

        // 3. Создаем запрос на выполнение
        ExecutionRequest request = new ExecutionRequest(
            "Building",
            "93939",
            "order_egrn_extract",
            null
        );

        // 4. Создаем план
        Plan plan = engine.createPlan(request);
        log.info("Created plan: {}", plan);

        // 5. Настраиваем Agent
        AgentConfiguration config = new AgentConfiguration(
            "http://localhost:3000",  // URL Playwright сервера
            "http://localhost:8080",   // Базовый URL приложения
            false                       // Не headless (видимый браузер)
        );

        // Проверяем доступность агента
        if (!config.checkAgentAvailability()) {
            log.error("Agent is not available. Make sure Playwright server is running.");
            log.error("Start it with: node platform-agent/src/main/resources/playwright-server.js");
            return;
        }

        // 6. Создаем AgentService и выполняем план
        AgentService agentService = config.createAgentService(resolver);
        
        try {
            log.info("Executing plan through agent...");
            List<StepExecutionResult> results = agentService.executePlan(plan);

            // 7. Выводим результаты
            log.info("Execution completed. Results:");
            for (StepExecutionResult result : results) {
                if (result.isSuccess()) {
                    log.info("  ✓ {} - {} ({}ms)", 
                        result.getStepType(), 
                        result.getMessage(), 
                        result.getExecutionTimeMs());
                } else {
                    log.error("  ✗ {} - {} ({}ms)", 
                        result.getStepType(), 
                        result.getError(), 
                        result.getExecutionTimeMs());
                }
            }

        } finally {
            // 8. Закрываем браузер
            agentService.close();
        }
    }
}

