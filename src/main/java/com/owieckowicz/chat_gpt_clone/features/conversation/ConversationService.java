package com.owieckowicz.chat_gpt_clone.features.conversation;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConversationService {
    private final ConversationRepository repository;
    private final ConversationMapper mapper;

    public ConversationService(ConversationRepository repository, ConversationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public ConversationResponse create(ConversationCreateRequest req) {
        Conversation c = new Conversation();
        if (req != null) {
            if (req.title() != null && !req.title().isBlank()) c.setTitle(req.title());
            if (req.settings() != null) c.setSettings(req.settings());
        }
        return mapper.toResponse(repository.save(c));
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse get(Long id) {
        return repository.findById(id).map(mapper::toResponse).orElseThrow();
    }

    public ConversationResponse update(Long id, ConversationUpdateRequest req) {
        Conversation c = repository.findById(id).orElseThrow();
        if (req.title() != null && !req.title().isBlank()) c.setTitle(req.title());
        if (req.settings() != null) c.setSettings(req.settings());
        return mapper.toResponse(repository.save(c));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}


