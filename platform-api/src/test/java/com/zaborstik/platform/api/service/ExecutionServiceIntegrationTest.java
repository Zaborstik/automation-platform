package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.repository.EntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@EntityScan("com.zaborstik.platform.api.dto")
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
    private EntityRepository entityRepository;

    @BeforeEach
    void setUp() {
        entityRepository.findById_TableName(EntityDTO.TABLE_PLANS).forEach(entityRepository::delete);
        entityRepository.findById_TableName(EntityDTO.TABLE_ENTITY_TYPES).forEach(entityRepository::delete);
        entityRepository.findById_TableName(EntityDTO.TABLE_ACTIONS).forEach(entityRepository::delete);
        entityRepository.findById_TableName(EntityDTO.TABLE_UI_BINDINGS).forEach(entityRepository::delete);

        entityRepository.save(new EntityDTO(EntityDTO.TABLE_ENTITY_TYPES, "Building",
                Map.of("name", "Здание", "metadata", Map.of("description", "Тип сущности для работы со зданиями"))));
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_ACTIONS, "order_egrn_extract",
                Map.of("name", "Заказать выписку из ЕГРН", "description", "Заказывает выписку из ЕГРН для указанного здания",
                        "applicableEntityTypes", java.util.List.of("Building"), "metadata", Map.of("category", "document"))));
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_UI_BINDINGS, "order_egrn_extract",
                Map.of("selector", "[data-action='order_egrn_extract']", "selectorType", "CSS", "metadata", Map.of("highlight", "true"))));
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

        Optional<EntityDTO> savedPlan = entityRepository.findByTableNameAndId(EntityDTO.TABLE_PLANS, plan.getId());
        assertTrue(savedPlan.isPresent());
        assertEquals("Building", savedPlan.get().get("entityTypeId"));
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
