package com.zaborstik.platform.core.plan;

import java.util.Map;
import java.util.Objects;

/**
 * Шаг плана выполнения.
 * Примеры:
 * - open_page: /buildings/{id}
 * - explain: "Открываю карточку здания"
 * - hover: action(order_egrn_extract)
 * - click: action(order_egrn_extract)
 * - wait: result
 * 
 * Plan execution step.
 * Examples:
 * - open_page: /buildings/{id}
 * - explain: "Opening building card"
 * - hover: action(order_egrn_extract)
 * - click: action(order_egrn_extract)
 * - wait: result
 */
public class PlanStep {
    private final String type;
    private final String target;
    private final String explanation;
    private final Map<String, Object> parameters;

    public PlanStep(String type, String target, String explanation, Map<String, Object> parameters) {
        this.type = Objects.requireNonNull(type, "Step type cannot be null");
        this.target = target;
        this.explanation = explanation;
        this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
    }

    public String getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public String getExplanation() {
        return explanation;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public static PlanStep openPage(String url, String explanation) {
        return new PlanStep("open_page", url, explanation, Map.of());
    }

    public static PlanStep explain(String message) {
        return new PlanStep("explain", null, message, Map.of());
    }

    public static PlanStep hover(String actionId, String explanation) {
        return new PlanStep("hover", "action(" + actionId + ")", explanation, Map.of());
    }

    public static PlanStep click(String actionId, String explanation) {
        return new PlanStep("click", "action(" + actionId + ")", explanation, Map.of());
    }

    public static PlanStep wait(String condition, String explanation) {
        return new PlanStep("wait", condition, explanation, Map.of());
    }

    public static PlanStep type(String selector, String text, String explanation) {
        return new PlanStep("type", selector, explanation, Map.of("text", text));
    }

    @Override
    public String toString() {
        return "PlanStep{type='" + type + "', target='" + target + 
               "', explanation='" + explanation + "'}";
    }
}

