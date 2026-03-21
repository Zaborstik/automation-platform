package com.zaborstik.platform.agent.service;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.client.AgentException;
import com.zaborstik.platform.agent.dto.AgentCommand;
import com.zaborstik.platform.agent.dto.AgentResponse;
import com.zaborstik.platform.agent.dto.RetryPolicy;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
            if (!initResponse.success()) {
                log.error("Failed to initialize agent: {}", initResponse.error());
                results.add(StepExecutionResult.failure(
                    "initialize",
                    "browser",
                    initResponse.error(),
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

                if (!result.success()) {
                    success = false;
                    log.error("Step execution failed: {}", result.error());
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
            boolean finalSuccess = success && results.stream().allMatch(StepExecutionResult::success);
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
            if (result.success()) {
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

    private boolean isCoordinateStep(String operationInternalName) {
        return "click".equals(operationInternalName)
            || "hover".equals(operationInternalName)
            || "type".equals(operationInternalName);
    }

    /**
     * Тип UI-операции для исполнителя: {@code system.action.internalname} первого шага с валидным {@code actionId}.
     * Если действий нет — устаревший fallback: {@link PlanStep#workflowStepInternalName()}, если это не состояние ЖЦ шага.
     */
    private String resolveExecutorInternalName(PlanStep step) {
        for (PlanStepAction a : step.actions()) {
            if (a.actionId() == null || a.actionId().isBlank()) {
                continue;
            }
            Optional<Action> found = resolver.findAction(a.actionId());
            if (found.isPresent()) {
                return found.get().internalName();
            }
        }
        String w = step.workflowStepInternalName();
        if (w != null && !resolver.isWorkflowStepInternalName(w)) {
            return w;
        }
        return null;
    }

    private StepExecutionResult executeStepOnce(PlanStep step, int stepIndex, int retryCount) {
        long startTime = System.currentTimeMillis();
        log.debug("Executing step: {}", step);

        try {
            String operation = resolveExecutorInternalName(step);
            if (operation == null) {
                return StepExecutionResult.failure(
                    step.id(),
                    step.displayName(),
                    "Cannot resolve executor operation: add plan_step_action with a valid action id "
                        + "(system.action.internalname defines the UI operation).",
                    System.currentTimeMillis() - startTime,
                    Map.of(),
                    retryCount,
                    stepIndex,
                    null
                );
            }

            if (isCoordinateStep(operation)) {
                return executeCoordinateStep(step, operation, startTime, stepIndex, retryCount);
            }

            AgentCommand command = convertToCommand(step, operation);
            if (command == null) {
                String error = "Unknown executor operation: " + operation;
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

            if (response.success()) {
                String screenshotPath = extractScreenshotPath(response.data());
                return StepExecutionResult.success(
                    step.id(),
                    step.displayName(),
                    response.message(),
                    executionTime,
                    screenshotPath,
                    response.data(),
                    retryCount,
                    stepIndex,
                    command.type().name()
                );
            }
            return StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                response.error(),
                executionTime,
                response.data(),
                retryCount,
                stepIndex,
                command.type().name()
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

    private StepExecutionResult executeCoordinateStep(PlanStep step, String operation,
                                                      long startTime,
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
        if (!coordsResponse.success()) {
            return StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                coordsResponse.error(),
                executionTime,
                mergeMetadata(selector, coordsResponse.data(), null),
                retryCount,
                stepIndex,
                AgentCommand.CommandType.RESOLVE_COORDS.name()
            );
        }

        AgentCommand command;
        switch (operation) {
            case "click":
                double x = extractRequiredNumber(coordsResponse.data(), "x");
                double y = extractRequiredNumber(coordsResponse.data(), "y");
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
                    "Unknown coordinate step type: " + operation,
                    System.currentTimeMillis() - startTime,
                    Map.of(),
                    retryCount,
                    stepIndex,
                    null
                );
        }

        AgentResponse executeResponse = agentClient.execute(command);
        executionTime = System.currentTimeMillis() - startTime;
        Map<String, Object> mergedMetadata = mergeMetadata(selector, coordsResponse.data(), executeResponse.data());
        String screenshotPath = extractScreenshotPath(executeResponse.data());
        if (executeResponse.success()) {
            return StepExecutionResult.success(
                step.id(),
                step.displayName(),
                executeResponse.message(),
                executionTime,
                screenshotPath,
                mergedMetadata,
                retryCount,
                stepIndex,
                command.type().name()
            );
        }
        return StepExecutionResult.failure(
            step.id(),
            step.displayName(),
            executeResponse.error(),
            executionTime,
            mergedMetadata,
            retryCount,
            stepIndex,
            command.type().name()
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
     * Тип операции — {@code system.action.internalname} (см. {@link #resolveExecutorInternalName(PlanStep)}).
     * entityId — target, displayName — explanation; plan_step_action задаёт actionId и metaValue.
     */
    private AgentCommand convertToCommand(PlanStep step, String operationInternalName) {
        String type = operationInternalName;
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
        String error = result.error();
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

