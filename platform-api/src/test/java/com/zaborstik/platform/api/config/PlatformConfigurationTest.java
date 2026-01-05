package com.zaborstik.platform.api.config;

import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.resolver.Resolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlatformConfigurationTest {

    private PlatformConfiguration configuration;
    private Resolver resolver;
    private ExecutionEngine executionEngine;

    @BeforeEach
    void setUp() {
        configuration = new PlatformConfiguration();
        resolver = configuration.resolver();
        executionEngine = configuration.executionEngine(resolver);
    }

    @Test
    void shouldCreateResolverBean() {
        assertNotNull(resolver);
    }

    @Test
    void shouldCreateExecutionEngineBean() {
        assertNotNull(executionEngine);
    }

    @Test
    void shouldFindRegisteredEntityTypes() {
        // Then
        assertTrue(resolver.findEntityType("Building").isPresent());
        assertTrue(resolver.findEntityType("Contract").isPresent());
        assertFalse(resolver.findEntityType("NonExistent").isPresent());

        EntityType building = resolver.findEntityType("Building").orElseThrow();
        assertEquals("Building", building.getId());
        assertEquals("Здание", building.getName());
    }

    @Test
    void shouldFindRegisteredActions() {
        // Then
        assertTrue(resolver.findAction("order_egrn_extract").isPresent());
        assertTrue(resolver.findAction("close_contract").isPresent());
        assertTrue(resolver.findAction("assign_owner").isPresent());
        assertFalse(resolver.findAction("non_existent").isPresent());

        Action action = resolver.findAction("order_egrn_extract").orElseThrow();
        assertEquals("order_egrn_extract", action.getId());
        assertEquals("Заказать выписку из ЕГРН", action.getName());
        assertTrue(action.isApplicableTo("Building"));
        assertFalse(action.isApplicableTo("Contract"));
    }

    @Test
    void shouldFindRegisteredUIBindings() {
        // Then
        assertTrue(resolver.findUIBinding("order_egrn_extract").isPresent());
        assertTrue(resolver.findUIBinding("close_contract").isPresent());
        assertTrue(resolver.findUIBinding("assign_owner").isPresent());
        assertFalse(resolver.findUIBinding("non_existent").isPresent());

        UIBinding uiBinding = resolver.findUIBinding("order_egrn_extract").orElseThrow();
        assertEquals("order_egrn_extract", uiBinding.getActionId());
        assertEquals("[data-action='order_egrn_extract']", uiBinding.getSelector());
        assertEquals(UIBinding.SelectorType.CSS, uiBinding.getSelectorType());
    }

    @Test
    void shouldCheckActionApplicability() {
        // Then
        assertTrue(resolver.isActionApplicable("order_egrn_extract", "Building"));
        assertFalse(resolver.isActionApplicable("order_egrn_extract", "Contract"));

        assertTrue(resolver.isActionApplicable("close_contract", "Contract"));
        assertFalse(resolver.isActionApplicable("close_contract", "Building"));

        assertTrue(resolver.isActionApplicable("assign_owner", "Building"));
        assertFalse(resolver.isActionApplicable("assign_owner", "Contract"));
    }

    @Test
    void shouldCreatePlanWithConfiguredResolver() {
        // Given
        ExecutionRequest request = new ExecutionRequest(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        // When
        Plan plan = executionEngine.createPlan(request);

        // Then
        assertNotNull(plan);
        assertEquals("Building", plan.getEntityTypeId());
        assertEquals("93939", plan.getEntityId());
        assertEquals("order_egrn_extract", plan.getActionId());
        assertFalse(plan.getSteps().isEmpty());
    }

    @Test
    void shouldThrowExceptionForNonExistentEntityType() {
        // Given
        ExecutionRequest request = new ExecutionRequest(
            "NonExistent",
            "123",
            "order_egrn_extract",
            Map.of()
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            executionEngine.createPlan(request);
        });
    }

    @Test
    void shouldThrowExceptionForNonApplicableAction() {
        // Given
        ExecutionRequest request = new ExecutionRequest(
            "Contract",
            "123",
            "order_egrn_extract", // Это действие применимо только к Building
            Map.of()
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            executionEngine.createPlan(request);
        });
    }
}

