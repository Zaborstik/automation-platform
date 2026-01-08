package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.ExecutionRequestDTO;
import com.zaborstik.platform.api.dto.PlanDTO;
import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.entity.UIBindingEntity;
import com.zaborstik.platform.api.repository.ActionRepository;
import com.zaborstik.platform.api.repository.EntityTypeRepository;
import com.zaborstik.platform.api.repository.PlanRepository;
import com.zaborstik.platform.api.repository.UIBindingRepository;
import com.zaborstik.platform.core.ExecutionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({ExecutionService.class, ExecutionEngine.class, com.zaborstik.platform.api.resolver.DatabaseResolver.class, 
        com.zaborstik.platform.api.mapper.PlanMapper.class})
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ExecutionServiceIntegrationTest {

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private UIBindingRepository uiBindingRepository;

    @Autowired
    private PlanRepository planRepository;

    @BeforeEach
    void setUp() {
        planRepository.deleteAll();
        uiBindingRepository.deleteAll();
        actionRepository.deleteAll();
        entityTypeRepository.deleteAll();

        // Создаем тестовые данные
        EntityTypeEntity entityType = new EntityTypeEntity(
            "Building",
            "Здание",
            Map.of("description", "Тип сущности для работы со зданиями")
        );
        entityTypeRepository.save(entityType);

        ActionEntity action = new ActionEntity(
            "order_egrn_extract",
            "Заказать выписку из ЕГРН",
            "Заказывает выписку из ЕГРН для указанного здания",
            Set.of("Building"),
            Map.of("category", "document")
        );
        actionRepository.save(action);

        UIBindingEntity uiBinding = new UIBindingEntity(
            "order_egrn_extract",
            "[data-action='order_egrn_extract']",
            UIBindingEntity.SelectorType.CSS,
            Map.of("highlight", "true")
        );
        uiBindingRepository.save(uiBinding);
    }

    @Test
    void shouldCreateAndSavePlan() {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        // When
        PlanDTO plan = executionService.createPlan(request);

        // Then
        assertNotNull(plan);
        assertEquals("Building", plan.getEntityTypeId());
        assertEquals("93939", plan.getEntityId());
        assertEquals("order_egrn_extract", plan.getActionId());
        assertNotNull(plan.getId());
        assertFalse(plan.getSteps().isEmpty());

        // Проверяем, что план сохранен в БД
        Optional<com.zaborstik.platform.api.entity.PlanEntity> savedPlan = planRepository.findById(plan.getId());
        assertTrue(savedPlan.isPresent());
        assertEquals("Building", savedPlan.get().getEntityTypeId());
        assertEquals(plan.getSteps().size(), savedPlan.get().getSteps().size());
    }

    @Test
    void shouldRetrievePlanFromDatabase() {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );
        PlanDTO createdPlan = executionService.createPlan(request);

        // When
        Optional<PlanDTO> retrievedPlan = executionService.getPlan(createdPlan.getId());

        // Then
        assertTrue(retrievedPlan.isPresent());
        PlanDTO plan = retrievedPlan.get();
        assertEquals(createdPlan.getId(), plan.getId());
        assertEquals("Building", plan.getEntityTypeId());
        assertEquals("93939", plan.getEntityId());
        assertEquals("order_egrn_extract", plan.getActionId());
        assertEquals(createdPlan.getSteps().size(), plan.getSteps().size());
    }

    @Test
    void shouldReturnEmptyForNonExistentPlan() {
        // When
        Optional<PlanDTO> result = executionService.getPlan("non-existent-id");

        // Then
        assertFalse(result.isPresent());
    }
}
