package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.CreatePlanRequest;
import com.zaborstik.platform.api.dto.PlanResponse;
import com.zaborstik.platform.api.entity.PlanEntity;
import com.zaborstik.platform.api.entity.WorkflowEntity;
import com.zaborstik.platform.api.entity.WorkflowStepEntity;
import com.zaborstik.platform.api.mapper.PlanMapper;
import com.zaborstik.platform.api.repository.*;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanMapper planMapper;

    @Mock
    private PlanResultRepository planResultRepository;

    @Mock
    private PlanStepLogRepository PlanStepLogRepository;

    @Mock
    private ActionRepository actionRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private PlanStepRepository planStepRepository;

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @InjectMocks
    private PlanService planService;

    private CreatePlanRequest createPlanRequest;

    @BeforeEach
    void setUp() {
        createPlanRequest = new CreatePlanRequest();
        createPlanRequest.setWorkflowId("wf-plan");
        createPlanRequest.setTarget("Test target");
        createPlanRequest.setExplanation("Test explanation");
        CreatePlanRequest.PlanStepRequest step = new CreatePlanRequest.PlanStepRequest();
        step.setWorkflowId("wf-plan-step");
        step.setEntityTypeId("et-building");
        step.setEntityId("e-1");
        step.setSortOrder(0);
        step.setDisplayName("Step 1");
        step.setActions(List.of(createActionRequest("act-1", "meta")));
        createPlanRequest.setSteps(List.of(step));
    }

    @Test
    void shouldCreatePlanAndReturnResponse() {
        when(workflowRepository.findById("wf-plan")).thenReturn(Optional.of(workflowWithFirstStep("wf-plan", "plan_first")));
        when(workflowRepository.findById("wf-plan-step")).thenReturn(Optional.of(workflowWithFirstStep("wf-plan-step", "step_first")));

        PlanEntity savedEntity = new PlanEntity();
        Plan planForDomain = new Plan("plan-1", "wf-plan", "plan_first", "step-1", "Test target", "Test explanation",
                List.of(new PlanStep("step-1", "plan-1", "wf-plan-step", "step_first", "et-building", "e-1", 0, "Step 1",
                        List.of(new PlanStepAction("act-1", "meta")))));

        when(planMapper.toEntity(any(Plan.class))).thenAnswer(inv -> {
            Plan p = inv.getArgument(0);
            savedEntity.setId(p.id());
            return savedEntity;
        });
        when(planRepository.save(any(PlanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planMapper.toDomain(any(PlanEntity.class))).thenReturn(planForDomain);

        PlanResponse result = planService.createPlan(createPlanRequest);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("wf-plan", result.getWorkflowId());
        assertEquals("Test target", result.getTarget());
        verify(planRepository, times(1)).save(any(PlanEntity.class));

        ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
        verify(planMapper).toEntity(planCaptor.capture());
        Plan built = planCaptor.getValue();
        assertEquals("plan_first", built.workflowStepInternalName());
        assertEquals(1, built.steps().size());
        assertEquals("step_first", built.steps().get(0).workflowStepInternalName());
    }

    @Test
    void shouldGetPlanWhenFound() {
        PlanEntity entity = new PlanEntity();
        entity.setId("plan-1");
        Plan plan = new Plan("plan-1", "wf-plan", "new", "step-1", "Target", "Explanation", List.of());

        when(planRepository.findById("plan-1")).thenReturn(Optional.of(entity));
        when(planMapper.toDomain(entity)).thenReturn(plan);

        Optional<PlanResponse> result = planService.getPlan("plan-1");

        assertTrue(result.isPresent());
        assertEquals("plan-1", result.get().getId());
        assertEquals("Target", result.get().getTarget());
    }

    @Test
    void shouldReturnEmptyWhenPlanNotFound() {
        when(planRepository.findById("missing")).thenReturn(Optional.empty());

        Optional<PlanResponse> result = planService.getPlan("missing");

        assertTrue(result.isEmpty());
        verify(planMapper, never()).toDomain(any());
    }

    private static CreatePlanRequest.PlanStepActionRequest createActionRequest(String actionId, String metaValue) {
        CreatePlanRequest.PlanStepActionRequest a = new CreatePlanRequest.PlanStepActionRequest();
        a.setActionId(actionId);
        a.setMetaValue(metaValue);
        return a;
    }

    private static WorkflowEntity workflowWithFirstStep(String workflowId, String firstInternalName) {
        WorkflowEntity wf = new WorkflowEntity();
        wf.setId(workflowId);
        WorkflowStepEntity first = new WorkflowStepEntity();
        first.setInternalname(firstInternalName);
        wf.setFirststep(first);
        return wf;
    }
}
