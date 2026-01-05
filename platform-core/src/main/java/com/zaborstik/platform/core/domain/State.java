package com.zaborstik.platform.core.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Состояние сущности или системы.
 * Используется для проверки preconditions/postconditions и state transitions.
 */
public class State {
    private final String id;
    private final String name;
    private final Map<String, Object> properties;

    public State(String id, String name, Map<String, Object> properties) {
        this.id = Objects.requireNonNull(id, "State id cannot be null");
        this.name = Objects.requireNonNull(name, "State name cannot be null");
        this.properties = properties != null ? Map.copyOf(properties) : Map.of();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return Objects.equals(id, state.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "State{id='" + id + "', name='" + name + "'}";
    }
}

