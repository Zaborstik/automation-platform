package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.knowledge.model.AppKnowledge;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryKnowledgeRepository implements KnowledgeRepository {

    private final ConcurrentHashMap<String, AppKnowledge> byAppId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> appIdByBaseUrl = new ConcurrentHashMap<>();

    @Override
    public void save(AppKnowledge knowledge) {
        Objects.requireNonNull(knowledge, "knowledge cannot be null");
        byAppId.put(knowledge.appId(), knowledge);
        appIdByBaseUrl.put(knowledge.baseUrl(), knowledge.appId());
    }

    @Override
    public Optional<AppKnowledge> findByAppId(String appId) {
        return Optional.ofNullable(byAppId.get(appId));
    }

    @Override
    public Optional<AppKnowledge> findByBaseUrl(String baseUrl) {
        String appId = appIdByBaseUrl.get(baseUrl);
        if (appId == null) {
            return Optional.empty();
        }
        return findByAppId(appId);
    }

    @Override
    public List<AppKnowledge> listAll() {
        return List.copyOf(byAppId.values());
    }

    @Override
    public void deleteByAppId(String appId) {
        AppKnowledge removed = byAppId.remove(appId);
        if (removed != null) {
            appIdByBaseUrl.remove(removed.baseUrl(), appId);
        }
    }
}
