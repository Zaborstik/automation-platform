package com.zaborstik.platform.core.domain;

import java.util.Objects;

/**
 * Вложение (zbrtstk.attachment).
 * Например скриншот ошибки/экрана пользователя.
 */
public record Attachment(String id, String displayName) {
    public Attachment(String id, String displayName) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.displayName = displayName;
    }
}
