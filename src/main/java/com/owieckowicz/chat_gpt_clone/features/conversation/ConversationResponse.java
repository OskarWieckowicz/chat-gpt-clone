package com.owieckowicz.chat_gpt_clone.features.conversation;

import java.time.OffsetDateTime;

public record ConversationResponse(
        Long id,
        String title,
        String settings,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}


