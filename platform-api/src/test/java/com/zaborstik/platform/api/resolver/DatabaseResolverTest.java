package com.zaborstik.platform.api.resolver;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.repository.EntityRepository;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
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
@Import(DatabaseResolver.class)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DatabaseResolverTest {

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private DatabaseResolver databaseResolver;

    @BeforeEach
    void setUp() {
        entityRepository.findById_TableName(EntityDTO.TABLE_UI_BINDINGS).forEach(entityRepository::delete);
        entityRepository.findById_TableName(EntityDTO.TABLE_ACTIONS).forEach(entityRepository::delete);
        entityRepository.findById_TableName(EntityDTO.TABLE_ENTITY_TYPES).forEach(entityRepository::delete);

        entityRepository.save(new EntityDTO(EntityDTO.TABLE_ENTITY_TYPES, "Building",
                Map.of("name", "Здание", "metadata", Map.of("description", "Тип сущности для работы со зданиями"))));
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_ACTIONS, "order_egrn_extract",
                Map.of("name", "Заказать выписку из ЕГРН", "description", "Описание действия",
                        "applicableEntityTypes", List.of("Building"), "metadata", Map.of("category", "document"))));
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_UI_BINDINGS, "order_egrn_extract",
                Map.of("selector", "[data-action='order_egrn_extract']", "selectorType", "CSS", "metadata", Map.of("highlight", "true"))));
    }

    @Test
    void shouldFindEntityType() {
        Optional<EntityType> result = databaseResolver.findEntityType("Building");
        assertTrue(result.isPresent());
        EntityType entityType = result.get();
        assertEquals("Building", entityType.id());
        assertEquals("Здание", entityType.name());
        assertTrue(entityType.metadata().containsKey("description"));
    }

    @Test
    void shouldReturnEmptyWhenEntityTypeNotFound() {
        Optional<EntityType> result = databaseResolver.findEntityType("NonExistent");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldFindAction() {
        Optional<Action> result = databaseResolver.findAction("order_egrn_extract");
        assertTrue(result.isPresent());
        Action action = result.get();
        assertEquals("order_egrn_extract", action.id());
        assertEquals("Заказать выписку из ЕГРН", action.name());
        assertTrue(action.isApplicableTo("Building"));
        assertFalse(action.isApplicableTo("Contract"));
    }

    @Test
    void shouldReturnEmptyWhenActionNotFound() {
        Optional<Action> result = databaseResolver.findAction("non_existent");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldFindUIBinding() {
        Optional<UIBinding> result = databaseResolver.findUIBinding("order_egrn_extract");
        assertTrue(result.isPresent());
        UIBinding uiBinding = result.get();
        assertEquals("order_egrn_extract", uiBinding.actionId());
        assertEquals("[data-action='order_egrn_extract']", uiBinding.selector());
        assertEquals(UIBinding.SelectorType.CSS, uiBinding.selectorType());
    }

    @Test
    void shouldReturnEmptyWhenUIBindingNotFound() {
        Optional<UIBinding> result = databaseResolver.findUIBinding("non_existent");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldCheckActionApplicability() {
        assertTrue(databaseResolver.isActionApplicable("order_egrn_extract", "Building"));
        assertFalse(databaseResolver.isActionApplicable("order_egrn_extract", "Contract"));
        assertFalse(databaseResolver.isActionApplicable("non_existent", "Building"));
    }

    @Test
    void shouldConvertSelectorTypes() {
        entityRepository.save(new EntityDTO(EntityDTO.TABLE_UI_BINDINGS, "test_action",
                Map.of("selector", "selector", "selectorType", "XPATH", "metadata", Map.of())));

        Optional<UIBinding> result = databaseResolver.findUIBinding("test_action");
        assertTrue(result.isPresent());
        assertEquals(UIBinding.SelectorType.XPATH, result.get().selectorType());
    }
}
