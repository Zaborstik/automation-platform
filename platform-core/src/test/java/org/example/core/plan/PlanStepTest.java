package org.example.core.plan;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanStepTest {

    @Test
    void shouldCreatePlanStepWithAllFields() {
        Map<String, Object> parameters = Map.of("timeout", 5000);
        PlanStep step = new PlanStep("click", "action(order_egrn_extract)", "Кликаю на элемент", parameters);

        assertEquals("click", step.getType());
        assertEquals("action(order_egrn_extract)", step.getTarget());
        assertEquals("Кликаю на элемент", step.getExplanation());
        assertEquals(parameters, step.getParameters());
    }

    @Test
    void shouldCreatePlanStepWithNullTarget() {
        PlanStep step = new PlanStep("explain", null, "Объяснение", Map.of());

        assertEquals("explain", step.getType());
        assertNull(step.getTarget());
    }

    @Test
    void shouldCreatePlanStepWithNullExplanation() {
        PlanStep step = new PlanStep("click", "selector", null, Map.of());

        assertEquals("click", step.getType());
        assertNull(step.getExplanation());
    }

    @Test
    void shouldCreatePlanStepWithNullParameters() {
        PlanStep step = new PlanStep("click", "selector", "explanation", null);

        assertTrue(step.getParameters().isEmpty());
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

        Map<String, Object> returnedParams = step.getParameters();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedParams.put("newKey", "newValue");
        });
    }

    @Test
    void shouldCreateOpenPageStep() {
        PlanStep step = PlanStep.openPage("/buildings/123", "Открываю страницу");

        assertEquals("open_page", step.getType());
        assertEquals("/buildings/123", step.getTarget());
        assertEquals("Открываю страницу", step.getExplanation());
        assertTrue(step.getParameters().isEmpty());
    }

    @Test
    void shouldCreateExplainStep() {
        PlanStep step = PlanStep.explain("Выполняю действие");

        assertEquals("explain", step.getType());
        assertNull(step.getTarget());
        assertEquals("Выполняю действие", step.getExplanation());
    }

    @Test
    void shouldCreateHoverStep() {
        PlanStep step = PlanStep.hover("order_egrn_extract", "Навожу курсор");

        assertEquals("hover", step.getType());
        assertEquals("action(order_egrn_extract)", step.getTarget());
        assertEquals("Навожу курсор", step.getExplanation());
    }

    @Test
    void shouldCreateClickStep() {
        PlanStep step = PlanStep.click("order_egrn_extract", "Кликаю");

        assertEquals("click", step.getType());
        assertEquals("action(order_egrn_extract)", step.getTarget());
        assertEquals("Кликаю", step.getExplanation());
    }

    @Test
    void shouldCreateWaitStep() {
        PlanStep step = PlanStep.wait("result", "Ожидаю результат");

        assertEquals("wait", step.getType());
        assertEquals("result", step.getTarget());
        assertEquals("Ожидаю результат", step.getExplanation());
    }

    @Test
    void shouldCreateTypeStep() {
        PlanStep step = PlanStep.type("#input", "текст", "Ввожу текст");

        assertEquals("type", step.getType());
        assertEquals("#input", step.getTarget());
        assertEquals("Ввожу текст", step.getExplanation());
        assertEquals("текст", step.getParameters().get("text"));
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

