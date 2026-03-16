package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.knowledge.model.AppKnowledge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryKnowledgeRepositoryTest {

    private InMemoryKnowledgeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryKnowledgeRepository();
    }

    @Test
    void shouldSaveAndFindByAppId() {
        AppKnowledge app = app("app-1", "https://a.example");
        repository.save(app);
        assertTrue(repository.findByAppId("app-1").isPresent());
    }

    @Test
    void shouldSaveAndFindByBaseUrl() {
        AppKnowledge app = app("app-1", "https://a.example");
        repository.save(app);
        assertTrue(repository.findByBaseUrl("https://a.example").isPresent());
    }

    @Test
    void shouldListAllSavedApps() {
        repository.save(app("app-1", "https://a.example"));
        repository.save(app("app-2", "https://b.example"));
        assertEquals(2, repository.listAll().size());
    }

    @Test
    void shouldDeleteByAppId() {
        repository.save(app("app-1", "https://a.example"));
        repository.deleteByAppId("app-1");
        assertFalse(repository.findByAppId("app-1").isPresent());
        assertFalse(repository.findByBaseUrl("https://a.example").isPresent());
    }

    @Test
    void shouldReturnEmptyForUnknownAppId() {
        assertFalse(repository.findByAppId("missing").isPresent());
    }

    @Test
    void shouldPersistSeveralApps() {
        repository.save(app("app-1", "https://a.example"));
        repository.save(app("app-2", "https://b.example"));
        repository.save(app("app-3", "https://c.example"));
        List<AppKnowledge> all = repository.listAll();
        assertEquals(3, all.size());
    }

    private static AppKnowledge app(String id, String baseUrl) {
        return new AppKnowledge(id, "App " + id, baseUrl, List.of(), Instant.now());
    }
}
