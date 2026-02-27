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
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@EntityScan("com.zaborstik.platform.api.entity")
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
        uiBindingRepository.deleteAll();
        actionRepository.deleteAll();
        entityTypeRepository.deleteAll();

        EntityTypeEntity et = new EntityTypeEntity();
        et.setShortname("Building");
        et.setDisplayname("Здание");
        et.setMetadata(Map.of("description", "Тип сущности для работы со зданиями"));
        entityTypeRepository.save(et);

        et = new EntityTypeEntity();
        et.setShortname("Contract");
        et.setDisplayname("Договор");
        et.setMetadata(Map.of("description", "Тип сущности для работы с договорами"));
        entityTypeRepository.save(et);

        ActionEntity a = new ActionEntity();
        a.setShortname("order_egrn_extract");
        a.setDisplayname("Заказать выписку из ЕГРН");
        a.setDescription("Заказывает выписку из ЕГРН для указанного здания");
        a.setApplicableEntityTypes(Set.of("Building"));
        a.setMetadata(Map.of("category", "document"));
        actionRepository.save(a);

        a = new ActionEntity();
        a.setShortname("close_contract");
        a.setDisplayname("Закрыть договор");
        a.setDescription("Закрывает указанный договор");
        a.setApplicableEntityTypes(Set.of("Contract"));
        a.setMetadata(Map.of("category", "workflow"));
        actionRepository.save(a);

        a = new ActionEntity();
        a.setShortname("assign_owner");
        a.setDisplayname("Назначить владельца");
        a.setDescription("Назначает владельца для указанного здания");
        a.setApplicableEntityTypes(Set.of("Building"));
        a.setMetadata(Map.of("category", "management"));
        actionRepository.save(a);

        UIBindingEntity ui = new UIBindingEntity();
        ui.setAction("order_egrn_extract");
        ui.setSelector("[data-action='order_egrn_extract']");
        ui.setSelectorType(UIBindingEntity.SelectorType.CSS);
        ui.setMetadata(Map.of("highlight", "true"));
        uiBindingRepository.save(ui);

        ui = new UIBindingEntity();
        ui.setAction("close_contract");
        ui.setSelector("//button[contains(@class, 'close-contract-btn')]");
        ui.setSelectorType(UIBindingEntity.SelectorType.XPATH);
        ui.setMetadata(Map.of("highlight", "true"));
        uiBindingRepository.save(ui);

        ui = new UIBindingEntity();
        ui.setAction("assign_owner");
        ui.setSelector("[data-action='assign_owner']");
        ui.setSelectorType(UIBindingEntity.SelectorType.CSS);
        ui.setMetadata(Map.of("highlight", "true"));
        uiBindingRepository.save(ui);
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
        assertEquals("Здание", resolver.findEntityType("Building").orElseThrow().name());
    }

    @Test
    void shouldFindRegisteredActions() {
        assertTrue(resolver.findAction("order_egrn_extract").isPresent());
        assertTrue(resolver.findAction("close_contract").isPresent());
        assertTrue(resolver.findAction("assign_owner").isPresent());
        assertTrue(resolver.findAction("order_egrn_extract").orElseThrow().isApplicableTo("Building"));
        assertFalse(resolver.findAction("order_egrn_extract").orElseThrow().isApplicableTo("Contract"));
    }

    @Test
    void shouldFindRegisteredUIBindings() {
        assertTrue(resolver.findUIBinding("order_egrn_extract").isPresent());
        assertTrue(resolver.findUIBinding("close_contract").isPresent());
        assertTrue(resolver.findUIBinding("assign_owner").isPresent());
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
