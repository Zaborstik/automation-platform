package com.zaborstik.platform.core.resolver;

import com.zaborstik.platform.core.domain.UIBinding;

import java.util.Optional;

/**
 * Utility for resolving plan step entity_id to selector.
 * Handles action(actionId), element(elementName), raw selectors, URLs.
 */
public final class TargetResolution {

    private TargetResolution() {
    }

    /**
     * Resolves target to selector string.
     *
     * @param resolver       Resolver for UIBinding lookup
     * @param elementResolver optional for element name resolution; can be null
     * @param target         entity_id from plan step
     * @param pageUrl        optional page URL for element resolution
     * @return resolved selector/URL, or target if passthrough
     */
    public static String resolve(Resolver resolver, ElementResolver elementResolver,
                                String target, String pageUrl) {
        if (target == null || target.isBlank()) {
            return "";
        }
        String t = target.trim();

        // action(actionId) -> UIBinding
        if (t.startsWith("action(") && t.endsWith(")")) {
            String actionId = t.substring(7, t.length() - 1).trim();
            Optional<UIBinding> binding = resolver.findUIBinding(actionId);
            if (binding.isPresent()) {
                UIBinding b = binding.get();
                if (b.elementName() != null && !b.elementName().isBlank() && elementResolver != null) {
                    Optional<String> resolved = elementResolver.findSelectorByElementName(b.elementName(), pageUrl);
                    if (resolved.isPresent()) {
                        return resolved.get();
                    }
                }
                if (b.selector() != null && !b.selector().isBlank()) {
                    return b.selector();
                }
            }
            return target;
        }

        // element(elementName) or plain elementName
        if (elementResolver != null) {
            String elementName;
            if (t.startsWith("element(") && t.endsWith(")")) {
                elementName = t.substring(8, t.length() - 1).trim();
            } else if (!looksLikeSelectorOrUrl(t)) {
                elementName = t;
            } else {
                elementName = null;
            }
            if (elementName != null && !elementName.isBlank()) {
                Optional<String> resolved = elementResolver.findSelectorByElementName(elementName, pageUrl);
                if (resolved.isPresent()) {
                    return resolved.get();
                }
            }
        }

        // Passthrough: selector, URL
        return target;
    }

    private static boolean looksLikeSelectorOrUrl(String s) {
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return true;
        }
        if (s.contains("#") || s.contains("[") || s.startsWith("//")) {
            return true;
        }
        return false;
    }
}
