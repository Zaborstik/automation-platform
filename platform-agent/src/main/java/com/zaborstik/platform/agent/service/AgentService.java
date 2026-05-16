package com.zaborstik.platform.agent.service;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.client.AgentException;
import com.zaborstik.platform.agent.dto.AgentCommand;
import com.zaborstik.platform.agent.dto.AgentResponse;
import com.zaborstik.platform.agent.dto.RetryPolicy;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Service that turns a single {@link PlanStep} into one or more Playwright
 * commands and dispatches them via {@link AgentClient}.
 *
 * <p>Compared to the previous monolithic version, the service no longer needs
 * a {@code Resolver}. All upstream resolution (action id -> internal name,
 * UI bindings) is performed by {@code platform-executor} and passed in as
 * arguments, so this class can run on its own as a Spring Boot microservice.
 */
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AgentClient agentClient;
    private final RetryPolicy retryPolicy;

    public AgentService(AgentClient agentClient) {
        this(agentClient, RetryPolicy.defaultPolicy());
    }

    public AgentService(AgentClient agentClient, RetryPolicy retryPolicy) {
        this.agentClient = Objects.requireNonNull(agentClient, "agentClient cannot be null");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy cannot be null");
    }

    /**
     * Initialises an underlying browser session via Playwright.
     */
    public AgentResponse initializeSession(String baseUrl, boolean headless) throws AgentException {
        return agentClient.initialize(baseUrl != null ? baseUrl : "", headless);
    }

    /**
     * Closes the underlying browser session if any.
     */
    public AgentResponse closeSession() throws AgentException {
        return agentClient.close();
    }

    /**
     * Executes a single plan step. Retry policy is applied on retryable errors.
     *
     * @param step              plan step to execute
     * @param operation         the {@code system.action.internalname} value
     *                          ({@code click}, {@code type}, {@code open_page}, ...)
     * @param resolvedSelectors map of {@code actionId -> CSS/XPath selector} so that
     *                          steps targeting {@code "action(<id>)"} can be re-pointed
     *                          to the actual DOM element
     * @param stepIndex         zero-based step index within the plan
     */
    public StepExecutionResult executeStep(PlanStep step,
                                           String operation,
                                           Map<String, String> resolvedSelectors,
                                           int stepIndex) {
        Objects.requireNonNull(step, "step cannot be null");
        if (operation == null || operation.isBlank()) {
            return StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                "Operation (action.internalname) is required",
                0L,
                Map.of(),
                0,
                stepIndex,
                null
            );
        }
        Map<String, String> selectors = resolvedSelectors != null ? resolvedSelectors : Map.of();

        int maxAttempts = retryPolicy.maxRetries() + 1;
        StepExecutionResult lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            int retryCount = attempt - 1;
            log.info("Executing step {} attempt {}/{}", step.id(), attempt, maxAttempts);

            StepExecutionResult result = executeStepOnce(step, operation, selectors, stepIndex, retryCount);
            if (result.success()) {
                return result;
            }
            lastFailure = result;
            if (!shouldRetry(result, attempt, maxAttempts)) {
                return result;
            }
            sleepBeforeRetry();
        }
        return lastFailure;
    }

    private StepExecutionResult executeStepOnce(PlanStep step,
                                                String operation,
                                                Map<String, String> selectors,
                                                int stepIndex,
                                                int retryCount) {
        long startTime = System.currentTimeMillis();
        try {
            if (isCoordinateStep(operation)) {
                return executeCoordinateStep(step, operation, selectors, startTime, stepIndex, retryCount);
            }
            AgentCommand command = convertToCommand(step, operation, selectors);
            if (command == null) {
                return StepExecutionResult.failure(
                    step.id(),
                    step.displayName(),
                    "Unknown executor operation: " + operation,
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
        } catch (AgentException ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            return StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                ex.getMessage(),
                executionTime,
                Map.of(),
                retryCount,
                stepIndex,
                null
            );
        } catch (Exception ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            return StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                "Step execution failed: " + ex.getMessage(),
                executionTime,
                Map.of(),
                retryCount,
                stepIndex,
                null
            );
        }
    }

    private StepExecutionResult executeCoordinateStep(PlanStep step,
                                                      String operation,
                                                      Map<String, String> selectors,
                                                      long startTime,
                                                      int stepIndex,
                                                      int retryCount) throws AgentException {
        String selector = resolveSelector(step.entityId(), selectors);
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
            case "click": {
                double x = extractRequiredNumber(coordsResponse.data(), "x");
                double y = extractRequiredNumber(coordsResponse.data(), "y");
                command = AgentCommand.clickAt(x, y, explanation, selector);
                break;
            }
            case "hover":
                command = AgentCommand.hover(selector, explanation);
                break;
            case "type": {
                String rawMeta = firstActionMeta(step);
                boolean pressEnter = rawMeta.endsWith("\\n");
                String typeText = pressEnter ? rawMeta.substring(0, rawMeta.length() - 2) : rawMeta;
                command = pressEnter
                    ? AgentCommand.typeAndSubmit(selector, typeText, explanation)
                    : AgentCommand.type(selector, typeText, explanation);
                break;
            }
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

    private AgentCommand convertToCommand(PlanStep step,
                                          String operation,
                                          Map<String, String> selectors) {
        String target = step.entityId();
        String explanation = step.displayName();

        switch (operation) {
            case "open_page": {
                String url = target;
                String firstMeta = firstActionMeta(step);
                if (!firstMeta.isBlank()) {
                    url = firstMeta;
                }
                return AgentCommand.openPage(url != null ? url : "", explanation);
            }
            case "click":
                return AgentCommand.click(resolveSelector(target, selectors), explanation);
            case "hover":
                return AgentCommand.hover(resolveSelector(target, selectors), explanation);
            case "type": {
                String rawText = firstActionMeta(step);
                boolean submitAfterType = rawText.endsWith("\\n");
                String cleanText = submitAfterType ? rawText.substring(0, rawText.length() - 2) : rawText;
                return submitAfterType
                    ? AgentCommand.typeAndSubmit(resolveSelector(target, selectors), cleanText, explanation)
                    : AgentCommand.type(resolveSelector(target, selectors), cleanText, explanation);
            }
            case "wait": {
                long timeout = 5000L;
                String firstMeta = firstActionMeta(step);
                if (!firstMeta.isBlank()) {
                    try {
                        timeout = Long.parseLong(firstMeta);
                    } catch (NumberFormatException ignored) {
                        // keep default
                    }
                }
                return AgentCommand.wait(target != null ? target : "result", explanation, timeout);
            }
            case "explain":
                return AgentCommand.explain(explanation);
            case "select_option": {
                String selectValue = firstActionMeta(step);
                return AgentCommand.selectOption(resolveSelector(target, selectors), selectValue, explanation);
            }
            case "read_text":
                return AgentCommand.readText(resolveSelector(target, selectors), explanation);
            case "take_screenshot":
                return AgentCommand.screenshot(target != null ? target : "fullpage", explanation);
            default:
                log.warn("Unknown step operation: {}", operation);
                return null;
        }
    }

    private static String resolveSelector(String target, Map<String, String> selectors) {
        if (target != null && target.startsWith("action(") && target.endsWith(")")) {
            String actionId = target.substring(7, target.length() - 1);
            String resolved = selectors.get(actionId);
            if (resolved != null && !resolved.isBlank()) {
                return resolved;
            }
            log.warn("No selector provided for action {}, falling back to target", actionId);
        }
        return target != null ? target : "";
    }

    private static boolean isCoordinateStep(String operation) {
        return "click".equals(operation)
            || "hover".equals(operation)
            || "type".equals(operation);
    }

    private static String firstActionMeta(PlanStep step) {
        for (PlanStepAction action : step.actions()) {
            if (action.metaValue() != null && !action.metaValue().isBlank()) {
                return action.metaValue();
            }
        }
        return "";
    }

    private static String extractScreenshotPath(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object screenshot = data.get("screenshot");
        return screenshot instanceof String screenshotPath ? screenshotPath : null;
    }

    private static double extractRequiredNumber(Map<String, Object> data, String key) {
        if (data == null || !data.containsKey(key) || !(data.get(key) instanceof Number number)) {
            throw new IllegalStateException("Missing numeric field '" + key + "' in coordinates response");
        }
        return number.doubleValue();
    }

    private static Map<String, Object> mergeMetadata(String selector,
                                                     Map<String, Object> coordsData,
                                                     Map<String, Object> commandData) {
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

    private boolean shouldRetry(StepExecutionResult result, int attempt, int maxAttempts) {
        if (attempt >= maxAttempts) {
            return false;
        }
        boolean retryable = retryPolicy.isRetryable(result.error());
        if (retryable) {
            log.warn("Retrying step due to retryable error: {}", result.error());
        }
        return retryable;
    }

    private void sleepBeforeRetry() {
        if (retryPolicy.delayMs() <= 0) {
            return;
        }
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(retryPolicy.delayMs()));
    }
}
