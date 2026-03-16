package com.zaborstik.platform.knowledge.llm;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StubLLMClientTest {

    @Test
    void shouldReturnMappedResponseWhenPromptContainsKey() {
        StubLLMClient client = new StubLLMClient(Map.of("поиск", "{\"entityTypeId\":\"ent-input\"}"), "{\"default\":true}");
        LLMResponse response = client.complete("выполни поиск по сайту");
        assertTrue(response.success());
        assertEquals("{\"entityTypeId\":\"ent-input\"}", response.content());
    }

    @Test
    void shouldReturnDefaultResponseWhenNoMappingFound() {
        StubLLMClient client = new StubLLMClient(Map.of("поиск", "{\"ok\":true}"), "{\"default\":true}");
        LLMResponse response = client.complete("неизвестный запрос");
        assertEquals("{\"default\":true}", response.content());
    }

    @Test
    void shouldAlwaysBeAvailable() {
        StubLLMClient client = new StubLLMClient("{\"default\":true}");
        assertTrue(client.isAvailable());
    }

    @Test
    void completeShouldReturnSuccessfulResponse() {
        StubLLMClient client = new StubLLMClient("{\"default\":true}");
        LLMResponse response = client.complete("prompt");
        assertTrue(response.success());
    }
}
