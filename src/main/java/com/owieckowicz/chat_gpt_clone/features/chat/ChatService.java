package com.owieckowicz.chat_gpt_clone.features.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owieckowicz.chat_gpt_clone.features.message.Message;
import com.owieckowicz.chat_gpt_clone.features.message.MessageRepository;
import com.owieckowicz.chat_gpt_clone.features.tools.DateTimeTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Service
public class ChatService {
    // retained for backward compatibility of constants if needed elsewhere

    private final ChatClient chatClient;
    private final MessageRepository messageRepository;
    private final VectorStore vectorStore;
    private final ConversationSettingsService settingsService;
    private final WebContextBuilder webContextBuilder;

    public ChatService(ChatClient.Builder builder,
                       ChatMemory chatMemory,
                        MessageRepository messageRepository,
                         ObjectMapper objectMapper,
                        GoogleSearchService googleSearchService,
                        WebFetchService webFetchService,
                        VectorStore vectorStore,
                        ConversationSettingsService settingsService,
                        WebContextBuilder webContextBuilder) {
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.messageRepository = messageRepository;
        this.vectorStore = vectorStore;
        this.settingsService = settingsService;
        this.webContextBuilder = webContextBuilder;
    }

    public Flux<String> chat(String userMessage) {
        return chatClient
                .prompt()
                .user(userMessage)
                .stream()
                .content();
    }

    public Flux<String> chatInConversation(Long conversationId, String userMessage) {
        persistUserMessage(conversationId, userMessage);

        var spec = chatClient.prompt();

        ConversationSettings settings = settingsService.load(conversationId);

        String webContext = webContextBuilder.build(userMessage, settings.searchTopK(), settings.webAccessEnabled(), chatClient);
        boolean hasSystem = settings.systemPrompt() != null && !settings.systemPrompt().isBlank();
        boolean hasWeb = webContext != null && !webContext.isBlank();
        if (hasSystem && hasWeb) {
            spec = spec.system(settings.systemPrompt() + "\n\n" + webContext);
        } else if (hasSystem) {
            spec = spec.system(settings.systemPrompt());
        } else if (hasWeb) {
            spec = spec.system(webContext);
        }

        // Attach RAG advisor if enabled or if auto-detect finds uploaded docs for this conversation
        boolean hasDocs = hasAnyUploadedDocs(conversationId);
        boolean useRag = settings.ragEnabled() || hasDocs;
        if (useRag) {
            var search = SearchRequest.builder()
                    .topK(settings.ragTopK())
                    .filterExpression("conversationId == '" + conversationId + "'")
                    .build();
            var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(search)
                    .build();
            spec = spec.advisors(qaAdvisor);
        }

        var optionsBuilder = OpenAiChatOptions.builder();
        boolean hasOptions = false;
        if (settings.temperature() != null) {
            optionsBuilder.temperature(settings.temperature());
            hasOptions = true;
        }

        spec = spec.user(userMessage);
        if (hasOptions) {
            spec = spec.options(optionsBuilder.build());
        }

        StringBuilder assistantBuffer = new StringBuilder();
        return spec
                .tools(new DateTimeTool())
                .stream()
                .content()
                .doOnNext(assistantBuffer::append)
                .doOnError(err -> persistAssistantError(conversationId, err))
                .onErrorResume(err -> Flux.empty())
                .doOnComplete(() -> persistAssistantCompletion(conversationId, assistantBuffer.toString()));
    }

    private boolean hasAnyUploadedDocs(Long conversationId) {
        try {
            Path dir = Path.of("uploads", String.valueOf(conversationId));
            if (!Files.isDirectory(dir)) return false;
            try (Stream<Path> paths = Files.list(dir)) {
                return paths.anyMatch(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".pdf");
                });
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private void persistUserMessage(Long conversationId, String userMessage) {
        Message user = new Message();
        user.setConversationId(conversationId);
        user.setRole("user");
        user.setContent(userMessage);
        messageRepository.save(user);
    }

    private void persistAssistantCompletion(Long conversationId, String content) {
        Message assistant = new Message();
        assistant.setConversationId(conversationId);
        assistant.setRole("assistant");
        assistant.setContent(content);
        messageRepository.save(assistant);
    }

    private void persistAssistantError(Long conversationId, Throwable err) {
        Message failed = new Message();
        failed.setConversationId(conversationId);
        failed.setRole("assistant");
        failed.setContent("[ERROR] " + (err != null ? err.getMessage() : "unknown error"));
        messageRepository.save(failed);
    }


    // moved to SearchQueryService

}


