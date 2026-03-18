package com.zaborstik.platform.api.resolver;

import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.ActionTypeEntity;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.entity.WorkflowEntity;
import com.zaborstik.platform.api.entity.WorkflowStepEntity;
import com.zaborstik.platform.api.entity.WorkflowTransitionEntity;
import com.zaborstik.platform.api.repository.ActionRepository;
import com.zaborstik.platform.api.repository.ActionTypeRepository;
import com.zaborstik.platform.api.repository.EntityTypeRepository;
import com.zaborstik.platform.api.repository.WorkflowRepository;
import com.zaborstik.platform.api.repository.WorkflowStepRepository;
import com.zaborstik.platform.api.repository.WorkflowTransitionRepository;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@EntityScan("com.zaborstik.platform.api.entity")
@Import(DatabaseResolver.class)
@ActiveProfiles({"dev", "datajpa"})
class DatabaseResolverTest {

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
    private DatabaseResolver databaseResolver;

    @BeforeEach
    void setUp() {
        actionRepository.deleteAll();
        entityTypeRepository.deleteAll();
        actionTypeRepository.deleteAll();
        workflowTransitionRepository.deleteAll();
        workflowRepository.deleteAll();
        workflowStepRepository.deleteAll();

        EntityTypeEntity et = new EntityTypeEntity();
        et.setId("Building");
        et.setDisplayname("Здание");
        entityTypeRepository.save(et);

        ActionTypeEntity actionType = new ActionTypeEntity();
        actionType.setId("act-type-1");
        actionType.setInternalname("interaction");
        actionType.setDisplayname("Взаимодействие");
        actionTypeRepository.save(actionType);

        ActionEntity action = new ActionEntity();
        action.setId("order_egrn_extract");
        action.setDisplayname("Заказать выписку из ЕГРН");
        action.setInternalname("order_egrn_extract");
        action.setDescription("Описание действия");
        action.setActionType(actionType);
        action.setApplicableEntityTypes(Set.of(et));
        actionRepository.save(action);

        WorkflowStepEntity newStep = new WorkflowStepEntity();
        newStep.setId("wfs-new");
        newStep.setInternalname("new");
        newStep.setDisplayname("New");
        newStep.setSortorder(10);
        workflowStepRepository.save(newStep);

        WorkflowEntity workflow = new WorkflowEntity();
        workflow.setId("wf-plan");
        workflow.setDisplayname("Plan workflow");
        workflow.setFirststep(newStep);
        workflowRepository.save(workflow);

        WorkflowTransitionEntity transition = new WorkflowTransitionEntity();
        transition.setId("wft-1");
        transition.setWorkflow(workflow);
        transition.setFromStep("new");
        transition.setToStep("in_progress");
        workflowTransitionRepository.save(transition);
    }

    @Test
    void shouldFindEntityType() {
        Optional<EntityType> result = databaseResolver.findEntityType("Building");
        assertTrue(result.isPresent());
        assertEquals("Building", result.get().id());
        assertEquals("Здание", result.get().displayName());
    }

    @Test
    void shouldReturnEmptyWhenEntityTypeNotFound() {
        assertFalse(databaseResolver.findEntityType("NonExistent").isPresent());
    }

    @Test
    void shouldFindAction() {
        Optional<Action> result = databaseResolver.findAction("order_egrn_extract");
        assertTrue(result.isPresent());
        assertEquals("order_egrn_extract", result.get().id());
        assertTrue(databaseResolver.isActionApplicable("order_egrn_extract", "Building"));
        assertFalse(databaseResolver.isActionApplicable("order_egrn_extract", "Contract"));
    }

    @Test
    void shouldReturnEmptyWhenActionNotFound() {
        assertFalse(databaseResolver.findAction("non_existent").isPresent());
    }

    @Test
    void shouldReturnEmptyForUIBinding() {
        assertFalse(databaseResolver.findUIBinding("order_egrn_extract").isPresent());
    }

    @Test
    void shouldCheckActionApplicability() {
        assertTrue(databaseResolver.isActionApplicable("order_egrn_extract", "Building"));
        assertFalse(databaseResolver.isActionApplicable("order_egrn_extract", "Contract"));
    }

    @Test
    void shouldFindTransitions() {
        assertEquals(1, databaseResolver.findTransitions("wf-plan").size());
    }

    @Test
    void shouldFindTransitionByFromAndToSteps() {
        assertTrue(databaseResolver.findTransition("wf-plan", "new", "in_progress").isPresent());
        assertTrue(databaseResolver.findTransition("wf-plan", "completed", "new").isEmpty());
    }

    @Test
    void shouldPassthroughTargetWhenNoUIBinding() {
        assertEquals("input#search", databaseResolver.resolveTargetToSelector("input#search", null));
        assertEquals("https://example.com", databaseResolver.resolveTargetToSelector("https://example.com", null));
    }
}
