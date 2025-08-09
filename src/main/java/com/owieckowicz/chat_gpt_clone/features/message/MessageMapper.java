package com.owieckowicz.chat_gpt_clone.features.message;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageMapper {
    MessageResponse toResponse(Message entity);
}


