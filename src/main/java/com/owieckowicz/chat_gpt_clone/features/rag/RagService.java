package com.owieckowicz.chat_gpt_clone.features.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class RagService {

    private final TokenTextSplitter splitter;
    private final VectorStore vectorStore;

    public RagService(TokenTextSplitter splitter, VectorStore vectorStore) {
        this.splitter = splitter;
        this.vectorStore = vectorStore;
    }

    public void ingestPdf(Long conversationId, Long documentId, Path file, String filename) {
        Resource res = new FileSystemResource(file);
        PagePdfDocumentReader reader = new PagePdfDocumentReader(res);

        List<Document> docs = reader.get();
        for (Document d : docs) {
            d.getMetadata().put("conversationId", String.valueOf(conversationId));
            d.getMetadata().put("documentId", String.valueOf(documentId));
            if (filename != null) d.getMetadata().put("filename", filename);
        }

        List<Document> chunks = splitter.apply(docs);

        final int batchSize = 4;
        for (int i = 0; i < chunks.size(); i += batchSize) {
            List<Document> batch = chunks.subList(i, Math.min(i + batchSize, chunks.size()));
            vectorStore.add(batch);
        }
    }
}


