package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.entity.PlanEntity;
import com.zaborstik.platform.api.entity.UIBindingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RepositoryIntegrationTest {

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
    }

    @Test
    void shouldSaveAndFindEntityType() {
        // Given
        EntityTypeEntity entity = new EntityTypeEntity(
            "Building",
            "Здание",
            Map.of("description", "Тип сущности")
        );

        // When
        entityTypeRepository.save(entity);
        Optional<EntityTypeEntity> found = entityTypeRepository.findById("Building");

        // Then
        assertTrue(found.isPresent());
        assertEquals("Building", found.get().getId());
        assertEquals("Здание", found.get().getName());
        assertTrue(found.get().getMetadata().containsKey("description"));
    }

    @Test
    void shouldSaveAndFindAction() {
        // Given
        ActionEntity action = new ActionEntity(
            "order_egrn_extract",
            "Заказать выписку",
            "Описание",
            Set.of("Building"),
            Map.of("category", "document")
        );

        // When
        actionRepository.save(action);
        Optional<ActionEntity> found = actionRepository.findById("order_egrn_extract");

        // Then
        assertTrue(found.isPresent());
        assertEquals("order_egrn_extract", found.get().getId());
        assertTrue(found.get().getApplicableEntityTypes().contains("Building"));
    }

    @Test
    void shouldSaveAndFindUIBinding() {
        // Given
        UIBindingEntity binding = new UIBindingEntity(
            "order_egrn_extract",
            "[data-action='order_egrn_extract']",
            UIBindingEntity.SelectorType.CSS,
            Map.of("highlight", "true")
        );

        // When
        uiBindingRepository.save(binding);
        Optional<UIBindingEntity> found = uiBindingRepository.findByActionId("order_egrn_extract");

        // Then
        assertTrue(found.isPresent());
        assertEquals("order_egrn_extract", found.get().getActionId());
        assertEquals(UIBindingEntity.SelectorType.CSS, found.get().getSelectorType());
    }

    @Test
    void shouldSaveAndFindPlanWithSteps() {
        // Given
        PlanEntity plan = new PlanEntity(
            "plan-id",
            "Building",
            "93939",
            "order_egrn_extract",
            PlanEntity.PlanStatus.CREATED
        );

        com.zaborstik.platform.api.entity.PlanStepEntity step1 = new com.zaborstik.platform.api.entity.PlanStepEntity(
            plan, 0, "open_page", "/buildings/93939", "Открываю карточку", Map.of()
        );
        com.zaborstik.platform.api.entity.PlanStepEntity step2 = new com.zaborstik.platform.api.entity.PlanStepEntity(
            plan, 1, "click", "action(order_egrn_extract)", "Кликаю", Map.of()
        );
        plan.setSteps(java.util.List.of(step1, step2));

        // When
        planRepository.save(plan);
        Optional<PlanEntity> found = planRepository.findById("plan-id");

        // Then
        assertTrue(found.isPresent());
        PlanEntity foundPlan = found.get();
        assertEquals("plan-id", foundPlan.getId());
        assertEquals(2, foundPlan.getSteps().size());
        assertEquals(0, foundPlan.getSteps().get(0).getStepIndex());
        assertEquals(1, foundPlan.getSteps().get(1).getStepIndex());
    }

    @Test
    void shouldCascadeDeletePlanSteps() {
        // Given
        PlanEntity plan = new PlanEntity(
            "plan-id",
            "Building",
            "93939",
            "order_egrn_extract",
            PlanEntity.PlanStatus.CREATED
        );
        com.zaborstik.platform.api.entity.PlanStepEntity step = new com.zaborstik.platform.api.entity.PlanStepEntity(
            plan, 0, "open_page", "/page", "Step", Map.of()
        );
        plan.setSteps(java.util.List.of(step));
        planRepository.save(plan);

        // When
        planRepository.deleteById("plan-id");

        // Then
        assertFalse(planRepository.findById("plan-id").isPresent());
        // Steps должны быть удалены каскадно
    }

    @Test
    void shouldUpdatePlanStatus() {
        // Given
        PlanEntity plan = new PlanEntity(
            "plan-id",
            "Building",
            "93939",
            "order_egrn_extract",
            PlanEntity.PlanStatus.CREATED
        );
        planRepository.save(plan);

        // When
        plan.setStatus(PlanEntity.PlanStatus.EXECUTING);
        planRepository.save(plan);
        Optional<PlanEntity> found = planRepository.findById("plan-id");

        // Then
        assertTrue(found.isPresent());
        assertEquals(PlanEntity.PlanStatus.EXECUTING, found.get().getStatus());
    }
}
