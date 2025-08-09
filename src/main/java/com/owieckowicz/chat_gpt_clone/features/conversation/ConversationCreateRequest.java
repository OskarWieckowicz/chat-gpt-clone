package com.owieckowicz.chat_gpt_clone.features.conversation;

import jakarta.validation.constraints.Size;

/**
 * Request body for creating a conversation.
 * All fields are optional.
 */
public record ConversationCreateRequest(
        @Size(max = 200) String title,
        String settings
) {}


