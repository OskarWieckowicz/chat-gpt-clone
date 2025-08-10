package com.owieckowicz.chat_gpt_clone.features.message;

import org.springframework.stereotype.Component;

@Component
public class MessageMapper {
    public MessageResponse toResponse(Message entity) {
        if (entity == null) return null;
        return new MessageResponse(
                entity.getId(),
                entity.getConversationId(),
                entity.getRole(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }
}


