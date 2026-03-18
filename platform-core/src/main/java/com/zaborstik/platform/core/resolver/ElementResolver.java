package com.zaborstik.platform.core.resolver;

import java.util.Optional;

/**
 * Resolves element names to selectors using application knowledge (e.g. from scanned pages).
 * Used for universal storage: plans store element names, resolution happens at execution time.
 */
public interface ElementResolver {

    /**
     * Finds a CSS/XPATH selector for the given element name.
     *
     * @param elementName stable semantic identifier (e.g. "search_input", "submit_btn")
     * @param pageUrl     optional page URL for scoping; if null, search across all known pages
     * @return selector if found, empty otherwise
     */
    Optional<String> findSelectorByElementName(String elementName, String pageUrl);
}
