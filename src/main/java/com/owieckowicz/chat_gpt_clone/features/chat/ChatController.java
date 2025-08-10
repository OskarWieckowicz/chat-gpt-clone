package com.owieckowicz.chat_gpt_clone.features.chat;

import com.owieckowicz.chat_gpt_clone.features.message.MessageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody @jakarta.validation.Valid MessageRequest userMsg) {
        return chatService.chat(userMsg.message());
    }

    @PostMapping(value = "/{conversationId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatInConversation(@PathVariable Long conversationId, @RequestBody @jakarta.validation.Valid MessageRequest userMsg) {
        return chatService.chatInConversation(conversationId, userMsg.message());
    }
}
