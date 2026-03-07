package com.nevis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);

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

        String prompt = "Summarize the following document in 2-3 concise sentences:\n\n" + content;

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
            log.warn("Failed to get summary from Ollama");
            return null;
        }

        String summary = (String) response.get("response");
        log.debug("Generated summary of length {}", summary.length());
        return summary.trim();
    }
}
