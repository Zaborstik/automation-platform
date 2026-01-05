package org.example.agent.service;

import org.example.agent.client.AgentClient;
import org.example.agent.client.AgentException;
import org.example.agent.dto.AgentCommand;
import org.example.agent.dto.AgentResponse;
import org.example.agent.dto.StepExecutionResult;
import org.example.core.domain.UIBinding;
import org.example.core.plan.Plan;
import org.example.core.plan.PlanStep;
import org.example.core.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для выполнения планов через UI-агента.
 * Преобразует PlanStep в AgentCommand и выполняет их через AgentClient.
 */
public class AgentService {
    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    
    private final AgentClient agentClient;
    private final Resolver resolver;
    private final String baseUrl;
    private final boolean headless;

    public AgentService(AgentClient agentClient, Resolver resolver, String baseUrl, boolean headless) {
        this.agentClient = agentClient;
        this.resolver = resolver;
        this.baseUrl = baseUrl;
        this.headless = headless;
    }

    /**
     * Выполняет план через UI-агента.
     * 
     * @param plan план для выполнения
     * @return список результатов выполнения шагов
     */
    public List<StepExecutionResult> executePlan(Plan plan) {
        log.info("Starting plan execution: {}", plan.getId());
        List<StepExecutionResult> results = new ArrayList<>();

        try {
            // Инициализируем браузер
            AgentResponse initResponse = agentClient.initialize(baseUrl, headless);
            if (!initResponse.isSuccess()) {
                log.error("Failed to initialize agent: {}", initResponse.getError());
                results.add(StepExecutionResult.failure("initialize", "browser", 
                    initResponse.getError(), 0));
                return results;
            }

            // Выполняем каждый шаг плана
            for (PlanStep step : plan.getSteps()) {
                StepExecutionResult result = executeStep(step);
                results.add(result);

                if (!result.isSuccess()) {
                    log.error("Step execution failed: {}", result.getError());
                    // Можно добавить логику для обработки ошибок (retry, fallback и т.д.)
                }
            }

            log.info("Plan execution completed: {} steps executed", results.size());
            return results;

        } catch (Exception e) {
            log.error("Plan execution failed", e);
            results.add(StepExecutionResult.failure("plan", plan.getId(), 
                "Plan execution failed: " + e.getMessage(), 0));
            return results;
        }
    }

    /**
     * Выполняет один шаг плана.
     */
    private StepExecutionResult executeStep(PlanStep step) {
        long startTime = System.currentTimeMillis();
        log.debug("Executing step: {}", step);

        try {
            AgentCommand command = convertToCommand(step);
            if (command == null) {
                String error = "Unknown step type: " + step.getType();
                return StepExecutionResult.failure(step.getType(), step.getTarget(), 
                    error, System.currentTimeMillis() - startTime);
            }

            AgentResponse response = agentClient.execute(command);
            long executionTime = System.currentTimeMillis() - startTime;

            if (response.isSuccess()) {
                String screenshotPath = (String) response.getData().get("screenshot");
                return StepExecutionResult.success(step.getType(), step.getTarget(), 
                    response.getMessage(), executionTime, screenshotPath);
            } else {
                return StepExecutionResult.failure(step.getType(), step.getTarget(), 
                    response.getError(), executionTime);
            }

        } catch (AgentException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return StepExecutionResult.failure(step.getType(), step.getTarget(), 
                e.getMessage(), executionTime);
        }
    }

    /**
     * Преобразует PlanStep в AgentCommand.
     */
    private AgentCommand convertToCommand(PlanStep step) {
        String type = step.getType();
        String target = step.getTarget();
        String explanation = step.getExplanation();

        switch (type) {
            case "open_page":
                return AgentCommand.openPage(target, explanation);

            case "click":
                // Если target в формате "action(actionId)", находим UIBinding
                if (target != null && target.startsWith("action(") && target.endsWith(")")) {
                    String actionId = target.substring(7, target.length() - 1);
                    Optional<UIBinding> binding = resolver.findUIBinding(actionId);
                    if (binding.isPresent()) {
                        String selector = binding.get().getSelector();
                        return AgentCommand.click(selector, explanation);
                    } else {
                        log.warn("UIBinding not found for action: {}, using target as selector", actionId);
                        return AgentCommand.click(target, explanation);
                    }
                }
                return AgentCommand.click(target, explanation);

            case "hover":
                // Аналогично click
                if (target != null && target.startsWith("action(") && target.endsWith(")")) {
                    String actionId = target.substring(7, target.length() - 1);
                    Optional<UIBinding> binding = resolver.findUIBinding(actionId);
                    if (binding.isPresent()) {
                        String selector = binding.get().getSelector();
                        return AgentCommand.hover(selector, explanation);
                    } else {
                        log.warn("UIBinding not found for action: {}, using target as selector", actionId);
                        return AgentCommand.hover(target, explanation);
                    }
                }
                return AgentCommand.hover(target, explanation);

            case "type":
                String text = (String) step.getParameters().get("text");
                return AgentCommand.type(target, text, explanation);

            case "wait":
                long timeout = step.getParameters().containsKey("timeout") 
                    ? ((Number) step.getParameters().get("timeout")).longValue()
                    : 5000L; // default 5 seconds
                return AgentCommand.wait(target, explanation, timeout);

            case "explain":
                return AgentCommand.explain(explanation);

            default:
                log.warn("Unknown step type: {}", type);
                return null;
        }
    }

    /**
     * Закрывает браузер и освобождает ресурсы.
     */
    public void close() {
        try {
            agentClient.close();
            log.info("Agent closed successfully");
        } catch (AgentException e) {
            log.error("Failed to close agent", e);
        }
    }
}

