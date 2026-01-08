package com.zaborstik.platform.core.plan;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanStepTest {

    @Test
    void shouldCreatePlanStepWithAllFields() {
        Map<String, Object> parameters = Map.of("timeout", 5000);
        PlanStep step = new PlanStep("click", "action(order_egrn_extract)", "Кликаю на элемент", parameters);

        assertEquals("click", step.type());
        assertEquals("action(order_egrn_extract)", step.target());
        assertEquals("Кликаю на элемент", step.explanation());
        assertEquals(parameters, step.parameters());
    }

    @Test
    void shouldCreatePlanStepWithNullTarget() {
        PlanStep step = new PlanStep("explain", null, "Объяснение", Map.of());

        assertEquals("explain", step.type());
        assertNull(step.target());
    }

    @Test
    void shouldCreatePlanStepWithNullExplanation() {
        PlanStep step = new PlanStep("click", "selector", null, Map.of());

        assertEquals("click", step.type());
        assertNull(step.explanation());
    }

    @Test
    void shouldCreatePlanStepWithNullParameters() {
        PlanStep step = new PlanStep("click", "selector", "explanation", null);

        assertTrue(step.parameters().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new PlanStep(null, "target", "explanation", Map.of());
        });
    }

    @Test
    void shouldReturnImmutableParameters() {
        Map<String, Object> originalParams = Map.of("key", "value");
        PlanStep step = new PlanStep("type", "target", "explanation", originalParams);

        Map<String, Object> returnedParams = step.parameters();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedParams.put("newKey", "newValue");
        });
    }

    @Test
    void shouldCreateOpenPageStep() {
        PlanStep step = PlanStep.openPage("/buildings/123", "Открываю страницу");

        assertEquals("open_page", step.type());
        assertEquals("/buildings/123", step.target());
        assertEquals("Открываю страницу", step.explanation());
        assertTrue(step.parameters().isEmpty());
    }

    @Test
    void shouldCreateExplainStep() {
        PlanStep step = PlanStep.explain("Выполняю действие");

        assertEquals("explain", step.type());
        assertNull(step.target());
        assertEquals("Выполняю действие", step.explanation());
    }

    @Test
    void shouldCreateHoverStep() {
        PlanStep step = PlanStep.hover("order_egrn_extract", "Навожу курсор");

        assertEquals("hover", step.type());
        assertEquals("action(order_egrn_extract)", step.target());
        assertEquals("Навожу курсор", step.explanation());
    }

    @Test
    void shouldCreateClickStep() {
        PlanStep step = PlanStep.click("order_egrn_extract", "Кликаю");

        assertEquals("click", step.type());
        assertEquals("action(order_egrn_extract)", step.target());
        assertEquals("Кликаю", step.explanation());
    }

    @Test
    void shouldCreateWaitStep() {
        PlanStep step = PlanStep.wait("result", "Ожидаю результат");

        assertEquals("wait", step.type());
        assertEquals("result", step.target());
        assertEquals("Ожидаю результат", step.explanation());
    }

    @Test
    void shouldCreateTypeStep() {
        PlanStep step = PlanStep.type("#input", "текст", "Ввожу текст");

        assertEquals("type", step.type());
        assertEquals("#input", step.target());
        assertEquals("Ввожу текст", step.explanation());
        assertEquals("текст", step.parameters().get("text"));
    }

    @Test
    void shouldReturnCorrectToString() {
        PlanStep step = new PlanStep("click", "action(id)", "Объяснение", Map.of());
        String toString = step.toString();

        assertTrue(toString.contains("click"));
        assertTrue(toString.contains("action(id)"));
        assertTrue(toString.contains("Объяснение"));
        assertTrue(toString.contains("PlanStep"));
    }
}

