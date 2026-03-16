package com.zaborstik.platform.api.config;

import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.ActionTypeEntity;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.repository.ActionRepository;
import com.zaborstik.platform.api.repository.ActionTypeRepository;
import com.zaborstik.platform.api.repository.EntityTypeRepository;
import com.zaborstik.platform.api.repository.WorkflowRepository;
import com.zaborstik.platform.api.repository.WorkflowStepRepository;
import com.zaborstik.platform.api.repository.WorkflowTransitionRepository;
import com.zaborstik.platform.api.resolver.DatabaseResolver;
import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.resolver.Resolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan("com.zaborstik.platform.api.entity")
@Import({PlatformConfiguration.class, DatabaseResolver.class})
@ActiveProfiles({"dev", "datajpa"})
class PlatformConfigurationTest {

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
    private Resolver resolver;

    @Autowired
    private ExecutionEngine executionEngine;

    @BeforeEach
    void setUp() {
        actionRepository.deleteAll();
        entityTypeRepository.deleteAll();
        actionTypeRepository.deleteAll();
        workflowTransitionRepository.deleteAll();
        workflowRepository.deleteAll();
        workflowStepRepository.deleteAll();

        EntityTypeEntity building = new EntityTypeEntity();
        building.setId("Building");
        building.setDisplayname("Здание");
        entityTypeRepository.save(building);

        EntityTypeEntity contract = new EntityTypeEntity();
        contract.setId("Contract");
        contract.setDisplayname("Договор");
        entityTypeRepository.save(contract);

        ActionTypeEntity actionType = new ActionTypeEntity();
        actionType.setId("act-type-1");
        actionType.setInternalname("interaction");
        actionType.setDisplayname("Взаимодействие");
        actionTypeRepository.save(actionType);

        ActionEntity orderEgrn = new ActionEntity();
        orderEgrn.setId("order_egrn_extract");
        orderEgrn.setDisplayname("Заказать выписку из ЕГРН");
        orderEgrn.setInternalname("order_egrn_extract");
        orderEgrn.setDescription("Заказывает выписку из ЕГРН для указанного здания");
        orderEgrn.setActionType(actionType);
        orderEgrn.setApplicableEntityTypes(Set.of(building));
        actionRepository.save(orderEgrn);

        ActionEntity closeContract = new ActionEntity();
        closeContract.setId("close_contract");
        closeContract.setDisplayname("Закрыть договор");
        closeContract.setInternalname("close_contract");
        closeContract.setDescription("Закрывает указанный договор");
        closeContract.setActionType(actionType);
        closeContract.setApplicableEntityTypes(Set.of(contract));
        actionRepository.save(closeContract);

        ActionEntity assignOwner = new ActionEntity();
        assignOwner.setId("assign_owner");
        assignOwner.setDisplayname("Назначить владельца");
        assignOwner.setInternalname("assign_owner");
        assignOwner.setDescription("Назначает владельца для указанного здания");
        assignOwner.setActionType(actionType);
        assignOwner.setApplicableEntityTypes(Set.of(building));
        actionRepository.save(assignOwner);
    }

    @Test
    void shouldCreateResolverBean() {
        assertNotNull(resolver);
        assertTrue(resolver instanceof DatabaseResolver);
    }

    @Test
    void shouldCreateExecutionEngineBean() {
        assertNotNull(executionEngine);
    }

    @Test
    void shouldFindRegisteredEntityTypes() {
        assertTrue(resolver.findEntityType("Building").isPresent());
        assertTrue(resolver.findEntityType("Contract").isPresent());
        assertFalse(resolver.findEntityType("NonExistent").isPresent());
        assertEquals("Building", resolver.findEntityType("Building").orElseThrow().id());
        assertEquals("Здание", resolver.findEntityType("Building").orElseThrow().displayName());
    }

    @Test
    void shouldFindRegisteredActions() {
        assertTrue(resolver.findAction("order_egrn_extract").isPresent());
        assertTrue(resolver.findAction("close_contract").isPresent());
        assertTrue(resolver.findAction("assign_owner").isPresent());
        assertTrue(resolver.isActionApplicable("order_egrn_extract", "Building"));
        assertFalse(resolver.isActionApplicable("order_egrn_extract", "Contract"));
    }

    @Test
    void shouldReturnEmptyForUIBinding() {
        assertFalse(resolver.findUIBinding("order_egrn_extract").isPresent());
    }

    @Test
    void shouldCheckActionApplicability() {
        assertTrue(resolver.isActionApplicable("order_egrn_extract", "Building"));
        assertFalse(resolver.isActionApplicable("order_egrn_extract", "Contract"));
        assertTrue(resolver.isActionApplicable("close_contract", "Contract"));
        assertFalse(resolver.isActionApplicable("close_contract", "Building"));
    }

    @Test
    void shouldCreatePlanWithConfiguredResolver() {
        ExecutionRequest request = new ExecutionRequest("Building", "93939", "order_egrn_extract", Map.of());
        Plan plan = executionEngine.createPlan(request);
        assertNotNull(plan);
        assertNotNull(plan.id());
        assertEquals("wf-plan", plan.workflowId());
        assertEquals("new", plan.workflowStepInternalName());
        assertFalse(plan.steps().isEmpty());
        assertEquals("Building", plan.steps().get(0).entityTypeId());
        assertEquals("93939", plan.steps().get(0).entityId());
        assertEquals("order_egrn_extract", plan.steps().get(0).actions().get(0).actionId());
    }

    @Test
    void shouldThrowExceptionForNonExistentEntityType() {
        ExecutionRequest request = new ExecutionRequest("NonExistent", "123", "order_egrn_extract", Map.of());
        assertThrows(IllegalArgumentException.class, () -> executionEngine.createPlan(request));
    }

    @Test
    void shouldThrowExceptionForNonApplicableAction() {
        ExecutionRequest request = new ExecutionRequest("Contract", "123", "order_egrn_extract", Map.of());
        assertThrows(IllegalArgumentException.class, () -> executionEngine.createPlan(request));
    }
}
