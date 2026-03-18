package com.zaborstik.platform.api.config;

import com.zaborstik.platform.api.resolver.DatabaseResolver;
import com.zaborstik.platform.core.resolver.Resolver;
import com.zaborstik.platform.knowledge.llm.LLMClient;
import com.zaborstik.platform.knowledge.llm.StubLLMClient;
import com.zaborstik.platform.knowledge.scanner.AppScanner;
import com.zaborstik.platform.knowledge.scanner.BasicAppScanner;
import com.zaborstik.platform.core.resolver.ElementResolver;
import com.zaborstik.platform.knowledge.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация компонентов platform-knowledge для использования в API.
 * Использует StubLLMClient по умолчанию; можно заменить на реальный LLM-клиент.
 */
@Configuration
public class KnowledgeConfiguration {

    @Bean
    public LLMClient llmClient() {
        return new StubLLMClient("{\"entityTypeId\":\"ent-page\",\"actionIds\":[\"act-open-page\"],\"clarificationNeeded\":false,\"clarificationQuestion\":null,\"parameters\":{\"target\":\"\"}}");
    }

    @Bean
    public KnowledgeRepository knowledgeRepository() {
        return new InMemoryKnowledgeRepository();
    }

    @Bean
    public ElementResolver elementResolver(KnowledgeRepository knowledgeRepository) {
        return new KnowledgeElementResolver(knowledgeRepository);
    }

    @Bean
    public AppScanner appScanner() {
        return new BasicAppScanner();
    }

    @Bean
    public EntityTypeDiscovery entityTypeDiscovery(Resolver resolver) {
        return new EntityTypeDiscovery(resolver);
    }

    @Bean
    public UserRequestParser userRequestParser(LLMClient llmClient, Resolver resolver) {
        return new UserRequestParser(llmClient, resolver);
    }

    @Bean
    public PlanGenerator planGenerator(Resolver resolver) {
        return new PlanGenerator(resolver);
    }

    @Bean
    public KnowledgeService knowledgeService(KnowledgeRepository knowledgeRepository,
                                            AppScanner appScanner,
                                            EntityTypeDiscovery entityTypeDiscovery,
                                            UserRequestParser userRequestParser,
                                            PlanGenerator planGenerator) {
        return new KnowledgeService(knowledgeRepository, appScanner, entityTypeDiscovery, userRequestParser, planGenerator);
    }
}
