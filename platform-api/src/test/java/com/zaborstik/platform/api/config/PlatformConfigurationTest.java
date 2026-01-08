package com.zaborstik.platform.api.config;

import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.entity.UIBindingEntity;
import com.zaborstik.platform.api.repository.ActionRepository;
import com.zaborstik.platform.api.repository.EntityTypeRepository;
import com.zaborstik.platform.api.repository.UIBindingRepository;
import com.zaborstik.platform.api.resolver.DatabaseResolver;
import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.resolver.Resolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({PlatformConfiguration.class, DatabaseResolver.class})
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PlatformConfigurationTest {

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private UIBindingRepository uiBindingRepository;

    @Autowired
    private Resolver resolver;

    @Autowired
    private ExecutionEngine executionEngine;

    @BeforeEach
    void setUp() {
        // Очищаем БД
        uiBindingRepository.deleteAll();
        actionRepository.deleteAll();
        entityTypeRepository.deleteAll();

        // Создаем тестовые данные (имитируем миграцию V2)
        EntityTypeEntity buildingType = new EntityTypeEntity(
            "Building",
            "Здание",
            Map.of("description", "Тип сущности для работы со зданиями")
        );
        entityTypeRepository.save(buildingType);

        EntityTypeEntity contractType = new EntityTypeEntity(
            "Contract",
            "Договор",
            Map.of("description", "Тип сущности для работы с договорами")
        );
        entityTypeRepository.save(contractType);

        ActionEntity action1 = new ActionEntity(
            "order_egrn_extract",
            "Заказать выписку из ЕГРН",
            "Заказывает выписку из ЕГРН для указанного здания",
            Set.of("Building"),
            Map.of("category", "document")
        );
        actionRepository.save(action1);

        ActionEntity action2 = new ActionEntity(
            "close_contract",
            "Закрыть договор",
            "Закрывает указанный договор",
            Set.of("Contract"),
            Map.of("category", "workflow")
        );
        actionRepository.save(action2);

        ActionEntity action3 = new ActionEntity(
            "assign_owner",
            "Назначить владельца",
            "Назначает владельца для указанного здания",
            Set.of("Building"),
            Map.of("category", "management")
        );
        actionRepository.save(action3);

        UIBindingEntity uiBinding1 = new UIBindingEntity(
            "order_egrn_extract",
            "[data-action='order_egrn_extract']",
            UIBindingEntity.SelectorType.CSS,
            Map.of("highlight", "true")
        );
        uiBindingRepository.save(uiBinding1);

        UIBindingEntity uiBinding2 = new UIBindingEntity(
            "close_contract",
            "//button[contains(@class, 'close-contract-btn')]",
            UIBindingEntity.SelectorType.XPATH,
            Map.of("highlight", "true")
        );
        uiBindingRepository.save(uiBinding2);

        UIBindingEntity uiBinding3 = new UIBindingEntity(
            "assign_owner",
            "[data-action='assign_owner']",
            UIBindingEntity.SelectorType.CSS,
            Map.of("highlight", "true")
        );
        uiBindingRepository.save(uiBinding3);
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
        // Then
        assertTrue(resolver.findEntityType("Building").isPresent());
        assertTrue(resolver.findEntityType("Contract").isPresent());
        assertFalse(resolver.findEntityType("NonExistent").isPresent());

        var building = resolver.findEntityType("Building").orElseThrow();
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

        var action = resolver.findAction("order_egrn_extract").orElseThrow();
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

        var uiBinding = resolver.findUIBinding("order_egrn_extract").orElseThrow();
        assertEquals("order_egrn_extract", uiBinding.getActionId());
        assertEquals("[data-action='order_egrn_extract']", uiBinding.getSelector());
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
