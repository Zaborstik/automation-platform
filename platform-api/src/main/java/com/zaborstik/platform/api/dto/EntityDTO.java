package com.zaborstik.platform.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Единый класс для API и БД: tableName + id + data (JSON).
 * Одна таблица {@code entities}, различие записей по {@code table_name}.
 */
@Entity
@Table(name = "entities", schema = "system", indexes = {
    @Index(name = "idx_entities_table_name", columnList = "table_name"),
    @Index(name = "idx_entities_table_id", columnList = "table_name, id")
})
public class EntityDTO {

    public static final String TABLE_EXECUTION_REQUEST = "execution_request";
    public static final String TABLE_PLANS = "plans";
    public static final String TABLE_PLAN_STEPS = "plan_steps";
    public static final String TABLE_ATTACHMENTS = "attachments";
    public static final String TABLE_ENTITY_TYPES = "entity_types";
    public static final String TABLE_ACTIONS = "actions";
    public static final String TABLE_UI_BINDINGS = "ui_bindings";

    @JsonIgnore
    @EmbeddedId
    private EntityId id;

    @JsonProperty("data")
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "data", columnDefinition = "CLOB")
    private Map<String, Object> data;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public EntityDTO() {
        this.data = Collections.emptyMap();
    }

    public EntityDTO(String tableName, String id, Map<String, Object> data) {
        this.id = new EntityId(tableName, id);
        this.data = data != null ? new java.util.HashMap<>(data) : new java.util.HashMap<>();
    }

    public EntityDTO(String tableName, Map<String, Object> data) {
        this(tableName, null, data);
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @JsonProperty("tableName")
    public String getTableName() {
        return id != null ? id.getTableName() : null;
    }

    public void setTableName(String tableName) {
        if (id == null) id = new EntityId();
        id.setTableName(tableName);
    }

    @JsonProperty("id")
    public String getId() {
        return id != null ? id.getId() : null;
    }

    public void setId(String id) {
        if (this.id == null) this.id = new EntityId();
        this.id.setId(id);
    }

    public Map<String, Object> getData() {
        return data == null ? Collections.emptyMap() : Collections.unmodifiableMap(data);
    }

    public void setData(Map<String, Object> data) {
        this.data = data != null ? new java.util.HashMap<>(data) : new java.util.HashMap<>();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @JsonIgnore
    public EntityId getEntityId() {
        return id;
    }

    public void setEntityId(EntityId id) {
        this.id = id;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return data == null ? null : (T) data.get(key);
    }

    public void put(String key, Object value) {
        if (data == null) data = new java.util.HashMap<>();
        if (!(data instanceof java.util.HashMap))
            data = new java.util.HashMap<>(data);
        ((java.util.Map<String, Object>) data).put(key, value);
    }
}
