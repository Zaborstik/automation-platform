package com.zaborstik.platform.api.config;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.repository.EntityRepository;
import com.zaborstik.platform.api.resolver.DatabaseResolver;
import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.resolver.Resolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@EntityScan("com.zaborstik.platform.api.dto")
@Import({PlatformConfiguration.class, DatabaseResolver.class})
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PlatformConfigurationTest {

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private Resolver resolver;

    @Autowired
    private ExecutionEngine executionEngine;

    @BeforeEach
    void setUp() {
        entityRepository.findById_TableName(EntityDTO.TABLE_UI_BINDINGS).forEach(entityRepository::delete);
        entityRepository.findById_TableName(EntityDTO.TABLE_ACTIONS).forEach(entityRepository::delete);
        entityRepository.findById_TableName(EntityDTO.TABLE_ENTITY_TYPES).forEach(entityRepository::delete);

        entityRepository.save(new EntityDTO(EntityDTO.TABLE_ENTITY_TYPES, "Building",
                Map.of("name", "Здание", "metadata", Map.of("description", "Тип сущности для работы со зданиями"))));
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_ENTITY_TYPES, "Contract",
                Map.of("name", "Договор", "metadata", Map.of("description", "Тип сущности для работы с договорами"))));
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_ACTIONS, "order_egrn_extract",
                Map.of("name", "Заказать выписку из ЕГРН", "description", "Заказывает выписку из ЕГРН для указанного здания",
                        "applicableEntityTypes", List.of("Building"), "metadata", Map.of("category", "document"))));
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_ACTIONS, "close_contract",
                Map.of("name", "Закрыть договор", "description", "Закрывает указанный договор",
                        "applicableEntityTypes", List.of("Contract"), "metadata", Map.of("category", "workflow"))));
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_ACTIONS, "assign_owner",
                Map.of("name", "Назначить владельца", "description", "Назначает владельца для указанного здания",
                        "applicableEntityTypes", List.of("Building"), "metadata", Map.of("category", "management"))));
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_UI_BINDINGS, "order_egrn_extract",
                Map.of("selector", "[data-action='order_egrn_extract']", "selectorType", "CSS", "metadata", Map.of("highlight", "true"))));
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_UI_BINDINGS, "close_contract",
                Map.of("selector", "//button[contains(@class, 'close-contract-btn')]", "selectorType", "XPATH", "metadata", Map.of("highlight", "true"))));
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_UI_BINDINGS, "assign_owner",
                Map.of("selector", "[data-action='assign_owner']", "selectorType", "CSS", "metadata", Map.of("highlight", "true"))));
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
        var building = resolver.findEntityType("Building").orElseThrow();
        assertEquals("Building", building.id());
        assertEquals("Здание", building.name());
    }

    @Test
    void shouldFindRegisteredActions() {
        assertTrue(resolver.findAction("order_egrn_extract").isPresent());
        assertTrue(resolver.findAction("close_contract").isPresent());
        assertTrue(resolver.findAction("assign_owner").isPresent());
        assertFalse(resolver.findAction("non_existent").isPresent());
        var action = resolver.findAction("order_egrn_extract").orElseThrow();
        assertEquals("order_egrn_extract", action.id());
        assertEquals("Заказать выписку из ЕГРН", action.name());
        assertTrue(action.isApplicableTo("Building"));
        assertFalse(action.isApplicableTo("Contract"));
    }

    @Test
    void shouldFindRegisteredUIBindings() {
        assertTrue(resolver.findUIBinding("order_egrn_extract").isPresent());
        assertTrue(resolver.findUIBinding("close_contract").isPresent());
        assertTrue(resolver.findUIBinding("assign_owner").isPresent());
        assertFalse(resolver.findUIBinding("non_existent").isPresent());
        var uiBinding = resolver.findUIBinding("order_egrn_extract").orElseThrow();
        assertEquals("order_egrn_extract", uiBinding.actionId());
        assertEquals("[data-action='order_egrn_extract']", uiBinding.selector());
    }

    @Test
    void shouldCheckActionApplicability() {
        assertTrue(resolver.isActionApplicable("order_egrn_extract", "Building"));
        assertFalse(resolver.isActionApplicable("order_egrn_extract", "Contract"));
        assertTrue(resolver.isActionApplicable("close_contract", "Contract"));
        assertFalse(resolver.isActionApplicable("close_contract", "Building"));
        assertTrue(resolver.isActionApplicable("assign_owner", "Building"));
        assertFalse(resolver.isActionApplicable("assign_owner", "Contract"));
    }

    @Test
    void shouldCreatePlanWithConfiguredResolver() {
        ExecutionRequest request = new ExecutionRequest("Building", "93939", "order_egrn_extract", Map.of());
        Plan plan = executionEngine.createPlan(request);
        assertNotNull(plan);
        assertEquals("Building", plan.entityTypeId());
        assertEquals("93939", plan.entityId());
        assertEquals("order_egrn_extract", plan.actionId());
        assertFalse(plan.steps().isEmpty());
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
