package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.knowledge.model.AppKnowledge;

import java.util.List;
import java.util.Optional;

public interface KnowledgeRepository {

    void save(AppKnowledge knowledge);

    Optional<AppKnowledge> findByAppId(String appId);

    Optional<AppKnowledge> findByBaseUrl(String baseUrl);

    List<AppKnowledge> listAll();

    void deleteByAppId(String appId);
}
