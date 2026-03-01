package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

/**
 * Вложение (zbrtstk.attachment). Скриншоты, артефакты выполнения.
 */
@Entity
@Table(name = "attachment", schema = "zbrtstk")
public class AttachmentEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "displayname", length = 255)
    private String displayname;

    public AttachmentEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
}
