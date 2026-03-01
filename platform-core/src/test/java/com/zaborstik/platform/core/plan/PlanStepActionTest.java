package com.zaborstik.platform.core.plan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlanStepActionTest {

    @Test
    void shouldCreateWithMetaValue() {
        PlanStepAction psa = new PlanStepAction("act-click", "search query text");
        assertEquals("act-click", psa.actionId());
        assertEquals("search query text", psa.metaValue());
    }

    @Test
    void shouldCreateWithNullMetaValue() {
        PlanStepAction psa = new PlanStepAction("act-input-text", null);
        assertEquals("act-input-text", psa.actionId());
        assertNull(psa.metaValue());
    }

    @Test
    void shouldThrowWhenActionIdNull() {
        assertThrows(NullPointerException.class, () -> new PlanStepAction(null, "meta"));
    }
}
