package com.owieckowicz.chat_gpt_clone.features.message;

import java.time.OffsetDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        String role,
        String content,
        OffsetDateTime createdAt
) {}


