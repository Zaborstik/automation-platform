package com.zaborstik.platform.knowledge.scanner;

import com.zaborstik.platform.knowledge.model.PageKnowledge;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicAppScannerTest {

    private final BasicAppScanner scanner = new BasicAppScanner();

    @Test
    void shouldParseSimpleFormPage() {
        String html = load("/sample-pages/simple-form.html");

        PageKnowledge page = scanner.scanPage(html, "/simple-form");

        long forms = page.elements().stream().filter(e -> "form".equals(e.elementType())).count();
        long inputs = page.elements().stream().filter(e -> "input".equals(e.elementType())).count();
        long buttons = page.elements().stream().filter(e -> "button".equals(e.elementType())).count();

        assertEquals(1, forms);
        assertEquals(3, inputs);
        assertEquals(1, buttons);
    }

    @Test
    void shouldParseSearchPageWithInputButtonLinksAndTable() {
        String html = load("/sample-pages/search-page.html");

        PageKnowledge page = scanner.scanPage(html, "/search");

        assertTrue(page.elements().stream().anyMatch(e -> "input".equals(e.elementType())));
        assertTrue(page.elements().stream().anyMatch(e -> "button".equals(e.elementType())));
        assertTrue(page.elements().stream().anyMatch(e -> "link".equals(e.elementType())));
        assertTrue(page.elements().stream().anyMatch(e -> "table".equals(e.elementType())));
    }

    @Test
    void shouldReturnZeroElementsForEmptyHtml() {
        PageKnowledge page = scanner.scanPage("", "/empty");
        assertEquals(0, page.elements().size());
    }

    @Test
    void shouldExtractTitle() {
        String html = load("/sample-pages/search-page.html");
        PageKnowledge page = scanner.scanPage(html, "/search");
        assertEquals("Search Results", page.pageTitle());
    }

    @Test
    void shouldBuildCorrectCssSelectors() {
        String html = load("/sample-pages/search-page.html");
        PageKnowledge page = scanner.scanPage(html, "/search");
        assertTrue(page.elements().stream().anyMatch(e -> "input#search-input".equals(e.selector())));
        assertTrue(page.elements().stream().anyMatch(e -> "button#search-btn".equals(e.selector())));
        assertTrue(page.elements().stream().anyMatch(e -> "form#search-form".equals(e.selector())));
        assertTrue(page.elements().stream().anyMatch(e -> "table#results-table".equals(e.selector())));
    }

    @Test
    void shouldDeriveElementNamesFromIdAndAttributes() {
        String html = load("/sample-pages/search-page.html");
        PageKnowledge page = scanner.scanPage(html, "/search");
        assertTrue(page.elements().stream().anyMatch(e -> "search_input".equals(e.elementName())));
        assertTrue(page.elements().stream().anyMatch(e -> "search_btn".equals(e.elementName())));
        assertTrue(page.elements().stream().anyMatch(e -> "search_form".equals(e.elementName())));
        assertTrue(page.elements().stream().anyMatch(e -> "results_table".equals(e.elementName())));
    }

    private static String load(String path) {
        try (InputStream stream = BasicAppScannerTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resource: " + path, e);
        }
    }
}
