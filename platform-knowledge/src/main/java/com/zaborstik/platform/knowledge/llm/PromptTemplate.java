package com.zaborstik.platform.knowledge.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public final class PromptTemplate {

    private PromptTemplate() {
    }

    public static String load(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath cannot be null");
        try (InputStream stream = PromptTemplate.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + resourcePath, e);
        }
    }

    public static String fill(String template, Map<String, String> variables) {
        Objects.requireNonNull(template, "template cannot be null");
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = "{{" + entry.getKey() + "}}";
            String value = Objects.requireNonNullElse(entry.getValue(), "");
            result = result.replace(key, value);
        }
        return result;
    }

    public static String loadAndFill(String resourcePath, Map<String, String> variables) {
        return fill(load(resourcePath), variables);
    }
}
