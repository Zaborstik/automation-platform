package com.zaborstik.platform.knowledge.scanner;

import com.zaborstik.platform.knowledge.model.PageKnowledge;

public interface AppScanner {

    PageKnowledge scanPage(String html, String pageUrl);
}
