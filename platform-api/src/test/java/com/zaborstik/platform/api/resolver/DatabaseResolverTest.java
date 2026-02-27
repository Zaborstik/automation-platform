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
        action.setDescription("Описание действия");
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
    void shouldFindEntityType() {
        Optional<EntityType> result = databaseResolver.findEntityType("Building");
        assertTrue(result.isPresent());
        assertEquals("Building", result.get().id());
        assertEquals("Здание", result.get().name());
        assertTrue(result.get().metadata().containsKey("description"));
    }

    @Test
    void shouldReturnEmptyWhenEntityTypeNotFound() {
        assertFalse(databaseResolver.findEntityType("NonExistent").isPresent());
    }

    @Test
    void shouldFindAction() {
        Optional<Action> result = databaseResolver.findAction("order_egrn_extract");
        assertTrue(result.isPresent());
        assertEquals("order_egrn_extract", result.get().id());
        assertTrue(result.get().isApplicableTo("Building"));
        assertFalse(result.get().isApplicableTo("Contract"));
    }

    @Test
    void shouldReturnEmptyWhenActionNotFound() {
        assertFalse(databaseResolver.findAction("non_existent").isPresent());
    }

    @Test
    void shouldFindUIBinding() {
        Optional<UIBinding> result = databaseResolver.findUIBinding("order_egrn_extract");
        assertTrue(result.isPresent());
        assertEquals("[data-action='order_egrn_extract']", result.get().selector());
        assertEquals(UIBinding.SelectorType.CSS, result.get().selectorType());
    }

    @Test
    void shouldReturnEmptyWhenUIBindingNotFound() {
        assertFalse(databaseResolver.findUIBinding("non_existent").isPresent());
    }

    @Test
    void shouldCheckActionApplicability() {
        assertTrue(databaseResolver.isActionApplicable("order_egrn_extract", "Building"));
        assertFalse(databaseResolver.isActionApplicable("order_egrn_extract", "Contract"));
    }

    @Test
    void shouldConvertSelectorTypes() {
        UIBindingEntity ui = new UIBindingEntity();
        ui.setAction("test_action");
        ui.setSelector("selector");
        ui.setSelectorType(UIBindingEntity.SelectorType.XPATH);
        ui.setMetadata(Map.of());
        uiBindingRepository.save(ui);
        Optional<UIBinding> result = databaseResolver.findUIBinding("test_action");
        assertTrue(result.isPresent());
        assertEquals(UIBinding.SelectorType.XPATH, result.get().selectorType());
    }
}
