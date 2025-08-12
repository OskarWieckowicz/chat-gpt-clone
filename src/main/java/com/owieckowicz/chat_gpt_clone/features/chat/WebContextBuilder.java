package com.owieckowicz.chat_gpt_clone.features.chat;

import org.springframework.stereotype.Component;

@Component
public class WebContextBuilder {
    private static final int MAX_TOTAL_WEB_CONTEXT_CHARS = 8_000;
    private static final int MAX_PER_DOC_CHARS = 3_000;

    private final GoogleSearchService googleSearchService;
    private final WebFetchService webFetchService;
    private final SearchQueryService searchQueryService;

    public WebContextBuilder(GoogleSearchService googleSearchService,
                             WebFetchService webFetchService,
                             SearchQueryService searchQueryService) {
        this.googleSearchService = googleSearchService;
        this.webFetchService = webFetchService;
        this.searchQueryService = searchQueryService;
    }

    public String build(String userMessage, int topK, boolean enabled, Object chatClient) {
        if (!enabled) return null;
        String query = searchQueryService.craftWebSearchQuery((org.springframework.ai.chat.client.ChatClient) chatClient, userMessage);
        if (query == null || query.isBlank()) return null;

        var hits = googleSearchService.search(query, topK);
        if (hits.isEmpty()) return null;

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("You can use the following web context. Cite sources inline as [n] and end with a 'Sources' section listing the referenced URLs.\n");

        int totalChars = 0;
        int index = 1;
        for (var hit : hits) {
            String url = hit.url();
            if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) continue;
            String text;
            try {
                text = webFetchService.fetchText(url, MAX_PER_DOC_CHARS);
            } catch (Exception e) {
                continue;
            }
            if (text == null || text.isBlank()) continue;

            String header = "[" + (index++) + "] " + url + ": ";
            if (totalChars + header.length() + text.length() > MAX_TOTAL_WEB_CONTEXT_CHARS) break;
            contextBuilder.append(header).append(text).append('\n');
            totalChars += header.length() + text.length();
        }

        return totalChars > 0 ? contextBuilder.toString() : null;
    }
}


