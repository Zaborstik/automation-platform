package com.zaborstik.platform.agent.service;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.client.AgentException;
import com.zaborstik.platform.agent.dto.AgentCommand;
import com.zaborstik.platform.agent.dto.AgentResponse;
import com.zaborstik.platform.agent.dto.RetryPolicy;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

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
    private final RetryPolicy retryPolicy;

    public AgentService(AgentClient agentClient, Resolver resolver, String baseUrl, boolean headless) {
        this(agentClient, resolver, baseUrl, headless, RetryPolicy.defaultPolicy());
    }

    public AgentService(AgentClient agentClient, Resolver resolver, String baseUrl, boolean headless,
                        RetryPolicy retryPolicy) {
        this.agentClient = agentClient;
        this.resolver = resolver;
        this.baseUrl = baseUrl;
        this.headless = headless;
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy cannot be null");
    }

    /**
     * Выполняет план через UI-агента.
     *
     * Executes plan through UI agent.
     *
     * @param plan план для выполнения / plan to execute
     * @return список результатов выполнения шагов / list of step execution results
     */
    public List<StepExecutionResult> executePlan(Plan plan) {
        return executePlan(plan, false, StepExecutionCallback.noOp());
    }

    public List<StepExecutionResult> executePlan(Plan plan, StepExecutionCallback callback) {
        return executePlan(plan, false, callback);
    }

    public List<StepExecutionResult> executePlan(Plan plan, boolean stopOnFailure, StepExecutionCallback callback) {
        Objects.requireNonNull(plan, "plan cannot be null");
        StepExecutionCallback effectiveCallback = callback != null ? callback : StepExecutionCallback.noOp();

        log.info("Starting plan execution: {}", plan.id());
        List<StepExecutionResult> results = new ArrayList<>();
        boolean success = true;

        safeOnPlanStarted(effectiveCallback, plan);
        try {
            AgentResponse initResponse = agentClient.initialize(baseUrl, headless);
            if (!initResponse.isSuccess()) {
                log.error("Failed to initialize agent: {}", initResponse.getError());
                results.add(StepExecutionResult.failure(
                    "initialize",
                    "browser",
                    initResponse.getError(),
                    0,
                    Map.of(),
                    0,
                    -1,
                    null
                ));
                success = false;
                return results;
            }

            List<PlanStep> steps = plan.steps();
            for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
                PlanStep step = steps.get(stepIndex);
                safeOnStepStarted(effectiveCallback, step, stepIndex, steps.size());

                StepExecutionResult result = executeStep(step, stepIndex);
                results.add(result);
                safeOnStepCompleted(effectiveCallback, step, result, stepIndex);

                if (!result.isSuccess()) {
                    success = false;
                    log.error("Step execution failed: {}", result.getError());
                    if (stopOnFailure) {
                        break;
                    }
                }
            }

            log.info("Plan execution completed: {} steps executed", results.size());
            return results;
        } catch (Exception e) {
            log.error("Plan execution failed", e);
            results.add(StepExecutionResult.failure(
                "plan",
                plan.id(),
                "Plan execution failed: " + e.getMessage(),
                0,
                Map.of(),
                0,
                -1,
                null
            ));
            success = false;
            return results;
        } finally {
            boolean finalSuccess = success && results.stream().allMatch(StepExecutionResult::isSuccess);
            safeOnPlanCompleted(effectiveCallback, plan, List.copyOf(results), finalSuccess);
        }
    }

    /**
     * Выполняет один шаг плана.
     * 
     * Executes one plan step.
     */
    private StepExecutionResult executeStep(PlanStep step, int stepIndex) {
        int maxAttempts = retryPolicy.maxRetries() + 1;
        StepExecutionResult lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            int retryCount = attempt - 1;
            log.info("Executing step {} attempt {}/{}", step.id(), attempt, maxAttempts);

            StepExecutionResult result = executeStepOnce(step, stepIndex, retryCount);
            if (result.isSuccess()) {
                return result;
            }

            lastFailure = result;
            if (!shouldRetry(result, attempt, maxAttempts)) {
                return result;
            }

            sleepBeforeRetry();
        }

        return lastFailure != null
            ? lastFailure
            : StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                "Step execution failed without details",
                0,
                Map.of(),
                retryPolicy.maxRetries(),
                stepIndex,
                null
            );
    }

    private boolean isCoordinateStep(String stepType) {
        return "click".equals(stepType) || "hover".equals(stepType) || "type".equals(stepType);
    }

    private StepExecutionResult executeStepOnce(PlanStep step, int stepIndex, int retryCount) {
        long startTime = System.currentTimeMillis();
        log.debug("Executing step: {}", step);

        try {
            if (isCoordinateStep(step.workflowStepInternalName())) {
                return executeCoordinateStep(step, startTime, stepIndex, retryCount);
            }

            AgentCommand command = convertToCommand(step);
            if (command == null) {
                String error = "Unknown step type: " + step.workflowStepInternalName();
                return StepExecutionResult.failure(
                    step.id(),
                    step.displayName(),
                    error,
                    System.currentTimeMillis() - startTime,
                    Map.of(),
                    retryCount,
                    stepIndex,
                    null
                );
            }

            AgentResponse response = agentClient.execute(command);
            long executionTime = System.currentTimeMillis() - startTime;

            if (response.isSuccess()) {
                String screenshotPath = extractScreenshotPath(response.getData());
                return StepExecutionResult.success(
                    step.id(),
                    step.displayName(),
                    response.getMessage(),
                    executionTime,
                    screenshotPath,
                    response.getData(),
                    retryCount,
                    stepIndex,
                    command.getType().name()
                );
            }
            return StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                response.getError(),
                executionTime,
                response.getData(),
                retryCount,
                stepIndex,
                command.getType().name()
            );
        } catch (AgentException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                e.getMessage(),
                executionTime,
                Map.of(),
                retryCount,
                stepIndex,
                null
            );
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                "Step execution failed: " + e.getMessage(),
                executionTime,
                Map.of(),
                retryCount,
                stepIndex,
                null
            );
        }
    }

    private StepExecutionResult executeCoordinateStep(PlanStep step, long startTime,
                                                      int stepIndex, int retryCount) throws AgentException {
        String selector = resolveSelector(step.entityId());
        if (selector == null || selector.isBlank()) {
            return StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                "Target selector is empty for coordinate step",
                System.currentTimeMillis() - startTime,
                Map.of(),
                retryCount,
                stepIndex,
                null
            );
        }

        String explanation = step.displayName();
        AgentResponse coordsResponse = agentClient.execute(AgentCommand.resolveCoords(selector, explanation));
        long executionTime = System.currentTimeMillis() - startTime;
        if (!coordsResponse.isSuccess()) {
            return StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                coordsResponse.getError(),
                executionTime,
                mergeMetadata(selector, coordsResponse.getData(), null),
                retryCount,
                stepIndex,
                AgentCommand.CommandType.RESOLVE_COORDS.name()
            );
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
                return StepExecutionResult.failure(
                    step.id(),
                    step.displayName(),
                    "Unknown coordinate step type: " + step.workflowStepInternalName(),
                    System.currentTimeMillis() - startTime,
                    Map.of(),
                    retryCount,
                    stepIndex,
                    null
                );
        }

        AgentResponse executeResponse = agentClient.execute(command);
        executionTime = System.currentTimeMillis() - startTime;
        Map<String, Object> mergedMetadata = mergeMetadata(selector, coordsResponse.getData(), executeResponse.getData());
        String screenshotPath = extractScreenshotPath(executeResponse.getData());
        if (executeResponse.isSuccess()) {
            return StepExecutionResult.success(
                step.id(),
                step.displayName(),
                executeResponse.getMessage(),
                executionTime,
                screenshotPath,
                mergedMetadata,
                retryCount,
                stepIndex,
                command.getType().name()
            );
        }
        return StepExecutionResult.failure(
            step.id(),
            step.displayName(),
            executeResponse.getError(),
            executionTime,
            mergedMetadata,
            retryCount,
            stepIndex,
            command.getType().name()
        );
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
        return resolver.resolveTargetToSelector(target, baseUrl);
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

            case "select_option":
                String selectValue = step.actions().isEmpty() ? ""
                    : step.actions().get(0).metaValue() != null ? step.actions().get(0).metaValue() : "";
                return AgentCommand.selectOption(resolveSelector(target), selectValue, explanation);

            case "read_text":
                return AgentCommand.readText(resolveSelector(target), explanation);

            case "take_screenshot":
                return AgentCommand.screenshot(target != null ? target : "fullpage", explanation);

            default:
                log.warn("Unknown step type: {}", type);
                return null;
        }
    }

    private boolean shouldRetry(StepExecutionResult result, int attempt, int maxAttempts) {
        if (attempt >= maxAttempts) {
            return false;
        }
        String error = result.getError();
        boolean retryable = retryPolicy.isRetryable(error);
        if (retryable) {
            log.warn("Retrying step due to retryable error: {}", error);
        }
        return retryable;
    }

    private void sleepBeforeRetry() {
        if (retryPolicy.delayMs() <= 0) {
            return;
        }
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(retryPolicy.delayMs()));
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Retry delay interrupted");
        }
    }

    private void safeOnPlanStarted(StepExecutionCallback callback, Plan plan) {
        try {
            callback.onPlanStarted(plan);
        } catch (Exception e) {
            log.warn("StepExecutionCallback.onPlanStarted failed: {}", e.getMessage());
        }
    }

    private void safeOnStepStarted(StepExecutionCallback callback, PlanStep step, int stepIndex, int totalSteps) {
        try {
            callback.onStepStarted(step, stepIndex, totalSteps);
        } catch (Exception e) {
            log.warn("StepExecutionCallback.onStepStarted failed: {}", e.getMessage());
        }
    }

    private void safeOnStepCompleted(StepExecutionCallback callback, PlanStep step,
                                     StepExecutionResult result, int stepIndex) {
        try {
            callback.onStepCompleted(step, result, stepIndex);
        } catch (Exception e) {
            log.warn("StepExecutionCallback.onStepCompleted failed: {}", e.getMessage());
        }
    }

    private void safeOnPlanCompleted(StepExecutionCallback callback, Plan plan,
                                     List<StepExecutionResult> results, boolean success) {
        try {
            callback.onPlanCompleted(plan, results, success);
        } catch (Exception e) {
            log.warn("StepExecutionCallback.onPlanCompleted failed: {}", e.getMessage());
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

