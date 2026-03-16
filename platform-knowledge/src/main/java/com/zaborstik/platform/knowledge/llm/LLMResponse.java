package com.zaborstik.platform.knowledge.llm;

import java.util.Objects;

public record LLMResponse(String content, boolean success, String error, long latencyMs) {
    public LLMResponse(String content, boolean success, String error, long latencyMs) {
        this.content = Objects.requireNonNullElse(content, "");
        this.success = success;
        this.error = error;
        this.latencyMs = latencyMs;
    }
}
