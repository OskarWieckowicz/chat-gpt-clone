package com.owieckowicz.chat_gpt_clone.features.chat;

import com.owieckowicz.chat_gpt_clone.features.message.MessageRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.multipart.MultipartFile;
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

    @PostMapping(value = "/{conversationId}/messages/multimodal",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatInConversationMultimodal(
            @PathVariable Long conversationId,
            @RequestPart("message") String message,
            @RequestPart(value = "images", required = false) MultipartFile[] images
    ) throws java.io.IOException {
        ByteArrayResource[] resources = null;
        MediaType[] mimeTypes = null;
        if (images != null && images.length > 0) {
            resources = new ByteArrayResource[images.length];
            mimeTypes = new MediaType[images.length];
            for (int i = 0; i < images.length; i++) {
                var f = images[i];
                resources[i] = new ByteArrayResource(f.getBytes());
                var mt = f.getContentType();
                if (mt != null) {
                    mimeTypes[i] = MediaType.parseMediaType(mt);
                } else {
                    mimeTypes[i] = MediaTypeFactory.getMediaType(f.getOriginalFilename()).orElse(MediaType.IMAGE_JPEG);
                }
            }
        }
        return chatService
                .chatInConversationMultimodal(conversationId, message, resources, mimeTypes)
                .onErrorResume(err -> reactor.core.publisher.Flux.empty());
    }
}
