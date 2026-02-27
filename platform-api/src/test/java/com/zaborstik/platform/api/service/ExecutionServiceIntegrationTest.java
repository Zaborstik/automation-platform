package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.entity.UIBindingEntity;
import com.zaborstik.platform.api.repository.ActionRepository;
import com.zaborstik.platform.api.repository.EntityTypeRepository;
import com.zaborstik.platform.api.repository.PlanRepository;
import com.zaborstik.platform.api.repository.UIBindingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@EntityScan("com.zaborstik.platform.api.entity")
@Import({ExecutionService.class, com.zaborstik.platform.api.config.PlatformConfiguration.class,
        com.zaborstik.platform.api.resolver.DatabaseResolver.class,
        com.zaborstik.platform.api.mapper.PlanMapper.class})
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ExecutionServiceIntegrationTest {

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private UIBindingRepository uiBindingRepository;

    @BeforeEach
    void setUp() {
        planRepository.deleteAll();
        uiBindingRepository.deleteAll();
        actionRepository.deleteAll();
        entityTypeRepository.deleteAll();

        EntityTypeEntity et = new EntityTypeEntity();
        et.setShortname("Building");
        et.setDisplayname("Здание");
        et.setMetadata(Map.of("description", "Тип сущности для работы со зданиями"));
        entityTypeRepository.save(et);

        ActionEntity action = new ActionEntity();
        action.setShortname("order_egrn_extract");
        action.setDisplayname("Заказать выписку из ЕГРН");
        action.setDescription("Заказывает выписку из ЕГРН для указанного здания");
        action.setApplicableEntityTypes(Set.of("Building"));
        action.setMetadata(Map.of("category", "document"));
        actionRepository.save(action);

        UIBindingEntity ui = new UIBindingEntity();
        ui.setAction("order_egrn_extract");
        ui.setSelector("[data-action='order_egrn_extract']");
        ui.setSelectorType(UIBindingEntity.SelectorType.CSS);
        ui.setMetadata(Map.of("highlight", "true"));
        uiBindingRepository.save(ui);
    }

    @Test
    void shouldCreateAndSavePlan() {
        EntityDTO request = new EntityDTO(EntityDTO.TABLE_EXECUTION_REQUEST, null,
                Map.of("entity", "Building", "entityId", "93939", "action", "order_egrn_extract", "parameters", Map.of()));

        EntityDTO plan = executionService.createPlan(request);

        assertNotNull(plan);
        assertEquals(EntityDTO.TABLE_PLANS, plan.getTableName());
        assertEquals("Building", plan.get("entityTypeId"));
        assertEquals("93939", plan.get("entityId"));
        assertEquals("order_egrn_extract", plan.get("actionId"));
        assertNotNull(plan.getId());
        assertNotNull(plan.get("steps"));
        assertFalse(((java.util.Collection<?>) plan.get("steps")).isEmpty());

        assertTrue(planRepository.findById(plan.getId()).isPresent());
        assertEquals("Building", planRepository.findById(plan.getId()).orElseThrow().getEntityTypeId());
    }

    @Test
    void shouldRetrievePlanFromDatabase() {
        EntityDTO request = new EntityDTO(EntityDTO.TABLE_EXECUTION_REQUEST, null,
                Map.of("entity", "Building", "entityId", "93939", "action", "order_egrn_extract", "parameters", Map.of()));
        EntityDTO createdPlan = executionService.createPlan(request);

        Optional<EntityDTO> retrievedPlan = executionService.getPlan(createdPlan.getId());

        assertTrue(retrievedPlan.isPresent());
        EntityDTO plan = retrievedPlan.get();
        assertEquals(createdPlan.getId(), plan.getId());
        assertEquals("Building", plan.get("entityTypeId"));
        assertEquals("93939", plan.get("entityId"));
        assertEquals("order_egrn_extract", plan.get("actionId"));
    }

    @Test
    void shouldReturnEmptyForNonExistentPlan() {
        Optional<EntityDTO> result = executionService.getPlan("non-existent-id");
        assertFalse(result.isPresent());
    }
}
