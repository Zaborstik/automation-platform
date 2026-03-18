package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.core.resolver.ElementResolver;
import com.zaborstik.platform.knowledge.model.AppKnowledge;
import com.zaborstik.platform.knowledge.model.PageKnowledge;
import com.zaborstik.platform.knowledge.model.UIElement;

import java.util.Optional;

/**
 * Resolves element names to selectors using AppKnowledge from scanned pages.
 */
public class KnowledgeElementResolver implements ElementResolver {

    private final KnowledgeRepository knowledgeRepository;

    public KnowledgeElementResolver(KnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @Override
    public Optional<String> findSelectorByElementName(String elementName, String pageUrl) {
        if (elementName == null || elementName.isBlank()) {
            return Optional.empty();
        }
        String normalizedName = elementName.trim().toLowerCase().replaceAll("[-\\s]+", "_");

        if (pageUrl != null && !pageUrl.isBlank()) {
            return findByAppAndPage(elementName, normalizedName, pageUrl);
        }
        return findByElementNameOnly(elementName, normalizedName);
    }

    private Optional<String> findByAppAndPage(String originalName, String normalizedName, String pageUrl) {
        Optional<AppKnowledge> app = knowledgeRepository.findByBaseUrl(normalizeUrl(pageUrl));
        if (app.isEmpty()) {
            app = knowledgeRepository.listAll().stream()
                .filter(a -> a.baseUrl() != null && pageUrl.startsWith(a.baseUrl()))
                .findFirst();
        }
        if (app.isEmpty()) {
            return findByElementNameOnly(originalName, normalizedName);
        }
        for (PageKnowledge page : app.get().pages()) {
            if (pageUrlMatches(page.pageUrl(), pageUrl)) {
                for (UIElement el : page.elements()) {
                    if (elementNameMatches(el.elementName(), originalName, normalizedName)) {
                        return Optional.of(el.selector());
                    }
                }
                return Optional.empty();
            }
        }
        return findByElementNameOnly(originalName, normalizedName);
    }

    private Optional<String> findByElementNameOnly(String originalName, String normalizedName) {
        for (AppKnowledge app : knowledgeRepository.listAll()) {
            for (PageKnowledge page : app.pages()) {
                for (UIElement el : page.elements()) {
                    if (elementNameMatches(el.elementName(), originalName, normalizedName)) {
                        return Optional.of(el.selector());
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean elementNameMatches(String stored, String original, String normalized) {
        if (stored == null) {
            return false;
        }
        String storedNorm = stored.toLowerCase().replaceAll("[-\\s]+", "_");
        return storedNorm.equals(normalized) || stored.equals(original) || storedNorm.equals(original.toLowerCase());
    }

    private static boolean pageUrlMatches(String pageUrl, String target) {
        if (pageUrl == null || target == null) {
            return false;
        }
        return target.equals(pageUrl) || target.endsWith(pageUrl) || pageUrl.endsWith(target);
    }

    private static String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        String u = url.trim();
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
