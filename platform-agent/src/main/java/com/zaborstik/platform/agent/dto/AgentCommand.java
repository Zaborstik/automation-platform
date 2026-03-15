package com.zaborstik.platform.agent.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Команда для выполнения агентом.
 * Содержит тип команды, целевой элемент и параметры.
 */
public class AgentCommand {
    private final CommandType type;
    private final String target;
    private final String explanation;
    private final Map<String, Object> parameters;

    public enum CommandType {
        OPEN_PAGE,
        CLICK,
        CLICK_AT,
        TYPE,
        HOVER,
        RESOLVE_COORDS,
        WAIT,
        EXPLAIN,
        HIGHLIGHT,
        SCREENSHOT
    }

    public AgentCommand(CommandType type, String target, String explanation, Map<String, Object> parameters) {
        this.type = Objects.requireNonNull(type, "Command type cannot be null");
        this.target = target;
        this.explanation = explanation;
        this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
    }

    public CommandType getType() {
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

    public static AgentCommand openPage(String url, String explanation) {
        return new AgentCommand(CommandType.OPEN_PAGE, url, explanation, Map.of());
    }

    public static AgentCommand click(String selector, String explanation) {
        return new AgentCommand(CommandType.CLICK, selector, explanation, Map.of());
    }

    public static AgentCommand clickAt(double x, double y, String explanation, String selectorUsed) {
        String normalizedSelector = selectorUsed != null ? selectorUsed : "";
        return new AgentCommand(
            CommandType.CLICK_AT,
            normalizedSelector,
            explanation,
            Map.of("x", x, "y", y, "selectorUsed", normalizedSelector)
        );
    }

    public static AgentCommand type(String selector, String text, String explanation) {
        return new AgentCommand(CommandType.TYPE, selector, explanation, Map.of("text", text));
    }

    public static AgentCommand typeAndSubmit(String selector, String text, String explanation) {
        return new AgentCommand(CommandType.TYPE, selector, explanation, Map.of("text", text, "pressEnter", true));
    }

    public static AgentCommand hover(String selector, String explanation) {
        return new AgentCommand(CommandType.HOVER, selector, explanation, Map.of());
    }

    public static AgentCommand resolveCoords(String selector, String explanation) {
        return new AgentCommand(CommandType.RESOLVE_COORDS, selector, explanation, Map.of());
    }

    public static AgentCommand wait(String condition, String explanation, long timeoutMs) {
        return new AgentCommand(CommandType.WAIT, condition, explanation, Map.of("timeout", timeoutMs));
    }

    public static AgentCommand explain(String message) {
        return new AgentCommand(CommandType.EXPLAIN, null, message, Map.of());
    }

    public static AgentCommand highlight(String selector, String explanation) {
        return new AgentCommand(CommandType.HIGHLIGHT, selector, explanation, Map.of());
    }

    public static AgentCommand screenshot(String explanation) {
        return new AgentCommand(CommandType.SCREENSHOT, null, explanation, Map.of());
    }

    @Override
    public String toString() {
        return "AgentCommand{type=" + type + ", target='" + target + 
               "', explanation='" + explanation + "'}";
    }
}

