package com.zaborstik.platform.api.repository;

import com.zaborstik.platform.api.dto.EntityDTO;
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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@EntityScan("com.zaborstik.platform.api.dto")
@Import(PlanMapper.class)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RepositoryIntegrationTest {

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private PlanMapper planMapper;

    @BeforeEach
    void setUp() {
        entityRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindEntityType() {
        EntityDTO dto = new EntityDTO(EntityDTO.TABLE_ENTITY_TYPES, "Building",
                Map.of("name", "Здание", "metadata", Map.of("description", "Тип сущности")));
        entityRepository.save(dto);

        Optional<EntityDTO> found = entityRepository.findByTableNameAndId(EntityDTO.TABLE_ENTITY_TYPES, "Building");
        assertTrue(found.isPresent());
        assertEquals("Building", found.get().getId());
        assertEquals("Здание", found.get().get("name"));
        assertTrue(((Map<?, ?>) found.get().getData().get("metadata")).containsKey("description"));
    }

    @Test
    void shouldSaveAndFindAction() {
        EntityDTO dto = new EntityDTO(EntityDTO.TABLE_ACTIONS, "order_egrn_extract",
                Map.of("name", "Заказать выписку", "description", "Описание",
                        "applicableEntityTypes", List.of("Building"), "metadata", Map.of("category", "document")));
        entityRepository.save(dto);

        Optional<EntityDTO> found = entityRepository.findByTableNameAndId(EntityDTO.TABLE_ACTIONS, "order_egrn_extract");
        assertTrue(found.isPresent());
        assertEquals("order_egrn_extract", found.get().getId());
        assertTrue(((List<?>) found.get().get("applicableEntityTypes")).contains("Building"));
    }

    @Test
    void shouldSaveAndFindUIBinding() {
        EntityDTO dto = new EntityDTO(EntityDTO.TABLE_UI_BINDINGS, "order_egrn_extract",
                Map.of("selector", "[data-action='order_egrn_extract']", "selectorType", "CSS", "metadata", Map.of("highlight", "true")));
        entityRepository.save(dto);

        Optional<EntityDTO> found = entityRepository.findByTableNameAndId(EntityDTO.TABLE_UI_BINDINGS, "order_egrn_extract");
        assertTrue(found.isPresent());
        assertEquals("order_egrn_extract", found.get().getId());
        assertEquals("CSS", found.get().get("selectorType"));
    }

    @Test
    void shouldSaveAndFindPlanWithSteps() {
        Plan plan = new Plan("plan-id", "Building", "93939", "order_egrn_extract",
                List.of(
                        PlanStep.openPage("/buildings/93939", "Открываю карточку"),
                        PlanStep.click("order_egrn_extract", "Кликаю")
                ),
                Plan.PlanStatus.CREATED);
        EntityDTO dto = planMapper.toEntityDTO(plan);
        entityRepository.save(dto);

        Optional<EntityDTO> found = entityRepository.findByTableNameAndId(EntityDTO.TABLE_PLANS, "plan-id");
        assertTrue(found.isPresent());
        assertEquals("plan-id", found.get().getId());
        assertEquals(2, ((List<?>) found.get().get("steps")).size());
    }

    @Test
    void shouldDeletePlan() {
        EntityDTO dto = new EntityDTO(EntityDTO.TABLE_PLANS, "plan-id",
                Map.of("entityTypeId", "B", "entityId", "1", "actionId", "a", "status", "CREATED", "steps", List.of()));
        entityRepository.save(dto);

        entityRepository.delete(dto);
        assertFalse(entityRepository.findByTableNameAndId(EntityDTO.TABLE_PLANS, "plan-id").isPresent());
    }

    @Test
    void shouldUpdatePlanStatus() {
        EntityDTO dto = new EntityDTO(EntityDTO.TABLE_PLANS, "plan-id",
                Map.of("entityTypeId", "B", "entityId", "1", "actionId", "a", "status", "CREATED", "steps", List.of()));
        entityRepository.save(dto);

        dto.put("status", "EXECUTING");
        entityRepository.save(dto);

        Optional<EntityDTO> found = entityRepository.findByTableNameAndId(EntityDTO.TABLE_PLANS, "plan-id");
        assertTrue(found.isPresent());
        assertEquals("EXECUTING", found.get().get("status"));
    }
}
