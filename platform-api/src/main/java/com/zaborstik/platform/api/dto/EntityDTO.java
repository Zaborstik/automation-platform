package com.zaborstik.platform.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/**
 * Универсальный DTO для API: tableName + id + data.
 * Персистенция по схеме newdatabase.drawio (zbrtstk / system), ответ API в этом виде.
 */
public class EntityDTO {

    public static final String TABLE_EXECUTION_REQUEST = "execution_request";
    public static final String TABLE_PLANS = "plans";
    public static final String TABLE_PLAN_STEPS = "plan_steps";
    public static final String TABLE_ATTACHMENTS = "attachments";

    @JsonProperty("tableName")
    private String tableName;

    @JsonProperty("id")
    private String id;

    @JsonProperty("data")
    private Map<String, Object> data;

    public EntityDTO() {
        this.data = Map.of();
    }

    public EntityDTO(String tableName, String id, Map<String, Object> data) {
        this.tableName = tableName;
        this.id = id;
        this.data = data != null ? new java.util.HashMap<>(data) : new java.util.HashMap<>();
    }

    public EntityDTO(String tableName, Map<String, Object> data) {
        this(tableName, null, data);
    }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Map<String, Object> getData() {
        return data == null ? Collections.emptyMap() : Collections.unmodifiableMap(data);
    }

    public void setData(Map<String, Object> data) {
        this.data = data != null ? new java.util.HashMap<>(data) : new java.util.HashMap<>();
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
