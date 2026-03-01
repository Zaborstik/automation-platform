package com.zaborstik.platform.core.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ActionTest {

    @Test
    void shouldCreateActionWithOf() {
        Action action = Action.of("act-click", "Клик", "click", "Нажатие на элемент.", "act-type-interaction");
        assertEquals("act-click", action.id());
        assertEquals("Клик", action.displayName());
        assertEquals("click", action.internalName());
        assertEquals("Нажатие на элемент.", action.description());
        assertEquals("act-type-interaction", action.actionTypeId());
        assertNull(action.metaValue());
    }

    @Test
    void shouldCreateActionWithAllFields() {
        Instant now = Instant.now();
        Action action = new Action(
            "act-open-page",
            "Открыть страницу",
            "open_page",
            "{\"url\":\"/page\"}",
            "Переход по URL.",
            "act-type-navigation",
            now,
            now
        );
        assertEquals("act-open-page", action.id());
        assertEquals("Открыть страницу", action.displayName());
        assertEquals("open_page", action.internalName());
        assertEquals("{\"url\":\"/page\"}", action.metaValue());
        assertEquals("act-type-navigation", action.actionTypeId());
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        Instant now = Instant.now();
        assertThrows(NullPointerException.class, () ->
            new Action(null, "Name", "internal", null, null, "act-type-1", now, now)
        );
    }

    @Test
    void shouldThrowExceptionWhenActionTypeIdIsNull() {
        Instant now = Instant.now();
        assertThrows(NullPointerException.class, () ->
            new Action("act-1", "Name", "internal", null, null, null, now, now)
        );
    }
}
