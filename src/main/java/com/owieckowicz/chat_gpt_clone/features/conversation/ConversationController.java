package com.owieckowicz.chat_gpt_clone.features.conversation;

import java.util.List;

import com.owieckowicz.chat_gpt_clone.features.message.MessageResponse;
import com.owieckowicz.chat_gpt_clone.features.message.MessageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationController(ConversationService conversationService,
                                  MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @PostMapping
    public ConversationResponse create(@RequestBody(required = false) @jakarta.validation.Valid ConversationCreateRequest body) {
        return conversationService.create(body);
    }

    @GetMapping
    public List<ConversationResponse> list() {
        return conversationService.list();
    }

    @GetMapping("/{id}")
    public ConversationResponse get(@PathVariable Long id) {
        return conversationService.get(id);
    }

    @PatchMapping("/{id}")
    public ConversationResponse update(@PathVariable Long id, @RequestBody @jakarta.validation.Valid ConversationUpdateRequest body) {
        return conversationService.update(id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        conversationService.delete(id);
    }

    @GetMapping(value = "/{id}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MessageResponse> listMessages(@PathVariable Long id) {
        return messageService.listByConversation(id);
    }
}


