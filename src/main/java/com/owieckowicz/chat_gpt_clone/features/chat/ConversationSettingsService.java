package com.owieckowicz.chat_gpt_clone.features.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owieckowicz.chat_gpt_clone.features.conversation.ConversationRepository;
import org.springframework.stereotype.Service;

@Service
public class ConversationSettingsService {
    private static final int MIN_TOPK = 1;
    private static final int MAX_TOPK = 5;

    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    public ConversationSettingsService(ConversationRepository conversationRepository, ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.objectMapper = objectMapper;
    }

    public ConversationSettings load(Long conversationId) {
        Double temperature = null;
        String systemPrompt = null;
        boolean webAccessEnabled = false;
        int searchTopK = 3;
        boolean ragEnabled = false;
        int ragTopK = 5;
        try {
            var conv = conversationRepository.findById(conversationId).orElse(null);
            if (conv != null && conv.getSettings() != null && !conv.getSettings().isBlank()) {
                JsonNode node = objectMapper.readTree(conv.getSettings());
                if (node.hasNonNull("temperature")) temperature = node.get("temperature").asDouble();
                if (node.hasNonNull("systemPrompt")) systemPrompt = node.get("systemPrompt").asText();
                if (node.hasNonNull("webAccessEnabled")) webAccessEnabled = node.get("webAccessEnabled").asBoolean(false);
                if (node.hasNonNull("searchTopK")) searchTopK = node.get("searchTopK").asInt(3);
                if (node.hasNonNull("ragEnabled")) ragEnabled = node.get("ragEnabled").asBoolean(false);
                if (node.hasNonNull("ragTopK")) ragTopK = node.get("ragTopK").asInt(5);
            }
        } catch (Exception ignored) {
        }
        int clampedTopK = Math.max(MIN_TOPK, Math.min(MAX_TOPK, searchTopK <= 0 ? 3 : searchTopK));
        int clampedRagTopK = Math.max(1, Math.min(10, ragTopK <= 0 ? 5 : ragTopK));
        return new ConversationSettings(temperature, systemPrompt, webAccessEnabled, clampedTopK, ragEnabled, clampedRagTopK);
    }
}


