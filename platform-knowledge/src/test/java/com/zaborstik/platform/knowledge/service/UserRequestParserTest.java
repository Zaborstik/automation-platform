package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import com.zaborstik.platform.knowledge.llm.LLMClient;
import com.zaborstik.platform.knowledge.llm.LLMResponse;
import com.zaborstik.platform.knowledge.model.ParsedUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRequestParserTest {

    private InMemoryResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new InMemoryResolver();
        resolver.registerEntityType(EntityType.of("ent-input", "Input"));
        resolver.registerAction(Action.of("act-input-text", "Input text", "input_text", "Enter text", "act-type-data-input"));
        resolver.registerAction(Action.of("act-click", "Click", "click", "Click button", "act-type-interaction"));
    }

    @Test
    void shouldParseValidJsonFromLlm() {
        CapturingLLMClient client = new CapturingLLMClient(
            "{\"entityTypeId\":\"ent-input\",\"actionIds\":[\"act-input-text\",\"act-click\"],\"parameters\":{\"meta_value\":\"java\"},\"clarificationNeeded\":false,\"clarificationQuestion\":null}"
        );

        UserRequestParser parser = new UserRequestParser(client, resolver);
        ParsedUserRequest parsed = parser.parse("введи java и нажми");

        assertEquals("ent-input", parsed.entityTypeId());
        assertEquals(2, parsed.actionIds().size());
        assertEquals("java", parsed.parameters().get("meta_value"));
        assertFalse(parsed.clarificationNeeded());
    }

    @Test
    void shouldSetClarificationNeededForInvalidJson() {
        CapturingLLMClient client = new CapturingLLMClient("not-json");
        UserRequestParser parser = new UserRequestParser(client, resolver);

        ParsedUserRequest parsed = parser.parse("сделай что-то");

        assertTrue(parsed.clarificationNeeded());
        assertTrue(parsed.clarificationQuestion().contains("Уточните"));
    }

    @Test
    void shouldIncludeEntityTypesAndActionsInPrompt() {
        CapturingLLMClient client = new CapturingLLMClient(
            "{\"entityTypeId\":\"ent-input\",\"actionIds\":[],\"parameters\":{},\"clarificationNeeded\":false,\"clarificationQuestion\":null}"
        );
        UserRequestParser parser = new UserRequestParser(client, resolver);

        parser.parse("простой запрос");

        assertTrue(client.lastPrompt.contains("ent-input"));
        assertTrue(client.lastPrompt.contains("act-input-text"));
        assertTrue(client.lastPrompt.contains("простой запрос"));
    }

    private static class CapturingLLMClient implements LLMClient {
        private final String responseContent;
        private String lastPrompt = "";

        private CapturingLLMClient(String responseContent) {
            this.responseContent = responseContent;
        }

        @Override
        public LLMResponse complete(String systemPrompt, String userMessage) {
            this.lastPrompt = systemPrompt + "\n" + userMessage;
            return new LLMResponse(responseContent, true, null, 1);
        }

        @Override
        public LLMResponse complete(String prompt) {
            this.lastPrompt = prompt;
            return new LLMResponse(responseContent, true, null, 1);
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }
}
