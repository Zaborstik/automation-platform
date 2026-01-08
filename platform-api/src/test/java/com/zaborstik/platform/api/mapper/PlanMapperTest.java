package com.zaborstik.platform.api.mapper;

import com.zaborstik.platform.api.entity.PlanEntity;
import com.zaborstik.platform.api.entity.PlanStepEntity;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanMapperTest {

    private PlanMapper planMapper;

    @BeforeEach
    void setUp() {
        planMapper = new PlanMapper();
    }

    @Test
    void shouldConvertPlanToEntity() {
        // Given
        List<PlanStep> steps = List.of(
            PlanStep.openPage("/buildings/93939", "Открываю карточку"),
            PlanStep.explain("Выполняю действие"),
            PlanStep.click("order_egrn_extract", "Кликаю")
        );
        Plan plan = new Plan("Building", "93939", "order_egrn_extract", steps);

        // When
        PlanEntity entity = planMapper.toEntity(plan);

        // Then
        assertNotNull(entity);
        assertEquals(plan.id(), entity.getId());
        assertEquals("Building", entity.getEntityTypeId());
        assertEquals("93939", entity.getEntityId());
        assertEquals("order_egrn_extract", entity.getActionId());
        assertEquals(PlanEntity.PlanStatus.CREATED, entity.getStatus());
        assertEquals(3, entity.getSteps().size());
    }

    @Test
    void shouldConvertEntityToPlan() {
        // Given
        PlanEntity entity = new PlanEntity(
            "plan-id",
            "Building",
            "93939",
            "order_egrn_extract",
            PlanEntity.PlanStatus.CREATED
        );

        PlanStepEntity step1 = new PlanStepEntity(
            entity, 0, "open_page", "/buildings/93939", "Открываю карточку", Map.of()
        );
        PlanStepEntity step2 = new PlanStepEntity(
            entity, 1, "click", "action(order_egrn_extract)", "Кликаю", Map.of()
        );
        entity.setSteps(List.of(step1, step2));

        // When
        Plan plan = planMapper.toDomain(entity);

        // Then
        assertNotNull(plan);
        assertEquals("plan-id", plan.id());
        assertEquals("Building", plan.entityTypeId());
        assertEquals("93939", plan.entityId());
        assertEquals("order_egrn_extract", plan.actionId());
        assertEquals(Plan.PlanStatus.CREATED, plan.status());
        assertEquals(2, plan.steps().size());
        assertEquals("open_page", plan.steps().get(0).type());
        assertEquals("click", plan.steps().get(1).type());
    }

    @Test
    void shouldConvertPlanStepParameters() {
        // Given
        PlanStep step = PlanStep.type("input", "test text", "Ввожу текст");
        Plan plan = new Plan("Building", "93939", "order_egrn_extract", List.of(step));

        // When
        PlanEntity entity = planMapper.toEntity(plan);

        // Then
        assertNotNull(entity);
        assertEquals(1, entity.getSteps().size());
        PlanStepEntity stepEntity = entity.getSteps().get(0);
        assertEquals("type", stepEntity.getType());
        assertTrue(stepEntity.getParameters().containsKey("text"));
        assertEquals("test text", stepEntity.getParameters().get("text"));
    }

    @Test
    void shouldConvertAllPlanStatuses() {
        // Test CREATED
        PlanEntity entity1 = new PlanEntity("id1", "Building", "123", "action", PlanEntity.PlanStatus.CREATED);
        Plan plan1 = planMapper.toDomain(entity1);
        assertEquals(Plan.PlanStatus.CREATED, plan1.status());

        // Test EXECUTING
        PlanEntity entity2 = new PlanEntity("id2", "Building", "123", "action", PlanEntity.PlanStatus.EXECUTING);
        Plan plan2 = planMapper.toDomain(entity2);
        assertEquals(Plan.PlanStatus.EXECUTING, plan2.status());

        // Test COMPLETED
        PlanEntity entity3 = new PlanEntity("id3", "Building", "123", "action", PlanEntity.PlanStatus.COMPLETED);
        Plan plan3 = planMapper.toDomain(entity3);
        assertEquals(Plan.PlanStatus.COMPLETED, plan3.status());

        // Test FAILED
        PlanEntity entity4 = new PlanEntity("id4", "Building", "123", "action", PlanEntity.PlanStatus.FAILED);
        Plan plan4 = planMapper.toDomain(entity4);
        assertEquals(Plan.PlanStatus.FAILED, plan4.status());

        // Test CANCELLED
        PlanEntity entity5 = new PlanEntity("id5", "Building", "123", "action", PlanEntity.PlanStatus.CANCELLED);
        Plan plan5 = planMapper.toDomain(entity5);
        assertEquals(Plan.PlanStatus.CANCELLED, plan5.status());
    }

    @Test
    void shouldHandleEmptySteps() {
        // Given
        Plan plan = new Plan("Building", "93939", "order_egrn_extract", List.of());

        // When
        PlanEntity entity = planMapper.toEntity(plan);

        // Then
        assertNotNull(entity);
        assertTrue(entity.getSteps().isEmpty());
    }

    @Test
    void shouldPreserveStepOrder() {
        // Given
        List<PlanStep> steps = List.of(
            PlanStep.openPage("/page", "Step 1"),
            PlanStep.explain("Step 2"),
            PlanStep.click("action", "Step 3")
        );
        Plan plan = new Plan("Building", "93939", "order_egrn_extract", steps);

        // When
        PlanEntity entity = planMapper.toEntity(plan);
        Plan convertedPlan = planMapper.toDomain(entity);

        // Then
        assertEquals(3, convertedPlan.steps().size());
        assertEquals("open_page", convertedPlan.steps().get(0).type());
        assertEquals("explain", convertedPlan.steps().get(1).type());
        assertEquals("click", convertedPlan.steps().get(2).type());
    }
}
