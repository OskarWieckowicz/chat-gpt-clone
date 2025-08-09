package com.owieckowicz.chat_gpt_clone.features.message;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MessageService {
    private final MessageRepository repository;
    private final MessageMapper mapper;

    public MessageService(MessageRepository repository, MessageMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<MessageResponse> listByConversation(Long conversationId) {
        return repository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream().map(mapper::toResponse).toList();
    }
}


