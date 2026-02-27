package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.entity.PlanEntity;
import com.zaborstik.platform.api.entity.PlanStepEntity;
import com.zaborstik.platform.api.entity.UIBindingEntity;
import com.zaborstik.platform.api.mapper.PlanMapper;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@EntityScan("com.zaborstik.platform.api.entity")
@Import(PlanMapper.class)
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

    @Autowired
    private PlanMapper planMapper;

    @BeforeEach
    void setUp() {
        planRepository.deleteAll();
        uiBindingRepository.deleteAll();
        actionRepository.deleteAll();
        entityTypeRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindEntityType() {
        EntityTypeEntity et = new EntityTypeEntity();
        et.setShortname("Building");
        et.setDisplayname("Здание");
        et.setMetadata(Map.of("description", "Тип сущности"));
        entityTypeRepository.save(et);

        Optional<EntityTypeEntity> found = entityTypeRepository.findById("Building");
        assertTrue(found.isPresent());
        assertEquals("Building", found.get().getId());
        assertEquals("Здание", found.get().getName());
        assertTrue(found.get().getMetadata().containsKey("description"));
    }

    @Test
    void shouldSaveAndFindAction() {
        ActionEntity a = new ActionEntity();
        a.setShortname("order_egrn_extract");
        a.setDisplayname("Заказать выписку");
        a.setDescription("Описание");
        a.setApplicableEntityTypes(Set.of("Building"));
        a.setMetadata(Map.of("category", "document"));
        actionRepository.save(a);

        Optional<ActionEntity> found = actionRepository.findById("order_egrn_extract");
        assertTrue(found.isPresent());
        assertTrue(found.get().getApplicableEntityTypes().contains("Building"));
    }

    @Test
    void shouldSaveAndFindUIBinding() {
        UIBindingEntity ui = new UIBindingEntity();
        ui.setAction("order_egrn_extract");
        ui.setSelector("[data-action='order_egrn_extract']");
        ui.setSelectorType(UIBindingEntity.SelectorType.CSS);
        ui.setMetadata(Map.of("highlight", "true"));
        uiBindingRepository.save(ui);

        Optional<UIBindingEntity> found = uiBindingRepository.findById("order_egrn_extract");
        assertTrue(found.isPresent());
        assertEquals(UIBindingEntity.SelectorType.CSS, found.get().getSelectorType());
    }

    @Test
    void shouldSaveAndFindPlanWithSteps() {
        ActionEntity action = new ActionEntity();
        action.setShortname("order_egrn_extract");
        action.setDisplayname("Action");
        actionRepository.save(action);

        Plan plan = new Plan("plan-id", "Building", "93939", "order_egrn_extract",
                List.of(
                        PlanStep.openPage("/buildings/93939", "Открываю карточку"),
                        PlanStep.click("order_egrn_extract", "Кликаю")
                ),
                Plan.PlanStatus.CREATED);
        PlanEntity entity = planMapper.toEntity(plan);
        planRepository.save(entity);

        Optional<PlanEntity> found = planRepository.findById("plan-id");
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getSteps().size());
    }

    @Test
    void shouldDeletePlan() {
        ActionEntity action = new ActionEntity();
        action.setShortname("a");
        action.setDisplayname("A");
        actionRepository.save(action);

        PlanEntity plan = new PlanEntity();
        plan.setShortname("plan-id");
        plan.setEntityTypeId("Building");
        plan.setEntityId("1");
        plan.setAction(action);
        plan.setStatus(PlanEntity.PlanStatus.CREATED);
        planRepository.save(plan);

        planRepository.deleteById("plan-id");
        assertFalse(planRepository.findById("plan-id").isPresent());
    }

    @Test
    void shouldUpdatePlanStatus() {
        ActionEntity action = new ActionEntity();
        action.setShortname("a");
        action.setDisplayname("A");
        actionRepository.save(action);

        PlanEntity plan = new PlanEntity();
        plan.setShortname("plan-id");
        plan.setEntityTypeId("B");
        plan.setEntityId("1");
        plan.setAction(action);
        plan.setStatus(PlanEntity.PlanStatus.CREATED);
        planRepository.save(plan);

        plan.setStatus(PlanEntity.PlanStatus.EXECUTING);
        planRepository.save(plan);

        Optional<PlanEntity> found = planRepository.findById("plan-id");
        assertTrue(found.isPresent());
        assertEquals(PlanEntity.PlanStatus.EXECUTING, found.get().getStatus());
    }
}
