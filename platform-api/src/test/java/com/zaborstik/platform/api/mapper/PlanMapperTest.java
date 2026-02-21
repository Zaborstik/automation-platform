package com.zaborstik.platform.api.mapper;

import com.zaborstik.platform.api.dto.EntityDTO;
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
    void shouldConvertPlanToEntityDTO() {
        List<PlanStep> steps = List.of(
            PlanStep.openPage("/buildings/93939", "Открываю карточку"),
            PlanStep.explain("Выполняю действие"),
            PlanStep.click("order_egrn_extract", "Кликаю")
        );
        Plan plan = new Plan("Building", "93939", "order_egrn_extract", steps);

        EntityDTO dto = planMapper.toEntityDTO(plan);

        assertNotNull(dto);
        assertEquals(EntityDTO.TABLE_PLANS, dto.getTableName());
        assertEquals(plan.id(), dto.getId());
        assertEquals("Building", dto.get("entityTypeId"));
        assertEquals("93939", dto.get("entityId"));
        assertEquals("order_egrn_extract", dto.get("actionId"));
        assertEquals(Plan.PlanStatus.CREATED.name(), dto.get("status"));
        assertEquals(3, ((List<?>) dto.get("steps")).size());
    }

    @Test
    void shouldConvertEntityDTOToPlan() {
        EntityDTO dto = new EntityDTO(EntityDTO.TABLE_PLANS, "plan-id",
                Map.of(
                        "entityTypeId", "Building",
                        "entityId", "93939",
                        "actionId", "order_egrn_extract",
                        "status", "CREATED",
                        "steps", List.of(
                                Map.of("type", "open_page", "target", "/buildings/93939", "explanation", "Открываю карточку"),
                                Map.of("type", "click", "target", "action(order_egrn_extract)", "explanation", "Кликаю")
                        )
                ));

        Plan plan = planMapper.toPlan(dto);

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
        PlanStep step = PlanStep.type("input", "test text", "Ввожу текст");
        Plan plan = new Plan("Building", "93939", "order_egrn_extract", List.of(step));

        EntityDTO dto = planMapper.toEntityDTO(plan);

        assertNotNull(dto);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stepsData = (List<Map<String, Object>>) dto.get("steps");
        assertEquals(1, stepsData.size());
        assertEquals("type", stepsData.get(0).get("type"));
        assertEquals("test text", ((Map<?, ?>) stepsData.get(0).get("parameters")).get("text"));
    }

    @Test
    void shouldConvertAllPlanStatuses() {
        for (Plan.PlanStatus status : Plan.PlanStatus.values()) {
            EntityDTO dto = new EntityDTO(EntityDTO.TABLE_PLANS, "id",
                    Map.of("entityTypeId", "B", "entityId", "1", "actionId", "a", "status", status.name(), "steps", List.of()));
            Plan plan = planMapper.toPlan(dto);
            assertEquals(status, plan.status());
        }
    }

    @Test
    void shouldHandleEmptySteps() {
        Plan plan = new Plan("Building", "93939", "order_egrn_extract", List.of());

        EntityDTO dto = planMapper.toEntityDTO(plan);

        assertNotNull(dto);
        assertTrue(((List<?>) dto.get("steps")).isEmpty());
    }

    @Test
    void shouldPreserveStepOrder() {
        List<PlanStep> steps = List.of(
            PlanStep.openPage("/page", "Step 1"),
            PlanStep.explain("Step 2"),
            PlanStep.click("action", "Step 3")
        );
        Plan plan = new Plan("Building", "93939", "order_egrn_extract", steps);

        EntityDTO dto = planMapper.toEntityDTO(plan);
        Plan convertedPlan = planMapper.toPlan(dto);

        assertEquals(3, convertedPlan.steps().size());
        assertEquals("open_page", convertedPlan.steps().get(0).type());
        assertEquals("explain", convertedPlan.steps().get(1).type());
        assertEquals("click", convertedPlan.steps().get(2).type());
    }
}
