package com.owieckowicz.chat_gpt_clone.features.chat;

/**
 * Immutable settings for a conversation.
 */
public record ConversationSettings(
        Double temperature,
        String systemPrompt,
        boolean webAccessEnabled,
        int searchTopK,
        boolean ragEnabled,
        int ragTopK
) {}


