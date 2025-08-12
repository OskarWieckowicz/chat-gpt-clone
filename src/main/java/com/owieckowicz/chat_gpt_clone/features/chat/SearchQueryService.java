package com.owieckowicz.chat_gpt_clone.features.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SearchQueryService {

    public String craftWebSearchQuery(ChatClient chatClient, String userMessage) {
        try {
            String instruction = "You write concise web search queries.\n"
                    + "Constraints:\n"
                    + "- 3 to 10 words.\n"
                    + "- No quotes or punctuation at ends.\n"
                    + "- Avoid code, stopwords, and filler.\n"
                    + "- Output ONLY the query string, nothing else.";
            String raw = chatClient
                    .prompt()
                    .system(instruction)
                    .user("Question: " + userMessage)
                    .call()
                    .content();
            if (raw == null) return null;
            String trimmed = raw.trim()
                    .replaceAll("\r", " ")
                    .replaceAll("\n", " ")
                    .replaceAll("[\u201C\u201D\"']", "")
                    .replaceAll("\s+", " ")
                    .trim();
            if (trimmed.length() > 160) trimmed = trimmed.substring(0, 160);
            if (trimmed.length() < 3) return null;
            return trimmed;
        } catch (Exception e) {
            return null;
        }
    }
}


