package com.owieckowicz.chat_gpt_clone.features.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owieckowicz.chat_gpt_clone.features.conversation.Conversation;
import com.owieckowicz.chat_gpt_clone.features.conversation.ConversationRepository;
import com.owieckowicz.chat_gpt_clone.features.message.Message;
import com.owieckowicz.chat_gpt_clone.features.message.MessageRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {
    private static final int MAX_TOTAL_WEB_CONTEXT_CHARS = 8_000;
    private static final int MAX_PER_DOC_CHARS = 3_000;
    private static final int MIN_TOPK = 1;
    private static final int MAX_TOPK = 5;

    private final ChatClient chatClient;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;
    private final GoogleSearchService googleSearchService;
    private final WebFetchService webFetchService;

    public ChatService(ChatClient.Builder builder,
                       ChatMemory chatMemory,
                       MessageRepository messageRepository,
                       ConversationRepository conversationRepository,
                       ObjectMapper objectMapper,
                       GoogleSearchService googleSearchService,
                       WebFetchService webFetchService) {
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.objectMapper = objectMapper;
        this.googleSearchService = googleSearchService;
        this.webFetchService = webFetchService;
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

        ConversationSettings settings = loadSettings(conversationId);

        String webContext = buildWebContextIfEnabled(userMessage, settings);
        boolean hasSystem = settings.systemPrompt() != null && !settings.systemPrompt().isBlank();
        boolean hasWeb = webContext != null && !webContext.isBlank();
        if (hasSystem && hasWeb) {
            spec = spec.system(settings.systemPrompt() + "\n\n" + webContext);
        } else if (hasSystem) {
            spec = spec.system(settings.systemPrompt());
        } else if (hasWeb) {
            spec = spec.system(webContext);
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
                .stream()
                .content()
                .doOnNext(assistantBuffer::append)
                .doOnError(err -> persistAssistantError(conversationId, err))
                .doOnComplete(() -> persistAssistantCompletion(conversationId, assistantBuffer.toString()));
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

    private record ConversationSettings(Double temperature, String systemPrompt, boolean webAccessEnabled, int searchTopK) {}

    private ConversationSettings loadSettings(Long conversationId) {
        Double temperature = null;
        String systemPrompt = null;
        boolean webAccessEnabled = false;
        int searchTopK = 3;
        try {
            Conversation conv = conversationRepository.findById(conversationId).orElse(null);
            if (conv != null && conv.getSettings() != null && !conv.getSettings().isBlank()) {
                JsonNode node = objectMapper.readTree(conv.getSettings());
                if (node.hasNonNull("temperature")) temperature = node.get("temperature").asDouble();
                if (node.hasNonNull("systemPrompt")) systemPrompt = node.get("systemPrompt").asText();
                if (node.hasNonNull("webAccessEnabled")) webAccessEnabled = node.get("webAccessEnabled").asBoolean(false);
                if (node.hasNonNull("searchTopK")) searchTopK = node.get("searchTopK").asInt(3);
            }
        } catch (Exception ignored) {
        }
        int clampedTopK = Math.max(MIN_TOPK, Math.min(MAX_TOPK, searchTopK <= 0 ? 3 : searchTopK));
        return new ConversationSettings(temperature, systemPrompt, webAccessEnabled, clampedTopK);
    }

    private String buildWebContextIfEnabled(String userMessage, ConversationSettings settings) {
        if (!settings.webAccessEnabled()) return null;
        // Let the model craft a concise web search query from the user message
        String query = craftWebSearchQuery(userMessage);
        if (query == null || query.isBlank()) return null;

        var hits = googleSearchService.search(query, settings.searchTopK());
        if (hits.isEmpty()) return null;

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("You can use the following web context. Cite sources inline as [n] and end with a 'Sources' section listing the referenced URLs.\n");

        int totalChars = 0;
        int index = 1;
        for (var hit : hits) {
            String url = hit.url();
            if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) continue;
            String text;
            try {
                text = webFetchService.fetchText(url, MAX_PER_DOC_CHARS);
            } catch (Exception e) {
                continue;
            }
            if (text == null || text.isBlank()) continue;

            String header = "[" + (index++) + "] " + url + ": ";
            if (totalChars + header.length() + text.length() > MAX_TOTAL_WEB_CONTEXT_CHARS) break;
            contextBuilder.append(header).append(text).append('\n');
            totalChars += header.length() + text.length();
        }

        return totalChars > 0 ? contextBuilder.toString() : null;
    }

    private String craftWebSearchQuery(String userMessage) {
        try {
            String instruction = "You write concise web search queries.\n"
                    + "Constraints:\n"
                    + "- 3 to 10 words.\n"
                    + "- No quotes or punctuation at ends.\n"
                    + "- Avoid code, stopwords, and filler.\n"
                    + "- Output ONLY the query string, nothing else.";
            String raw = chatClient
                    .prompt()
                    .system(instruction)
                    .user("Question: " + userMessage)
                    .call()
                    .content();
            if (raw == null) return null;
            String trimmed = raw.trim()
                    .replaceAll("\r", " ")
                    .replaceAll("\n", " ")
                    .replaceAll("[\u201C\u201D\"']", "")
                    .replaceAll("\s+", " ")
                    .trim();
            if (trimmed.length() > 160) trimmed = trimmed.substring(0, 160);
            // very short outputs are likely useless
            if (trimmed.length() < 3) return null;
            return trimmed;
        } catch (Exception e) {
            return null;
        }
    }

}


