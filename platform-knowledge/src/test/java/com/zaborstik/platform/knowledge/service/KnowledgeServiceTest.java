package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import com.zaborstik.platform.knowledge.llm.StubLLMClient;
import com.zaborstik.platform.knowledge.model.AppKnowledge;
import com.zaborstik.platform.knowledge.model.PageKnowledge;
import com.zaborstik.platform.knowledge.model.ParsedUserRequest;
import com.zaborstik.platform.knowledge.scanner.AppScanner;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeServiceTest {

    @Test
    void scanAndStoreShouldPersistKnowledge() {
        InMemoryKnowledgeRepository repository = new InMemoryKnowledgeRepository();
        AppScanner scanner = (html, pageUrl) -> new PageKnowledge(pageUrl, "Title", List.of(), Instant.now());
        EntityTypeDiscovery discovery = new EntityTypeDiscovery(new InMemoryResolver());
        UserRequestParser parser = new FixedUserRequestParser(new ParsedUserRequest("x", "ent-input", List.of("act-click"), Map.of(), false, null));
        PlanGenerator generator = new FixedPlanGenerator(dummyPlan());

        KnowledgeService service = new KnowledgeService(repository, scanner, discovery, parser, generator);
        AppKnowledge saved = service.scanAndStore("App", "https://example.com", "<html></html>");

        assertTrue(service.getKnowledge(saved.appId()).isPresent());
        assertEquals("https://example.com", saved.baseUrl());
    }

    @Test
    void getKnowledgeShouldReturnSavedItem() {
        InMemoryKnowledgeRepository repository = new InMemoryKnowledgeRepository();
        AppScanner scanner = (html, pageUrl) -> new PageKnowledge(pageUrl, "Title", List.of(), Instant.now());
        EntityTypeDiscovery discovery = new EntityTypeDiscovery(new InMemoryResolver());
        UserRequestParser parser = new FixedUserRequestParser(new ParsedUserRequest("x", "ent-input", List.of("act-click"), Map.of(), false, null));
        PlanGenerator generator = new FixedPlanGenerator(dummyPlan());

        KnowledgeService service = new KnowledgeService(repository, scanner, discovery, parser, generator);
        AppKnowledge saved = service.scanAndStore("App", "https://example.com", "<html></html>");

        assertTrue(service.getKnowledge(saved.appId()).isPresent());
    }

    @Test
    void generatePlanFromRequestShouldReturnPlanWhenRequestIsValid() {
        InMemoryKnowledgeRepository repository = new InMemoryKnowledgeRepository();
        AppScanner scanner = (html, pageUrl) -> new PageKnowledge(pageUrl, "Title", List.of(), Instant.now());
        EntityTypeDiscovery discovery = new EntityTypeDiscovery(new InMemoryResolver());
        UserRequestParser parser = new FixedUserRequestParser(new ParsedUserRequest("x", "ent-input", List.of("act-click"), Map.of(), false, null));
        PlanGenerator generator = new FixedPlanGenerator(dummyPlan());

        KnowledgeService service = new KnowledgeService(repository, scanner, discovery, parser, generator);
        Plan result = service.generatePlanFromRequest("click");

        assertEquals("plan-1", result.id());
    }

    @Test
    void generatePlanFromRequestShouldThrowClarificationNeededException() {
        InMemoryKnowledgeRepository repository = new InMemoryKnowledgeRepository();
        AppScanner scanner = (html, pageUrl) -> new PageKnowledge(pageUrl, "Title", List.of(), Instant.now());
        EntityTypeDiscovery discovery = new EntityTypeDiscovery(new InMemoryResolver());
        UserRequestParser parser = new FixedUserRequestParser(new ParsedUserRequest("x", null, List.of(), Map.of(), true, "Что сделать?"));
        PlanGenerator generator = new FixedPlanGenerator(dummyPlan());

        KnowledgeService service = new KnowledgeService(repository, scanner, discovery, parser, generator);
        ClarificationNeededException ex = assertThrows(ClarificationNeededException.class,
            () -> service.generatePlanFromRequest("unknown"));
        assertEquals("Что сделать?", ex.getQuestion());
    }

    @Test
    void generatePlanFromRequestShouldPropagateInvalidRequestError() {
        InMemoryKnowledgeRepository repository = new InMemoryKnowledgeRepository();
        AppScanner scanner = (html, pageUrl) -> new PageKnowledge(pageUrl, "Title", List.of(), Instant.now());
        EntityTypeDiscovery discovery = new EntityTypeDiscovery(new InMemoryResolver());
        UserRequestParser parser = new FixedUserRequestParser(new ParsedUserRequest("x", "ent-input", List.of("act-click"), Map.of(), false, null));
        PlanGenerator generator = new ThrowingPlanGenerator();

        KnowledgeService service = new KnowledgeService(repository, scanner, discovery, parser, generator);
        assertThrows(IllegalArgumentException.class, () -> service.generatePlanFromRequest("click"));
    }

    private static Plan dummyPlan() {
        return new Plan(
            "plan-1",
            "wf-plan",
            "new",
            "step-1",
            "target",
            "explanation",
            List.of(new PlanStep(
                "step-1", "plan-1", "wf-plan-step", "new",
                "ent-button", "btn-1", 1, "Step", List.of(new PlanStepAction("act-click", null))
            ))
        );
    }

    private static class FixedUserRequestParser extends UserRequestParser {
        private final ParsedUserRequest parsed;

        FixedUserRequestParser(ParsedUserRequest parsed) {
            super(new StubLLMClient("{}"), new InMemoryResolver());
            this.parsed = parsed;
        }

        @Override
        public ParsedUserRequest parse(String userInput) {
            return parsed;
        }
    }

    private static class FixedPlanGenerator extends PlanGenerator {
        private final Plan plan;

        FixedPlanGenerator(Plan plan) {
            super(new InMemoryResolver());
            this.plan = plan;
        }

        @Override
        public Plan generate(ParsedUserRequest request) {
            return plan;
        }
    }

    private static class ThrowingPlanGenerator extends PlanGenerator {
        ThrowingPlanGenerator() {
            super(new InMemoryResolver());
        }

        @Override
        public Plan generate(ParsedUserRequest request) {
            throw new IllegalArgumentException("invalid");
        }
    }
}
