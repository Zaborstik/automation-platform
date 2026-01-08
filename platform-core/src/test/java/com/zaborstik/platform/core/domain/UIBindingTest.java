package com.zaborstik.platform.core.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UIBindingTest {

    @Test
    void shouldCreateUIBindingWithAllFields() {
        Map<String, Object> metadata = Map.of("fallback", "vision");
        UIBinding binding = new UIBinding(
            "order_egrn_extract",
            "[data-action='order_egrn_extract']",
            UIBinding.SelectorType.CSS,
            metadata
        );

        assertEquals("order_egrn_extract", binding.actionId());
        assertEquals("[data-action='order_egrn_extract']", binding.selector());
        assertEquals(UIBinding.SelectorType.CSS, binding.selectorType());
        assertEquals(metadata, binding.metadata());
    }

    @Test
    void shouldCreateUIBindingWithNullMetadata() {
        UIBinding binding = new UIBinding(
            "order_egrn_extract",
            "selector",
            UIBinding.SelectorType.CSS,
            null
        );

        assertEquals("order_egrn_extract", binding.actionId());
        assertTrue(binding.metadata().isEmpty());
    }

    @Test
    void shouldCreateUIBindingWithAllSelectorTypes() {
        UIBinding cssBinding = new UIBinding("action1", "div.class", UIBinding.SelectorType.CSS, null);
        assertEquals(UIBinding.SelectorType.CSS, cssBinding.selectorType());

        UIBinding xpathBinding = new UIBinding("action2", "//div", UIBinding.SelectorType.XPATH, null);
        assertEquals(UIBinding.SelectorType.XPATH, xpathBinding.selectorType());

        UIBinding textBinding = new UIBinding("action3", "Button text", UIBinding.SelectorType.TEXT, null);
        assertEquals(UIBinding.SelectorType.TEXT, textBinding.selectorType());

        UIBinding actionIdBinding = new UIBinding("action4", "action_id", UIBinding.SelectorType.ACTION_ID, null);
        assertEquals(UIBinding.SelectorType.ACTION_ID, actionIdBinding.selectorType());
    }

    @Test
    void shouldThrowExceptionWhenActionIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new UIBinding(null, "selector", UIBinding.SelectorType.CSS, Map.of());
        });
    }

    @Test
    void shouldThrowExceptionWhenSelectorIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new UIBinding("action", null, UIBinding.SelectorType.CSS, Map.of());
        });
    }

    @Test
    void shouldThrowExceptionWhenSelectorTypeIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new UIBinding("action", "selector", null, Map.of());
        });
    }

    @Test
    void shouldReturnImmutableMetadata() {
        Map<String, Object> originalMetadata = Map.of("key", "value");
        UIBinding binding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, originalMetadata);

        Map<String, Object> returnedMetadata = binding.metadata();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedMetadata.put("newKey", "newValue");
        });
    }

    @Test
    void shouldBeEqualWhenActionIdsAreEqual() {
        UIBinding binding1 = new UIBinding("action", "selector1", UIBinding.SelectorType.CSS, Map.of());
        UIBinding binding2 = new UIBinding("action", "selector2", UIBinding.SelectorType.XPATH, Map.of("key", "value"));

        assertEquals(binding1, binding2);
        assertEquals(binding1.hashCode(), binding2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenActionIdsAreDifferent() {
        UIBinding binding1 = new UIBinding("action1", "selector", UIBinding.SelectorType.CSS, Map.of());
        UIBinding binding2 = new UIBinding("action2", "selector", UIBinding.SelectorType.CSS, Map.of());

        assertNotEquals(binding1, binding2);
    }

    @Test
    void shouldNotBeEqualWithNull() {
        UIBinding binding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        assertNotEquals(binding, null);
    }

    @Test
    void shouldNotBeEqualWithDifferentClass() {
        UIBinding binding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        assertNotEquals(binding, "action");
    }

    @Test
    void shouldReturnCorrectToString() {
        UIBinding binding = new UIBinding(
            "order_egrn_extract",
            "[data-action='order_egrn_extract']",
            UIBinding.SelectorType.CSS,
            Map.of()
        );
        String toString = binding.toString();

        assertTrue(toString.contains("order_egrn_extract"));
        assertTrue(toString.contains("[data-action='order_egrn_extract']"));
        assertTrue(toString.contains("CSS"));
        assertTrue(toString.contains("UIBinding"));
    }
}

