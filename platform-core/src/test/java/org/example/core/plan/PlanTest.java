package org.example.core.plan;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlanTest {

    @Test
    void shouldCreatePlanWithAllFields() {
        List<PlanStep> steps = List.of(
            PlanStep.openPage("/buildings/123", "Открываю страницу"),
            PlanStep.click("action", "Кликаю")
        );

        Plan plan = new Plan("Building", "123", "order_egrn_extract", steps);

        assertNotNull(plan.getId());
        assertEquals("Building", plan.getEntityTypeId());
        assertEquals("123", plan.getEntityId());
        assertEquals("order_egrn_extract", plan.getActionId());
        assertEquals(2, plan.getSteps().size());
        assertEquals(Plan.PlanStatus.CREATED, plan.getStatus());
    }

    @Test
    void shouldCreatePlanWithEmptySteps() {
        Plan plan = new Plan("Building", "123", "action", List.of());

        assertEquals("Building", plan.getEntityTypeId());
        assertTrue(plan.getSteps().isEmpty());
        assertEquals(Plan.PlanStatus.CREATED, plan.getStatus());
    }

    @Test
    void shouldCreatePlanWithNullSteps() {
        Plan plan = new Plan("Building", "123", "action", null);

        assertTrue(plan.getSteps().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenEntityTypeIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new Plan(null, "123", "action", List.of());
        });
    }

    @Test
    void shouldThrowExceptionWhenEntityIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new Plan("Building", null, "action", List.of());
        });
    }

    @Test
    void shouldThrowExceptionWhenActionIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new Plan("Building", "123", null, List.of());
        });
    }

    @Test
    void shouldReturnImmutableSteps() {
        List<PlanStep> originalSteps = new ArrayList<>();
        originalSteps.add(PlanStep.explain("Шаг 1"));
        
        Plan plan = new Plan("Building", "123", "action", originalSteps);
        
        List<PlanStep> returnedSteps = plan.getSteps();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedSteps.add(PlanStep.explain("Шаг 2"));
        });
    }

    @Test
    void shouldCreatePlanWithCustomIdAndStatus() {
        String customId = "custom-plan-id";
        List<PlanStep> steps = List.of(PlanStep.explain("Шаг"));
        
        Plan plan = new Plan(customId, "Building", "123", "action", steps, Plan.PlanStatus.EXECUTING);

        assertEquals(customId, plan.getId());
        assertEquals(Plan.PlanStatus.EXECUTING, plan.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenIdIsNullInFullConstructor() {
        assertThrows(NullPointerException.class, () -> {
            new Plan(null, "Building", "123", "action", List.of(), Plan.PlanStatus.CREATED);
        });
    }

    @Test
    void shouldThrowExceptionWhenStatusIsNullInFullConstructor() {
        assertThrows(NullPointerException.class, () -> {
            new Plan("id", "Building", "123", "action", List.of(), null);
        });
    }

    @Test
    void shouldCreateNewPlanWithUpdatedStatus() {
        Plan originalPlan = new Plan("Building", "123", "action", List.of());
        assertEquals(Plan.PlanStatus.CREATED, originalPlan.getStatus());

        Plan updatedPlan = originalPlan.withStatus(Plan.PlanStatus.EXECUTING);
        
        assertEquals(Plan.PlanStatus.EXECUTING, updatedPlan.getStatus());
        assertEquals(originalPlan.getId(), updatedPlan.getId());
        assertEquals(originalPlan.getEntityTypeId(), updatedPlan.getEntityTypeId());
        assertEquals(originalPlan.getEntityId(), updatedPlan.getEntityId());
        assertEquals(originalPlan.getActionId(), updatedPlan.getActionId());
        assertEquals(originalPlan.getSteps(), updatedPlan.getSteps());
    }

    @Test
    void shouldSupportAllPlanStatuses() {
        List<PlanStep> steps = List.of(PlanStep.explain("Шаг"));
        
        Plan created = new Plan("id", "Building", "123", "action", steps, Plan.PlanStatus.CREATED);
        Plan executing = new Plan("id", "Building", "123", "action", steps, Plan.PlanStatus.EXECUTING);
        Plan completed = new Plan("id", "Building", "123", "action", steps, Plan.PlanStatus.COMPLETED);
        Plan failed = new Plan("id", "Building", "123", "action", steps, Plan.PlanStatus.FAILED);
        Plan cancelled = new Plan("id", "Building", "123", "action", steps, Plan.PlanStatus.CANCELLED);

        assertEquals(Plan.PlanStatus.CREATED, created.getStatus());
        assertEquals(Plan.PlanStatus.EXECUTING, executing.getStatus());
        assertEquals(Plan.PlanStatus.COMPLETED, completed.getStatus());
        assertEquals(Plan.PlanStatus.FAILED, failed.getStatus());
        assertEquals(Plan.PlanStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void shouldReturnCorrectToString() {
        Plan plan = new Plan("Building", "123", "order_egrn_extract", List.of(PlanStep.explain("Шаг")));
        String toString = plan.toString();

        assertTrue(toString.contains("Building"));
        assertTrue(toString.contains("123"));
        assertTrue(toString.contains("order_egrn_extract"));
        assertTrue(toString.contains("CREATED"));
        assertTrue(toString.contains("Plan"));
    }
}

