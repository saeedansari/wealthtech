package com.nevis.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
@Slf4j
public class SummarizationService {
    private final WebClient webClient;
    private final String llmModel;

    public SummarizationService(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.llm-model}") String llmModel) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.llmModel = llmModel;
    }

    public String summarize(String content) {
        log.debug("Generating summary for content of length {}", content.length());

        String prompt = "Summarize the following document in 2-3 concise sentences. Focus on what document represents:\n\n" + content;

        Map<String, Object> request = Map.of(
                "model", llmModel,
                "prompt", prompt,
                "stream", false
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("response")) {
            log.info("Failed to get summary from Ollama");
            return null;
        }

        String summary = (String) response.get("response");
        log.info("Generated summary of length {}", summary.length());
        return summary.trim();
    }
}
