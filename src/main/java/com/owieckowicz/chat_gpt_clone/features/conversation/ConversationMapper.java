package com.owieckowicz.chat_gpt_clone.features.conversation;

import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {
    public ConversationResponse toResponse(Conversation entity) {
        if (entity == null) return null;
        return new ConversationResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSettings(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}


