package com.zaborstik.platform.knowledge.llm;

public interface LLMClient {

    LLMResponse complete(String systemPrompt, String userMessage);

    LLMResponse complete(String prompt);

    boolean isAvailable();
}
