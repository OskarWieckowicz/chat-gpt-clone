package com.owieckowicz.chat_gpt_clone.features.conversation;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConversationMapper {
    ConversationResponse toResponse(Conversation entity);
}


