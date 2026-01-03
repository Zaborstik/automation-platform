package org.example.core.resolver;

import org.example.core.domain.Action;
import org.example.core.domain.EntityType;
import org.example.core.domain.UIBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryResolverTest {

    private InMemoryResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new InMemoryResolver();
    }

    @Test
    void shouldRegisterAndFindEntityType() {
        EntityType entityType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(entityType);

        Optional<EntityType> found = resolver.findEntityType("Building");
        assertTrue(found.isPresent());
        assertEquals(entityType, found.get());
    }

    @Test
    void shouldReturnEmptyWhenEntityTypeNotFound() {
        Optional<EntityType> found = resolver.findEntityType("NonExistent");
        assertFalse(found.isPresent());
    }

    @Test
    void shouldRegisterAndFindAction() {
        Action action = new Action(
            "order_egrn_extract",
            "Заказать выписку",
            "Описание",
            Set.of("Building"),
            Map.of()
        );
        resolver.registerAction(action);

        Optional<Action> found = resolver.findAction("order_egrn_extract");
        assertTrue(found.isPresent());
        assertEquals(action, found.get());
    }

    @Test
    void shouldReturnEmptyWhenActionNotFound() {
        Optional<Action> found = resolver.findAction("non_existent");
        assertFalse(found.isPresent());
    }

    @Test
    void shouldRegisterAndFindUIBinding() {
        UIBinding uiBinding = new UIBinding(
            "order_egrn_extract",
            "[data-action='order_egrn_extract']",
            UIBinding.SelectorType.CSS,
            Map.of()
        );
        resolver.registerUIBinding(uiBinding);

        Optional<UIBinding> found = resolver.findUIBinding("order_egrn_extract");
        assertTrue(found.isPresent());
        assertEquals(uiBinding, found.get());
    }

    @Test
    void shouldReturnEmptyWhenUIBindingNotFound() {
        Optional<UIBinding> found = resolver.findUIBinding("non_existent");
        assertFalse(found.isPresent());
    }

    @Test
    void shouldCheckActionApplicability() {
        Action action = new Action(
            "order_egrn_extract",
            "Заказать выписку",
            "Описание",
            Set.of("Building", "Contract"),
            Map.of()
        );
        resolver.registerAction(action);

        assertTrue(resolver.isActionApplicable("order_egrn_extract", "Building"));
        assertTrue(resolver.isActionApplicable("order_egrn_extract", "Contract"));
        assertFalse(resolver.isActionApplicable("order_egrn_extract", "Extract"));
    }

    @Test
    void shouldReturnFalseWhenActionNotFoundForApplicabilityCheck() {
        assertFalse(resolver.isActionApplicable("non_existent", "Building"));
    }

    @Test
    void shouldSupportMultipleEntityTypes() {
        EntityType building = new EntityType("Building", "Здание", Map.of());
        EntityType contract = new EntityType("Contract", "Договор", Map.of());
        
        resolver.registerEntityType(building);
        resolver.registerEntityType(contract);

        assertTrue(resolver.findEntityType("Building").isPresent());
        assertTrue(resolver.findEntityType("Contract").isPresent());
    }

    @Test
    void shouldSupportMultipleActions() {
        Action action1 = new Action("action1", "Action 1", "Desc", Set.of(), Map.of());
        Action action2 = new Action("action2", "Action 2", "Desc", Set.of(), Map.of());
        
        resolver.registerAction(action1);
        resolver.registerAction(action2);

        assertTrue(resolver.findAction("action1").isPresent());
        assertTrue(resolver.findAction("action2").isPresent());
    }

    @Test
    void shouldSupportMultipleUIBindings() {
        UIBinding binding1 = new UIBinding("action1", "selector1", UIBinding.SelectorType.CSS, Map.of());
        UIBinding binding2 = new UIBinding("action2", "selector2", UIBinding.SelectorType.XPATH, Map.of());
        
        resolver.registerUIBinding(binding1);
        resolver.registerUIBinding(binding2);

        assertTrue(resolver.findUIBinding("action1").isPresent());
        assertTrue(resolver.findUIBinding("action2").isPresent());
    }

    @Test
    void shouldOverwriteExistingEntityType() {
        EntityType original = new EntityType("Building", "Здание", Map.of());
        EntityType updated = new EntityType("Building", "Обновленное здание", Map.of("key", "value"));
        
        resolver.registerEntityType(original);
        resolver.registerEntityType(updated);

        Optional<EntityType> found = resolver.findEntityType("Building");
        assertTrue(found.isPresent());
        assertEquals("Обновленное здание", found.get().getName());
    }

    @Test
    void shouldOverwriteExistingAction() {
        Action original = new Action("action", "Original", "Desc", Set.of(), Map.of());
        Action updated = new Action("action", "Updated", "New Desc", Set.of("Building"), Map.of());
        
        resolver.registerAction(original);
        resolver.registerAction(updated);

        Optional<Action> found = resolver.findAction("action");
        assertTrue(found.isPresent());
        assertEquals("Updated", found.get().getName());
    }

    @Test
    void shouldOverwriteExistingUIBinding() {
        UIBinding original = new UIBinding("action", "selector1", UIBinding.SelectorType.CSS, Map.of());
        UIBinding updated = new UIBinding("action", "selector2", UIBinding.SelectorType.XPATH, Map.of());
        
        resolver.registerUIBinding(original);
        resolver.registerUIBinding(updated);

        Optional<UIBinding> found = resolver.findUIBinding("action");
        assertTrue(found.isPresent());
        assertEquals("selector2", found.get().getSelector());
    }
}

