package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.*;
import com.zaborstik.platform.api.mapper.PlanMapper;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@EntityScan("com.zaborstik.platform.api.entity")
@Import(PlanMapper.class)
@ActiveProfiles({"dev", "datajpa"})
class RepositoryIntegrationTest {

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private ActionTypeRepository actionTypeRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowStepRepository workflowStepRepository;

    @Autowired
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanStepRepository planStepRepository;

    @Autowired
    private PlanStepActionRepository planStepActionRepository;

    @Autowired
    private PlanMapper planMapper;

    @BeforeEach
    void setUp() {
        planRepository.deleteAll();
        actionRepository.deleteAll();
        entityTypeRepository.deleteAll();
        actionTypeRepository.deleteAll();
        workflowTransitionRepository.deleteAll();
        workflowRepository.deleteAll();
        workflowStepRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindEntityType() {
        EntityTypeEntity et = new EntityTypeEntity();
        et.setId("ent-page");
        et.setDisplayname("Страница");
        entityTypeRepository.save(et);

        Optional<EntityTypeEntity> found = entityTypeRepository.findById("ent-page");
        assertTrue(found.isPresent());
        assertEquals("ent-page", found.get().getId());
        assertEquals("Страница", found.get().getDisplayname());
    }

    @Test
    void shouldSaveAndFindAction() {
        ActionTypeEntity at = new ActionTypeEntity();
        at.setId("act-type-1");
        at.setInternalname("navigation");
        at.setDisplayname("Навигация");
        actionTypeRepository.save(at);

        EntityTypeEntity et = new EntityTypeEntity();
        et.setId("ent-page");
        et.setDisplayname("Страница");
        entityTypeRepository.save(et);

        ActionEntity a = new ActionEntity();
        a.setId("act-open-page");
        a.setDisplayname("Открыть страницу");
        a.setInternalname("open_page");
        a.setDescription("Переход по URL");
        a.setActionType(at);
        a.setApplicableEntityTypes(Set.of(et));
        actionRepository.save(a);

        Optional<ActionEntity> found = actionRepository.findById("act-open-page");
        assertTrue(found.isPresent());
        assertTrue(found.get().getApplicableEntityTypes().stream().anyMatch(e -> e.getId().equals("ent-page")));
    }

    @Test
    void shouldSaveAndFindPlanWithSteps() {
        WorkflowStepEntity wfs = new WorkflowStepEntity();
        wfs.setId("wfs-new");
        wfs.setInternalname("new");
        wfs.setDisplayname("Новая");
        wfs.setSortorder(10);
        workflowStepRepository.save(wfs);

        WorkflowEntity wf = new WorkflowEntity();
        wf.setId("wf-plan");
        wf.setDisplayname("ЖЦ плана");
        wf.setFirststep(wfs);
        workflowRepository.save(wf);

        ActionTypeEntity at = new ActionTypeEntity();
        at.setId("act-type-1");
        at.setInternalname("navigation");
        at.setDisplayname("Навигация");
        actionTypeRepository.save(at);

        EntityTypeEntity et = new EntityTypeEntity();
        et.setId("ent-page");
        et.setDisplayname("Страница");
        entityTypeRepository.save(et);

        ActionEntity action = new ActionEntity();
        action.setId("act-open-page");
        action.setDisplayname("Открыть страницу");
        action.setInternalname("open_page");
        action.setActionType(at);
        action.setApplicableEntityTypes(Set.of(et));
        actionRepository.save(action);

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
                        "wf-plan",
                        "new",
                        "ent-page",
                        "page-1",
                        0,
                        "Open page",
                        List.of(new PlanStepAction("act-open-page", "https://example.com"))
                ))
        );
        PlanEntity entity = planMapper.toEntity(plan);
        planRepository.save(entity);

        Optional<PlanEntity> found = planRepository.findById("plan-1");
        assertTrue(found.isPresent());
        assertEquals(1, found.get().getSteps().size());
        assertEquals("step-1", found.get().getSteps().get(0).getId());
        assertEquals(1, found.get().getSteps().get(0).getActions().size());
    }

    @Test
    void shouldDeletePlan() {
        WorkflowStepEntity wfs = new WorkflowStepEntity();
        wfs.setId("wfs-new");
        wfs.setInternalname("new");
        wfs.setDisplayname("Новая");
        wfs.setSortorder(10);
        workflowStepRepository.save(wfs);

        WorkflowEntity wf = new WorkflowEntity();
        wf.setId("wf-plan");
        wf.setDisplayname("ЖЦ плана");
        wf.setFirststep(wfs);
        workflowRepository.save(wf);

        PlanEntity plan = new PlanEntity();
        plan.setId("plan-to-delete");
        plan.setWorkflow(wf);
        plan.setWorkflowStepInternalname("new");
        plan.setStoppedAtPlanStep("plan-to-delete");
        planRepository.save(plan);

        planRepository.deleteById("plan-to-delete");
        assertTrue(planRepository.findById("plan-to-delete").isEmpty());
    }

    @Test
    void shouldFindPlanStepsByPlanIdOrderedBySortorder() {
        WorkflowStepEntity wfs = new WorkflowStepEntity();
        wfs.setId("wfs-new");
        wfs.setInternalname("new");
        wfs.setDisplayname("Новая");
        wfs.setSortorder(10);
        workflowStepRepository.save(wfs);

        WorkflowEntity wf = new WorkflowEntity();
        wf.setId("wf-plan");
        wf.setDisplayname("ЖЦ плана");
        wf.setFirststep(wfs);
        workflowRepository.save(wf);

        ActionTypeEntity at = new ActionTypeEntity();
        at.setId("act-type-1");
        at.setInternalname("navigation");
        at.setDisplayname("Навигация");
        actionTypeRepository.save(at);

        EntityTypeEntity et = new EntityTypeEntity();
        et.setId("ent-page");
        et.setDisplayname("Страница");
        entityTypeRepository.save(et);

        ActionEntity action = new ActionEntity();
        action.setId("act-open-page");
        action.setDisplayname("Открыть страницу");
        action.setInternalname("open_page");
        action.setActionType(at);
        action.setApplicableEntityTypes(Set.of(et));
        actionRepository.save(action);

        Plan plan = new Plan(
            "plan-ordered-1",
            "wf-plan",
            "new",
            "step-b",
            "Target",
            "Explanation",
            List.of(
                new PlanStep(
                    "step-b",
                    "plan-ordered-1",
                    "wf-plan",
                    "new",
                    "ent-page",
                    "page-2",
                    2,
                    "Second step",
                    List.of(new PlanStepAction("act-open-page", "https://example.com/2"))
                ),
                new PlanStep(
                    "step-a",
                    "plan-ordered-1",
                    "wf-plan",
                    "new",
                    "ent-page",
                    "page-1",
                    1,
                    "First step",
                    List.of(new PlanStepAction("act-open-page", "https://example.com/1"))
                )
            )
        );
        planRepository.save(planMapper.toEntity(plan));

        List<PlanStepEntity> steps = planStepRepository.findByPlan_IdOrderBySortorder("plan-ordered-1");
        assertEquals(2, steps.size());
        assertEquals("step-a", steps.get(0).getId());
        assertEquals("step-b", steps.get(1).getId());
    }

    @Test
    void shouldFindPlanStepByIdAndPlanId() {
        WorkflowStepEntity wfs = new WorkflowStepEntity();
        wfs.setId("wfs-new");
        wfs.setInternalname("new");
        wfs.setDisplayname("Новая");
        wfs.setSortorder(10);
        workflowStepRepository.save(wfs);

        WorkflowEntity wf = new WorkflowEntity();
        wf.setId("wf-plan");
        wf.setDisplayname("ЖЦ плана");
        wf.setFirststep(wfs);
        workflowRepository.save(wf);

        ActionTypeEntity at = new ActionTypeEntity();
        at.setId("act-type-1");
        at.setInternalname("navigation");
        at.setDisplayname("Навигация");
        actionTypeRepository.save(at);

        EntityTypeEntity et = new EntityTypeEntity();
        et.setId("ent-page");
        et.setDisplayname("Страница");
        entityTypeRepository.save(et);

        ActionEntity action = new ActionEntity();
        action.setId("act-open-page");
        action.setDisplayname("Открыть страницу");
        action.setInternalname("open_page");
        action.setActionType(at);
        action.setApplicableEntityTypes(Set.of(et));
        actionRepository.save(action);

        Plan plan = new Plan(
            "plan-find-step",
            "wf-plan",
            "new",
            "step-find",
            "Target",
            "Explanation",
            List.of(new PlanStep(
                "step-find",
                "plan-find-step",
                "wf-plan",
                "new",
                "ent-page",
                "page-1",
                1,
                "Only step",
                List.of(new PlanStepAction("act-open-page", "https://example.com"))
            ))
        );
        planRepository.save(planMapper.toEntity(plan));

        Optional<PlanStepEntity> found = planStepRepository.findByIdAndPlan_Id("step-find", "plan-find-step");
        assertTrue(found.isPresent());
        assertEquals("step-find", found.get().getId());
        assertEquals("plan-find-step", found.get().getPlan().getId());
        assertTrue(planStepRepository.findByIdAndPlan_Id("step-find", "another-plan").isEmpty());
    }

    @Test
    void shouldFindPlanStepActionsByPlanStepId() {
        WorkflowStepEntity wfs = new WorkflowStepEntity();
        wfs.setId("wfs-new");
        wfs.setInternalname("new");
        wfs.setDisplayname("Новая");
        wfs.setSortorder(10);
        workflowStepRepository.save(wfs);

        WorkflowEntity wf = new WorkflowEntity();
        wf.setId("wf-plan");
        wf.setDisplayname("ЖЦ плана");
        wf.setFirststep(wfs);
        workflowRepository.save(wf);

        ActionTypeEntity at = new ActionTypeEntity();
        at.setId("act-type-navigation");
        at.setInternalname("navigation");
        at.setDisplayname("Навигация");
        actionTypeRepository.save(at);

        EntityTypeEntity et = new EntityTypeEntity();
        et.setId("ent-page");
        et.setDisplayname("Страница");
        entityTypeRepository.save(et);

        ActionEntity action1 = new ActionEntity();
        action1.setId("act-open-page");
        action1.setDisplayname("Открыть страницу");
        action1.setInternalname("open_page");
        action1.setActionType(at);
        action1.setApplicableEntityTypes(Set.of(et));
        actionRepository.save(action1);

        ActionEntity action2 = new ActionEntity();
        action2.setId("act-open-page-2");
        action2.setDisplayname("Открыть страницу 2");
        action2.setInternalname("open_page_2");
        action2.setActionType(at);
        action2.setApplicableEntityTypes(Set.of(et));
        actionRepository.save(action2);

        Plan plan = new Plan(
            "plan-actions",
            "wf-plan",
            "new",
            "step-actions",
            "Target",
            "Explanation",
            List.of(new PlanStep(
                "step-actions",
                "plan-actions",
                "wf-plan",
                "new",
                "ent-page",
                "page-1",
                1,
                "Step with actions",
                List.of(
                    new PlanStepAction("act-open-page", "https://example.com/1"),
                    new PlanStepAction("act-open-page-2", "https://example.com/2")
                )
            ))
        );
        planRepository.save(planMapper.toEntity(plan));

        List<PlanStepActionEntity> actions = planStepActionRepository.findByPlanStep_Id("step-actions");
        assertEquals(2, actions.size());
        assertTrue(actions.stream().anyMatch(a -> "act-open-page".equals(a.getAction().getId())));
        assertTrue(actions.stream().anyMatch(a -> "act-open-page-2".equals(a.getAction().getId())));
    }
}
