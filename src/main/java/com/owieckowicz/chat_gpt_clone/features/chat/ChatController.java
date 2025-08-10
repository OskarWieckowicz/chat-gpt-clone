package com.owieckowicz.chat_gpt_clone.features.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owieckowicz.chat_gpt_clone.features.conversation.Conversation;
import com.owieckowicz.chat_gpt_clone.features.conversation.ConversationRepository;
import com.owieckowicz.chat_gpt_clone.features.message.Message;
import com.owieckowicz.chat_gpt_clone.features.message.MessageRepository;
import com.owieckowicz.chat_gpt_clone.features.message.MessageRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatClient chat;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    public ChatController(ChatClient.Builder builder,
                          ChatMemory chatMemory,
                          MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          ObjectMapper objectMapper) {
        this.chat = builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                ).build();
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
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
        var spec = chat.prompt();

        // Load per-conversation settings: model, temperature, systemPrompt
        Conversation conv = conversationRepository.findById(conversationId).orElse(null);
        Double temperature = null;
        String systemPrompt = null;
        try {
            if (conv != null && conv.getSettings() != null && !conv.getSettings().isBlank()) {
                JsonNode n = objectMapper.readTree(conv.getSettings());
                if (n.hasNonNull("temperature")) temperature = n.get("temperature").asDouble();
                if (n.hasNonNull("systemPrompt")) systemPrompt = n.get("systemPrompt").asText();
            }
        } catch (Exception ignored) {
            // fallback to defaults if parsing fails
        }

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            spec = spec.system(systemPrompt);
        }

        var optionsBuilder = OpenAiChatOptions.builder();
        boolean hasOptions = false;
        if (temperature != null) {
            optionsBuilder.temperature(temperature);
            hasOptions = true;
        }

        spec = spec.user(userMsg.message());
        if (hasOptions) {
            spec = spec.options(optionsBuilder.build());
        }

        return spec
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
