package com.owieckowicz.chat_gpt_clone.features.chat;

import com.owieckowicz.chat_gpt_clone.features.message.Message;
import com.owieckowicz.chat_gpt_clone.features.message.MessageRepository;
import com.owieckowicz.chat_gpt_clone.features.message.MessageRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatClient chat;
    private final MessageRepository messageRepository;

    public ChatController(ChatClient.Builder builder, ChatMemory chatMemory, MessageRepository messageRepository) {
        this.chat = builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                ).build();
        this.messageRepository = messageRepository;
    }

    @PostMapping
    public Flux<String> chat(@RequestBody @jakarta.validation.Valid MessageRequest userMsg) {
        return chat.prompt()
                .user(userMsg.message())
                .stream()
                .content();
    }

    @PostMapping(value = "/{conversationId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatInConversation(@PathVariable Long conversationId, @RequestBody @jakarta.validation.Valid MessageRequest userMsg) {
        Message user = new Message();
        user.setConversationId(conversationId);
        user.setRole("user");
        user.setContent(userMsg.message());
        messageRepository.save(user);

        StringBuilder assistantBuffer = new StringBuilder();
        return chat.prompt()
                .user(userMsg.message())
                .stream()
                .content()
                .doOnNext(assistantBuffer::append)
                .doOnError(err -> {
                    Message failed = new Message();
                    failed.setConversationId(conversationId);
                    failed.setRole("assistant");
                    failed.setContent("[ERROR] " + err.getMessage());
                    messageRepository.save(failed);
                })
                .doOnComplete(() -> {
                    Message assistant = new Message();
                    assistant.setConversationId(conversationId);
                    assistant.setRole("assistant");
                    assistant.setContent(assistantBuffer.toString());
                    messageRepository.save(assistant);
                });
    }
}
