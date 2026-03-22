package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.CreatePlanRequest;
import com.zaborstik.platform.api.dto.PlanResponse;
import com.zaborstik.platform.api.entity.PlanEntity;
import com.zaborstik.platform.api.mapper.PlanMapper;
import com.zaborstik.platform.api.repository.*;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private PlanStepLogEntryRepository planStepLogEntryRepository;

    @Mock
    private ActionRepository actionRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private PlanStepRepository planStepRepository;

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @InjectMocks
    private PlanService planService;

    private CreatePlanRequest createPlanRequest;

    @BeforeEach
    void setUp() {
        createPlanRequest = new CreatePlanRequest();
        createPlanRequest.setWorkflowId("wf-plan");
        createPlanRequest.setWorkflowStepInternalName("new");
        createPlanRequest.setTarget("Test target");
        createPlanRequest.setExplanation("Test explanation");
        CreatePlanRequest.PlanStepRequest step = new CreatePlanRequest.PlanStepRequest();
        step.setWorkflowId("wf-plan");
        step.setWorkflowStepInternalName("new");
        step.setEntityTypeId("et-building");
        step.setEntityId("e-1");
        step.setSortOrder(0);
        step.setDisplayName("Step 1");
        step.setActions(List.of(createActionRequest("act-1", "meta")));
        createPlanRequest.setSteps(List.of(step));
    }

    @Test
    void shouldCreatePlanAndReturnResponse() {
        PlanEntity savedEntity = new PlanEntity();
        Plan planForDomain = new Plan("plan-1", "wf-plan", "new", "step-1", "Test target", "Test explanation",
                List.of(new PlanStep("step-1", "plan-1", "wf-plan", "new", "et-building", "e-1", 0, "Step 1",
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

    @Test
    void shouldCreatePlanWithEmptyStepsAndSetStoppedAtPlanStepToEmpty() {
        CreatePlanRequest emptyRequest = new CreatePlanRequest();
        emptyRequest.setWorkflowId("wf-plan");
        emptyRequest.setWorkflowStepInternalName("new");
        emptyRequest.setTarget("Empty plan");
        emptyRequest.setExplanation("No steps");
        emptyRequest.setSteps(List.of());

        PlanEntity savedEntity = new PlanEntity();
        when(planMapper.toEntity(any(Plan.class))).thenAnswer(inv -> {
            Plan p = inv.getArgument(0);
            savedEntity.setId(p.id());
            savedEntity.setStoppedAtPlanStep(p.stoppedAtPlanStepId());
            return savedEntity;
        });
        when(planRepository.save(any(PlanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planMapper.toDomain(any(PlanEntity.class))).thenAnswer(inv -> {
            PlanEntity e = inv.getArgument(0);
            Plan p = new Plan(e.getId(), "wf-plan", "new", e.getStoppedAtPlanStep(),
                    "Empty plan", "No steps", List.of());
            return p;
        });

        PlanResponse result = planService.createPlan(emptyRequest);

        assertNotNull(result);
        assertEquals(0, result.getSteps().size());
        assertEquals("", result.getStoppedAtPlanStepId(),
                "When steps are empty, stoppedAtPlanStepId is empty string (no step to reference)");
    }

    private static CreatePlanRequest.PlanStepActionRequest createActionRequest(String actionId, String metaValue) {
        CreatePlanRequest.PlanStepActionRequest a = new CreatePlanRequest.PlanStepActionRequest();
        a.setActionId(actionId);
        a.setMetaValue(metaValue);
        return a;
    }
}
