package com.zaborstik.platform.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.resolver.Resolver;
import com.zaborstik.platform.knowledge.llm.LLMClient;
import com.zaborstik.platform.knowledge.llm.LLMResponse;
import com.zaborstik.platform.knowledge.llm.PromptTemplate;
import com.zaborstik.platform.knowledge.model.ParsedUserRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UserRequestParser {

    private static final String DEFAULT_CLARIFICATION = "Уточните, что именно нужно сделать.";
    private static final List<String> DEFAULT_ENTITY_TYPE_IDS = List.of(
        "ent-page", "ent-form", "ent-input", "ent-button", "ent-link", "ent-table"
    );
    private static final List<String> DEFAULT_ACTION_IDS = List.of(
        "act-open-page", "act-click", "act-input-text", "act-select-option",
        "act-wait-element", "act-read-text", "act-take-screenshot"
    );

    private final LLMClient llmClient;
    private final Resolver resolver;
    private final ObjectMapper objectMapper;

    public UserRequestParser(LLMClient llmClient, Resolver resolver) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient cannot be null");
        this.resolver = Objects.requireNonNull(resolver, "resolver cannot be null");
        this.objectMapper = new ObjectMapper();
    }

    public ParsedUserRequest parse(String userInput) {
        Objects.requireNonNull(userInput, "userInput cannot be null");

        String prompt = PromptTemplate.loadAndFill("prompts/parse-user-request.txt", Map.of(
            "entity_types", String.join(", ", collectEntityTypes()),
            "actions", String.join(", ", collectActions()),
            "user_input", userInput
        ));

        LLMResponse response = llmClient.complete(prompt);
        if (!response.success()) {
            return fallback(userInput);
        }

        try {
            LlmParsedRequest parsed = objectMapper.readValue(response.content(), LlmParsedRequest.class);
            return new ParsedUserRequest(
                userInput,
                parsed.entityTypeId,
                parsed.actionIds != null ? parsed.actionIds : List.of(),
                parsed.parameters != null ? parsed.parameters : Map.of(),
                parsed.clarificationNeeded,
                parsed.clarificationQuestion
            );
        } catch (Exception e) {
            return fallback(userInput);
        }
    }

    private ParsedUserRequest fallback(String userInput) {
        return new ParsedUserRequest(
            userInput,
            null,
            List.of(),
            Map.of(),
            true,
            DEFAULT_CLARIFICATION
        );
    }

    private List<String> collectEntityTypes() {
        List<String> result = new ArrayList<>();
        for (String id : DEFAULT_ENTITY_TYPE_IDS) {
            resolver.findEntityType(id)
                .map(EntityType::id)
                .ifPresent(result::add);
        }
        return result;
    }

    private List<String> collectActions() {
        List<String> result = new ArrayList<>();
        for (String id : DEFAULT_ACTION_IDS) {
            resolver.findAction(id)
                .map(Action::id)
                .ifPresent(result::add);
        }
        return result;
    }

    private static class LlmParsedRequest {
        public String entityTypeId;
        public List<String> actionIds;
        public Map<String, String> parameters;
        public boolean clarificationNeeded;
        public String clarificationQuestion;
    }
}
