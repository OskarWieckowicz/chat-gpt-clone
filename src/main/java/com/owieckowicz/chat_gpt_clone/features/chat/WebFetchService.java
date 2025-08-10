package com.owieckowicz.chat_gpt_clone.features.chat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class WebFetchService {
    public String fetchText(String url, int maxChars) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (chat-gpt-clone)")
                .timeout(8000)
                .followRedirects(true)
                .get();
        String text = doc.text().replaceAll("\\s+", " ").trim();
        if (maxChars > 0 && text.length() > maxChars) {
            return text.substring(0, maxChars);
        }
        return text;
    }
}


