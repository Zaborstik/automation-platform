package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.knowledge.model.AppKnowledge;
import com.zaborstik.platform.knowledge.model.PageKnowledge;
import com.zaborstik.platform.knowledge.model.ParsedUserRequest;
import com.zaborstik.platform.knowledge.scanner.AppScanner;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class KnowledgeService {

    private final KnowledgeRepository repository;
    private final AppScanner scanner;
    private final EntityTypeDiscovery discovery;
    private final UserRequestParser parser;
    private final PlanGenerator planGenerator;

    public KnowledgeService(KnowledgeRepository repository, AppScanner scanner, EntityTypeDiscovery discovery,
                            UserRequestParser parser, PlanGenerator planGenerator) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.scanner = Objects.requireNonNull(scanner, "scanner cannot be null");
        this.discovery = Objects.requireNonNull(discovery, "discovery cannot be null");
        this.parser = Objects.requireNonNull(parser, "parser cannot be null");
        this.planGenerator = Objects.requireNonNull(planGenerator, "planGenerator cannot be null");
    }

    public AppKnowledge scanAndStore(String appName, String baseUrl, String html) {
        Objects.requireNonNull(appName, "appName cannot be null");
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");

        PageKnowledge page = scanner.scanPage(html, baseUrl);
        discovery.discoverApplicableActions(page);

        AppKnowledge knowledge = new AppKnowledge(
            UUID.randomUUID().toString(),
            appName,
            baseUrl,
            List.of(page),
            Instant.now()
        );
        repository.save(knowledge);
        return knowledge;
    }

    public Optional<AppKnowledge> getKnowledge(String appId) {
        return repository.findByAppId(appId);
    }

    public Plan generatePlanFromRequest(String userInput) {
        ParsedUserRequest parsed = parser.parse(userInput);
        if (parsed.clarificationNeeded()) {
            throw new ClarificationNeededException(parsed.clarificationQuestion());
        }
        return planGenerator.generate(parsed);
    }
}
