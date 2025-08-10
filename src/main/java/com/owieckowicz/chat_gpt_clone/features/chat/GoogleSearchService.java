package com.owieckowicz.chat_gpt_clone.features.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GoogleSearchService {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http;
    private final String apiKey;
    private final String cx;
    private final long timeoutMs;

    public GoogleSearchService(
            @Value("${google.cse.api-key:}") String apiKey,
            @Value("${google.cse.cx:}") String cx,
            @Value("${google.cse.timeout-ms:8000}") long timeoutMs
    ) {
        this.apiKey = apiKey;
        this.cx = cx;
        this.timeoutMs = timeoutMs;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && cx != null && !cx.isBlank();
    }

    public List<WebSnippet> search(String query, int topK) {

        if (!isConfigured()) return List.of();
        int num = Math.max(1, Math.min(10, topK));

        try {
            String url = "https://www.googleapis.com/customsearch/v1"
                    + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&cx=" + URLEncoder.encode(cx, StandardCharsets.UTF_8)
                    + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&num=" + num;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();
            log.info("Searching in google browser");
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return List.of();
            JsonNode res = MAPPER.readTree(resp.body());
            if (res == null || !res.has("items") || res.get("items").isEmpty()) return List.of();

            List<WebSnippet> out = new ArrayList<>();
            for (JsonNode item : res.get("items")) {
                String title = item.path("title").asText("");
                String link = item.path("link").asText("");
                String snippet = item.path("snippet").asText("");
                if (link == null || link.isBlank()) continue;
                out.add(new WebSnippet(title, link, snippet));
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }
}


