package com.owieckowicz.chat_gpt_clone.features.rag.pdf;

import com.owieckowicz.chat_gpt_clone.features.rag.RagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations/{conversationId}/documents")
public class DocumentUploadController {

    private final RagService ragService;
    private final JdbcTemplate jdbcTemplate;

    public DocumentUploadController(RagService ragService, JdbcTemplate jdbcTemplate) {
        this.ragService = ragService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @PathVariable Long conversationId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(Map.of("error", "Only application/pdf supported"));
        }

        String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : ("upload-" + UUID.randomUUID());
        String idStr = UUID.randomUUID().toString();
        Long documentId = Math.abs(idStr.hashCode() * 1L);

        Path uploadDir = Path.of("uploads", String.valueOf(conversationId));
        Files.createDirectories(uploadDir);
        Path dest = uploadDir.resolve(idStr + ".pdf");
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        // Synchronous ingest for MVP
        ragService.ingestPdf(conversationId, documentId, dest, original);

        return ResponseEntity.accepted().body(Map.of(
                "documentId", documentId,
                "filename", original
        ));
    }

    @GetMapping
    public List<DocumentItem> list(@PathVariable Long conversationId) {
        String sql = "SELECT DISTINCT metadata->>'documentId' AS document_id, metadata->>'filename' AS filename " +
                "FROM public.vector_store WHERE metadata->>'conversationId' = ? ORDER BY filename";
        return jdbcTemplate.query(sql, new Object[]{String.valueOf(conversationId)}, (rs, rowNum) -> {
            String docIdStr = rs.getString("document_id");
            String filename = rs.getString("filename");
            Long docId = null;
            try {
                docId = Long.parseLong(docIdStr);
            } catch (Exception ignored) {
                // fallback if parsing fails
                docId = 0L;
            }
            return new DocumentItem(docId, filename);
        });
    }

    public record DocumentItem(Long documentId, String filename) {}
}


