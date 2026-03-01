package com.zaborstik.platform.api.mapper;

import com.zaborstik.platform.api.entity.*;
import com.zaborstik.platform.api.repository.*;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanMapperTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkflowStepRepository workflowStepRepository;

    @Mock
    private EntityTypeRepository entityTypeRepository;

    @Mock
    private ActionRepository actionRepository;

    private PlanMapper planMapper;

    private WorkflowEntity workflowEntity;
    private EntityTypeEntity entityTypeEntity;
    private ActionEntity actionEntity;

    @BeforeEach
    void setUp() {
        planMapper = new PlanMapper(workflowRepository, workflowStepRepository, entityTypeRepository, actionRepository);

        workflowEntity = new WorkflowEntity();
        workflowEntity.setId("wf-plan");

        entityTypeEntity = new EntityTypeEntity();
        entityTypeEntity.setId("ent-page");

        actionEntity = new ActionEntity();
        actionEntity.setId("act-open-page");

        when(workflowRepository.findById("wf-plan")).thenReturn(Optional.of(workflowEntity));
        when(workflowRepository.findById("wf-plan-step")).thenReturn(Optional.of(workflowEntity));
        lenient().when(entityTypeRepository.findById("ent-page")).thenReturn(Optional.of(entityTypeEntity));
        lenient().when(actionRepository.findById("act-open-page")).thenReturn(Optional.of(actionEntity));
    }

    @Test
    void shouldConvertPlanToEntity() {
        Plan plan = new Plan(
                "plan-1",
                "wf-plan",
                "new",
                "step-1",
                "Target",
                "Explanation",
                List.of(new PlanStep(
                        "step-1",
                        "plan-1",
                        "wf-plan-step",
                        "new",
                        "ent-page",
                        "page-1",
                        0,
                        "Open page",
                        List.of(new PlanStepAction("act-open-page", "https://example.com"))
                ))
        );

        PlanEntity entity = planMapper.toEntity(plan);

        assertNotNull(entity);
        assertEquals("plan-1", entity.getId());
        assertEquals("wf-plan", entity.getWorkflow().getId());
        assertEquals("new", entity.getWorkflowStepInternalname());
        assertEquals("step-1", entity.getStoppedAtPlanStep());
        assertEquals("Target", entity.getTarget());
        assertEquals("Explanation", entity.getExplanation());
        assertEquals(1, entity.getSteps().size());
        PlanStepEntity stepEntity = entity.getSteps().get(0);
        assertEquals("step-1", stepEntity.getId());
        assertEquals("ent-page", stepEntity.getEntitytype().getId());
        assertEquals("page-1", stepEntity.getEntityId());
        assertEquals(0, stepEntity.getSortorder());
        assertEquals("Open page", stepEntity.getDisplayname());
        assertEquals(1, stepEntity.getActions().size());
        assertEquals("act-open-page", stepEntity.getActions().get(0).getAction().getId());
        assertEquals("https://example.com", stepEntity.getActions().get(0).getMetaValue());
    }

    @Test
    void shouldConvertEntityToDomain() {
        PlanEntity entity = new PlanEntity();
        entity.setId("plan-1");
        entity.setWorkflow(workflowEntity);
        entity.setWorkflowStepInternalname("new");
        entity.setStoppedAtPlanStep("step-1");
        entity.setTarget("Target");
        entity.setExplanation("Explanation");

        PlanStepEntity stepEntity = new PlanStepEntity();
        stepEntity.setId("step-1");
        stepEntity.setPlan(entity);
        stepEntity.setWorkflow(workflowEntity);
        stepEntity.setWorkflowStepInternalname("new");
        stepEntity.setEntitytype(entityTypeEntity);
        stepEntity.setEntityId("page-1");
        stepEntity.setSortorder(0);
        stepEntity.setDisplayname("Open page");
        PlanStepActionEntity psa = new PlanStepActionEntity();
        psa.setPlanStep(stepEntity);
        psa.setAction(actionEntity);
        psa.setMetaValue("https://example.com");
        stepEntity.setActions(List.of(psa));
        entity.setSteps(List.of(stepEntity));

        Plan plan = planMapper.toDomain(entity);

        assertNotNull(plan);
        assertEquals("plan-1", plan.id());
        assertEquals("wf-plan", plan.workflowId());
        assertEquals("new", plan.workflowStepInternalName());
        assertEquals("step-1", plan.stoppedAtPlanStepId());
        assertEquals("Target", plan.target());
        assertEquals("Explanation", plan.explanation());
        assertEquals(1, plan.steps().size());
        assertEquals("step-1", plan.steps().get(0).id());
        assertEquals("ent-page", plan.steps().get(0).entityTypeId());
        assertEquals("page-1", plan.steps().get(0).entityId());
        assertEquals(0, plan.steps().get(0).sortOrder());
        assertEquals("Open page", plan.steps().get(0).displayName());
        assertEquals(1, plan.steps().get(0).actions().size());
        assertEquals("act-open-page", plan.steps().get(0).actions().get(0).actionId());
        assertEquals("https://example.com", plan.steps().get(0).actions().get(0).metaValue());
    }

    @Test
    void shouldHandleEmptySteps() {
        Plan plan = new Plan("plan-1", "wf-plan", "new", "plan-1", "Target", "Explanation", List.of());
        PlanEntity entity = planMapper.toEntity(plan);
        assertNotNull(entity);
        assertTrue(entity.getSteps().isEmpty());
        Plan back = planMapper.toDomain(entity);
        assertTrue(back.steps().isEmpty());
    }
}
