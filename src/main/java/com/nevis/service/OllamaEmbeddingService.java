package com.nevis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Service
public class OllamaEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);

    private final WebClient webClient;
    private final String embeddingModel;

    public OllamaEmbeddingService(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.embedding-model}") String embeddingModel) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] embed(String text) {
        log.debug("Generating embedding for text of length {}", text.length());

        Map<String, Object> request = Map.of(
                "model", embeddingModel,
                "prompt", text
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClient.post()
                .uri("/api/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("embedding")) {
            throw new RuntimeException("Failed to get embedding from Ollama");
        }

        @SuppressWarnings("unchecked")
        List<Number> embeddingList = (List<Number>) response.get("embedding");
        float[] embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i).floatValue();
        }

        log.debug("Generated embedding with {} dimensions", embedding.length);
        return embedding;
    }
}
