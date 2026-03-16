package com.zaborstik.platform.knowledge.llm;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptTemplateTest {

    @Test
    void loadShouldReadResource() {
        String template = PromptTemplate.load("prompts/parse-user-request.txt");
        assertFalse(template.isBlank());
    }

    @Test
    void fillShouldReplaceKnownVariable() {
        String result = PromptTemplate.fill("Hello {{name}}", Map.of("name", "World"));
        assertEquals("Hello World", result);
    }

    @Test
    void fillShouldLeaveUnknownPlaceholderAsIs() {
        String result = PromptTemplate.fill("Hello {{name}} {{unknown}}", Map.of("name", "World"));
        assertEquals("Hello World {{unknown}}", result);
    }

    @Test
    void loadAndFillShouldCombineOperations() {
        String result = PromptTemplate.loadAndFill(
            "prompts/clarify-request.txt",
            Map.of("user_input", "найди кнопку", "context", "страница поиска")
        );
        assertTrue(result.contains("найди кнопку"));
        assertTrue(result.contains("страница поиска"));
    }

    @Test
    void fillShouldReplaceMultipleVariables() {
        String result = PromptTemplate.fill("{{a}} {{b}} {{c}}", Map.of("a", "1", "b", "2", "c", "3"));
        assertEquals("1 2 3", result);
    }
}
