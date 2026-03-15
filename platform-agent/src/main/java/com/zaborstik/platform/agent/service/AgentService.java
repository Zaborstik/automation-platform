package com.zaborstik.platform.agent.service;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.client.AgentException;
import com.zaborstik.platform.agent.dto.AgentCommand;
import com.zaborstik.platform.agent.dto.AgentResponse;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для выполнения планов через UI-агента.
 * Преобразует PlanStep в AgentCommand и выполняет их через AgentClient.
 * 
 * Service for executing plans through UI agent.
 * Converts PlanStep to AgentCommand and executes them through AgentClient.
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
     * 
     * Executes plan through UI agent.
     * 
     * @param plan plan to execute
     * @return list of step execution results
     */
    public List<StepExecutionResult> executePlan(Plan plan) {
        log.info("Starting plan execution: {}", plan.id());
        List<StepExecutionResult> results = new ArrayList<>();

        try {
            // Инициализируем браузер
            // Initialize browser
            AgentResponse initResponse = agentClient.initialize(baseUrl, headless);
            if (!initResponse.isSuccess()) {
                log.error("Failed to initialize agent: {}", initResponse.getError());
                results.add(StepExecutionResult.failure("initialize", "browser", 
                    initResponse.getError(), 0));
                return results;
            }

            // Выполняем каждый шаг плана
            // Execute each plan step
            for (PlanStep step : plan.steps()) {
                StepExecutionResult result = executeStep(step);
                results.add(result);

                if (!result.isSuccess()) {
                    log.error("Step execution failed: {}", result.getError());
                    // Можно добавить логику для обработки ошибок (retry, fallback и т.д.)
                    // Can add error handling logic (retry, fallback, etc.)
                }
            }

            log.info("Plan execution completed: {} steps executed", results.size());
            return results;

        } catch (Exception e) {
            log.error("Plan execution failed", e);
            results.add(StepExecutionResult.failure("plan", plan.id(),
                "Plan execution failed: " + e.getMessage(), 0));
            return results;
        }
    }

    /**
     * Выполняет один шаг плана.
     * 
     * Executes one plan step.
     */
    private StepExecutionResult executeStep(PlanStep step) {
        long startTime = System.currentTimeMillis();
        log.debug("Executing step: {}", step);

        try {
            if (isCoordinateStep(step.workflowStepInternalName())) {
                return executeCoordinateStep(step, startTime);
            }

            AgentCommand command = convertToCommand(step);
            if (command == null) {
                String error = "Unknown step type: " + step.workflowStepInternalName();
                return StepExecutionResult.failure(step.id(), step.displayName(),
                    error, System.currentTimeMillis() - startTime);
            }

            AgentResponse response = agentClient.execute(command);
            long executionTime = System.currentTimeMillis() - startTime;

            if (response.isSuccess()) {
                String screenshotPath = (String) response.getData().get("screenshot");
                return StepExecutionResult.success(step.id(), step.displayName(),
                    response.getMessage(), executionTime, screenshotPath, response.getData());
            } else {
                return StepExecutionResult.failure(step.id(), step.displayName(),
                    response.getError(), executionTime, response.getData());
            }

        } catch (AgentException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return StepExecutionResult.failure(step.id(), step.displayName(),
                e.getMessage(), executionTime);
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return StepExecutionResult.failure(step.id(), step.displayName(),
                "Step execution failed: " + e.getMessage(), executionTime);
        }
    }

    private boolean isCoordinateStep(String stepType) {
        return "click".equals(stepType) || "hover".equals(stepType) || "type".equals(stepType);
    }

    private StepExecutionResult executeCoordinateStep(PlanStep step, long startTime) throws AgentException {
        String selector = resolveSelector(step.entityId());
        if (selector == null || selector.isBlank()) {
            return StepExecutionResult.failure(step.id(), step.displayName(),
                "Target selector is empty for coordinate step", System.currentTimeMillis() - startTime);
        }

        String explanation = step.displayName();
        AgentResponse coordsResponse = agentClient.execute(AgentCommand.resolveCoords(selector, explanation));
        long executionTime = System.currentTimeMillis() - startTime;
        if (!coordsResponse.isSuccess()) {
            return StepExecutionResult.failure(step.id(), step.displayName(),
                coordsResponse.getError(), executionTime, mergeMetadata(selector, coordsResponse.getData(), null));
        }

        AgentCommand command;
        switch (step.workflowStepInternalName()) {
            case "click":
                double x = extractRequiredNumber(coordsResponse.getData(), "x");
                double y = extractRequiredNumber(coordsResponse.getData(), "y");
                command = AgentCommand.clickAt(x, y, explanation, selector);
                break;
            case "hover":
                command = AgentCommand.hover(selector, explanation);
                break;
            case "type":
                String rawMeta = step.actions().isEmpty() ? ""
                    : step.actions().get(0).metaValue() != null ? step.actions().get(0).metaValue() : "";
                boolean pressEnter = rawMeta.endsWith("\\n");
                String typeText = pressEnter ? rawMeta.substring(0, rawMeta.length() - 2) : rawMeta;
                command = pressEnter
                    ? AgentCommand.typeAndSubmit(selector, typeText, explanation)
                    : AgentCommand.type(selector, typeText, explanation);
                break;
            default:
                return StepExecutionResult.failure(step.id(), step.displayName(),
                    "Unknown coordinate step type: " + step.workflowStepInternalName(),
                    System.currentTimeMillis() - startTime);
        }

        AgentResponse executeResponse = agentClient.execute(command);
        executionTime = System.currentTimeMillis() - startTime;
        Map<String, Object> mergedMetadata = mergeMetadata(selector, coordsResponse.getData(), executeResponse.getData());
        String screenshotPath = extractScreenshotPath(executeResponse.getData());
        if (executeResponse.isSuccess()) {
            return StepExecutionResult.success(step.id(), step.displayName(),
                executeResponse.getMessage(), executionTime, screenshotPath, mergedMetadata);
        }
        return StepExecutionResult.failure(step.id(), step.displayName(),
            executeResponse.getError(), executionTime, mergedMetadata);
    }

    private String extractScreenshotPath(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object screenshot = data.get("screenshot");
        return screenshot instanceof String screenshotPath ? screenshotPath : null;
    }

    private double extractRequiredNumber(Map<String, Object> data, String key) {
        if (data == null || !data.containsKey(key) || !(data.get(key) instanceof Number number)) {
            throw new IllegalStateException("Missing numeric field '" + key + "' in coordinates response");
        }
        return number.doubleValue();
    }

    private Map<String, Object> mergeMetadata(String selector, Map<String, Object> coordsData, Map<String, Object> commandData) {
        Map<String, Object> merged = new HashMap<>();
        merged.put("selectorUsed", selector);
        if (coordsData != null) {
            merged.putAll(coordsData);
        }
        if (commandData != null) {
            merged.putAll(commandData);
        }
        return merged;
    }

    private String resolveSelector(String target) {
        if (target != null && target.startsWith("action(") && target.endsWith(")")) {
            String actionId = target.substring(7, target.length() - 1);
            Optional<UIBinding> binding = resolver.findUIBinding(actionId);
            if (binding.isPresent()) {
                return binding.get().selector();
            }
            log.warn("UIBinding not found for action: {}, using target as selector", actionId);
        }
        return target != null ? target : "";
    }

    /**
     * Преобразует PlanStep в AgentCommand.
     * Использует workflowStepInternalName как тип шага, entityId как target, displayName как explanation.
     * Действия шага (plan_step_action) задают actionId и metaValue.
     */
    private AgentCommand convertToCommand(PlanStep step) {
        String type = step.workflowStepInternalName();
        String target = step.entityId();
        String explanation = step.displayName();

        switch (type) {
            case "open_page":
                String url = target;
                if (!step.actions().isEmpty() && step.actions().get(0).metaValue() != null
                        && !step.actions().get(0).metaValue().isBlank()) {
                    url = step.actions().get(0).metaValue();
                }
                return AgentCommand.openPage(url != null ? url : "", explanation);

            case "click":
                return AgentCommand.click(resolveSelector(target), explanation);

            case "hover":
                return AgentCommand.hover(resolveSelector(target), explanation);

            case "type":
                String rawText = step.actions().isEmpty() ? ""
                    : step.actions().get(0).metaValue() != null ? step.actions().get(0).metaValue() : "";
                boolean submitAfterType = rawText.endsWith("\\n");
                String cleanText = submitAfterType ? rawText.substring(0, rawText.length() - 2) : rawText;
                return submitAfterType
                    ? AgentCommand.typeAndSubmit(resolveSelector(target), cleanText, explanation)
                    : AgentCommand.type(resolveSelector(target), cleanText, explanation);

            case "wait":
                long timeout = 5000L;
                if (!step.actions().isEmpty() && step.actions().get(0).metaValue() != null) {
                    try {
                        timeout = Long.parseLong(step.actions().get(0).metaValue());
                    } catch (NumberFormatException ignored) { }
                }
                return AgentCommand.wait(target != null ? target : "result", explanation, timeout);

            case "explain":
                return AgentCommand.explain(explanation);

            default:
                log.warn("Unknown step type: {}", type);
                return null;
        }
    }

    /**
     * Закрывает браузер и освобождает ресурсы.
     * 
     * Closes browser and releases resources.
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

