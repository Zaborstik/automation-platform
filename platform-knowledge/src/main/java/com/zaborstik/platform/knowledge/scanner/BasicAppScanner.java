package com.zaborstik.platform.knowledge.scanner;

import com.zaborstik.platform.knowledge.model.PageKnowledge;
import com.zaborstik.platform.knowledge.model.UIElement;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BasicAppScanner implements AppScanner {

    @Override
    public PageKnowledge scanPage(String html, String pageUrl) {
        Objects.requireNonNull(pageUrl, "pageUrl cannot be null");
        Document document = Jsoup.parse(Objects.requireNonNullElse(html, ""));
        List<UIElement> elements = new ArrayList<>();

        collectElements(document.select("input"), "input", elements);
        collectElements(document.select("button"), "button", elements);
        collectElements(document.select("a[href]"), "link", elements);
        collectElements(document.select("form"), "form", elements);
        collectElements(document.select("table"), "table", elements);
        collectElements(document.select("select"), "input", elements);
        collectElements(document.select("textarea"), "input", elements);

        String pageTitle = document.title();
        if (pageTitle != null && pageTitle.isBlank()) {
            pageTitle = null;
        }

        return new PageKnowledge(pageUrl, pageTitle, elements, Instant.now());
    }

    private void collectElements(Elements domElements, String elementType, List<UIElement> out) {
        for (Element element : domElements) {
            Map<String, String> attributes = extractAttributes(element, elementType);
            out.add(new UIElement(
                buildCssSelector(element),
                "CSS",
                elementType,
                extractLabel(element),
                attributes
            ));
        }
    }

    private Map<String, String> extractAttributes(Element element, String elementType) {
        Map<String, String> attributes = new HashMap<>();
        putIfPresent(attributes, "name", element.attr("name"));
        putIfPresent(attributes, "type", element.attr("type"));
        putIfPresent(attributes, "role", element.attr("role"));
        putIfPresent(attributes, "placeholder", element.attr("placeholder"));
        putIfPresent(attributes, "value", element.attr("value"));

        if ("select".equals(element.tagName())) {
            attributes.put("type", "select");
        } else if ("textarea".equals(element.tagName())) {
            attributes.putIfAbsent("type", "textarea");
        }
        if ("input".equals(elementType) && "select".equals(element.tagName())) {
            attributes.put("type", "select");
        }
        return attributes;
    }

    private static void putIfPresent(Map<String, String> attributes, String key, String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value);
        }
    }

    private String extractLabel(Element element) {
        String text = element.text();
        if (text != null && !text.isBlank()) {
            return text.trim();
        }
        String placeholder = element.attr("placeholder");
        if (!placeholder.isBlank()) {
            return placeholder.trim();
        }
        String ariaLabel = element.attr("aria-label");
        if (!ariaLabel.isBlank()) {
            return ariaLabel.trim();
        }
        String title = element.attr("title");
        if (!title.isBlank()) {
            return title.trim();
        }
        String value = element.attr("value");
        if (!value.isBlank()) {
            return value.trim();
        }
        return null;
    }

    private String buildCssSelector(Element element) {
        String tag = element.tagName();
        String id = element.id();
        if (!id.isBlank()) {
            return tag + "#" + id;
        }

        String name = element.attr("name");
        if (!name.isBlank()) {
            return tag + "[name='" + name + "']";
        }

        String classNames = element.className();
        if (!classNames.isBlank()) {
            String classes = String.join(".", classNames.trim().split("\\s+"));
            return tag + "." + classes;
        }
        return tag;
    }
}
