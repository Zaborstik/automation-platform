package com.zaborstik.platform.knowledge.llm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class StubLLMClient implements LLMClient {

    private final LinkedHashMap<String, String> responses = new LinkedHashMap<>();
    private final String defaultResponse;

    public StubLLMClient(Map<String, String> responses, String defaultResponse) {
        if (responses != null) {
            this.responses.putAll(responses);
        }
        this.defaultResponse = Objects.requireNonNullElse(defaultResponse, "{}");
    }

    public StubLLMClient(String defaultResponse) {
        this(Map.of(), defaultResponse);
    }

    public void addResponse(String inputContains, String response) {
        responses.put(Objects.requireNonNull(inputContains, "inputContains cannot be null"),
            Objects.requireNonNullElse(response, ""));
    }

    @Override
    public LLMResponse complete(String systemPrompt, String userMessage) {
        String mergedPrompt = Objects.requireNonNullElse(systemPrompt, "") + "\n" + Objects.requireNonNullElse(userMessage, "");
        return complete(mergedPrompt);
    }

    @Override
    public LLMResponse complete(String prompt) {
        long start = System.currentTimeMillis();
        String safePrompt = Objects.requireNonNullElse(prompt, "");
        for (Map.Entry<String, String> entry : responses.entrySet()) {
            if (safePrompt.contains(entry.getKey())) {
                return new LLMResponse(entry.getValue(), true, null, System.currentTimeMillis() - start);
            }
        }
        return new LLMResponse(defaultResponse, true, null, System.currentTimeMillis() - start);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
