package com.zaborstik.platform.api.resolver;

import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.entity.UIBindingEntity;
import com.zaborstik.platform.api.repository.ActionRepository;
import com.zaborstik.platform.api.repository.EntityTypeRepository;
import com.zaborstik.platform.api.repository.UIBindingRepository;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
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
@Import(DatabaseResolver.class)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DatabaseResolverTest {

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private UIBindingRepository uiBindingRepository;

    @Autowired
    private DatabaseResolver databaseResolver;

    @BeforeEach
    void setUp() {
        // Очищаем БД перед каждым тестом
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
            "Описание действия",
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
    void shouldFindEntityType() {
        // When
        Optional<EntityType> result = databaseResolver.findEntityType("Building");

        // Then
        assertTrue(result.isPresent());
        EntityType entityType = result.get();
        assertEquals("Building", entityType.id());
        assertEquals("Здание", entityType.name());
        assertTrue(entityType.metadata().containsKey("description"));
    }

    @Test
    void shouldReturnEmptyWhenEntityTypeNotFound() {
        // When
        Optional<EntityType> result = databaseResolver.findEntityType("NonExistent");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldFindAction() {
        // When
        Optional<Action> result = databaseResolver.findAction("order_egrn_extract");

        // Then
        assertTrue(result.isPresent());
        Action action = result.get();
        assertEquals("order_egrn_extract", action.id());
        assertEquals("Заказать выписку из ЕГРН", action.name());
        assertTrue(action.isApplicableTo("Building"));
        assertFalse(action.isApplicableTo("Contract"));
    }

    @Test
    void shouldReturnEmptyWhenActionNotFound() {
        // When
        Optional<Action> result = databaseResolver.findAction("non_existent");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldFindUIBinding() {
        // When
        Optional<UIBinding> result = databaseResolver.findUIBinding("order_egrn_extract");

        // Then
        assertTrue(result.isPresent());
        UIBinding uiBinding = result.get();
        assertEquals("order_egrn_extract", uiBinding.actionId());
        assertEquals("[data-action='order_egrn_extract']", uiBinding.selector());
        assertEquals(UIBinding.SelectorType.CSS, uiBinding.selectorType());
    }

    @Test
    void shouldReturnEmptyWhenUIBindingNotFound() {
        // When
        Optional<UIBinding> result = databaseResolver.findUIBinding("non_existent");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldCheckActionApplicability() {
        // Then
        assertTrue(databaseResolver.isActionApplicable("order_egrn_extract", "Building"));
        assertFalse(databaseResolver.isActionApplicable("order_egrn_extract", "Contract"));
        assertFalse(databaseResolver.isActionApplicable("non_existent", "Building"));
    }

    @Test
    void shouldConvertSelectorTypes() {
        // Given
        UIBindingEntity entity = new UIBindingEntity(
            "test_action",
            "selector",
            UIBindingEntity.SelectorType.XPATH,
            Map.of()
        );
        uiBindingRepository.save(entity);

        // When
        Optional<UIBinding> result = databaseResolver.findUIBinding("test_action");

        // Then
        assertTrue(result.isPresent());
        assertEquals(UIBinding.SelectorType.XPATH, result.get().selectorType());
    }
}
