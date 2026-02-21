package com.zaborstik.platform.api.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/** Составной ключ для EntityDTO: tableName + id. */
@Embeddable
public class EntityId implements Serializable {

    @Column(name = "table_name", length = 255)
    private String tableName;

    @Column(name = "id", length = 255)
    private String id;

    public EntityId() {
    }

    public EntityId(String tableName, String id) {
        this.tableName = tableName;
        this.id = id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityId entityId = (EntityId) o;
        return Objects.equals(tableName, entityId.tableName) && Objects.equals(id, entityId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, id);
    }
}
