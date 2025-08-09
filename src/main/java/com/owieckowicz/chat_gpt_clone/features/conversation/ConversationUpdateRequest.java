package com.owieckowicz.chat_gpt_clone.features.conversation;

import jakarta.validation.constraints.Size;

/**
 * Request body for updating a conversation (partial updates).
 */
public record ConversationUpdateRequest(
        @Size(max = 200) String title,
        String settings
) {}


