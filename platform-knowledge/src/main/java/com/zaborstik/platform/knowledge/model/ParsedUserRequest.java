package com.zaborstik.platform.knowledge.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ParsedUserRequest(
    String rawInput,
    String entityTypeId,
    List<String> actionIds,
    Map<String, String> parameters,
    boolean clarificationNeeded,
    String clarificationQuestion
) {
    public ParsedUserRequest(String rawInput, String entityTypeId, List<String> actionIds, Map<String, String> parameters,
                             boolean clarificationNeeded, String clarificationQuestion) {
        this.rawInput = Objects.requireNonNull(rawInput, "rawInput cannot be null");
        this.entityTypeId = entityTypeId;
        this.actionIds = actionIds != null ? List.copyOf(actionIds) : List.of();
        this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
        this.clarificationNeeded = clarificationNeeded;
        this.clarificationQuestion = clarificationQuestion;
    }
}
