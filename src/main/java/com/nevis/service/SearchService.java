package com.nevis.service;

import com.nevis.dto.ClientResponse;
import com.nevis.dto.DocumentResponse;
import com.nevis.dto.SearchResponse;
import com.nevis.repository.ClientRepository;
import com.nevis.repository.DocumentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingService embeddingService;

    @Value("${search.default-limit}")
    private final Integer defaultLimit;
    @Value("${search.minimum-score}")
    private final double minimumScore;

    public SearchService(ClientRepository clientRepository,
                         DocumentRepository documentRepository,
                         EmbeddingService embeddingService,
                         @Value("${search.default-limit}") int defaultLimit,
                         @Value("${search.minimum-score}") double minimumScore) {
        this.clientRepository = clientRepository;
        this.documentRepository = documentRepository;
        this.embeddingService = embeddingService;
        this.defaultLimit = defaultLimit;
        this.minimumScore = minimumScore;
    }

    public SearchResponse search(String query) {
        List<ClientResponse> clients = searchClients(query);
        List<DocumentResponse> documents = searchDocuments(query);
        return new SearchResponse(clients, documents);
    }

    private List<ClientResponse> searchClients(String query) {
        List<Object[]> results = clientRepository.searchClients(query, defaultLimit);
        List<ClientResponse> responses = new ArrayList<>();

        for (Object[] row : results) {
            ClientResponse response = new ClientResponse();
            response.setId((UUID) row[0]);
            response.setFirstName((String) row[1]);
            response.setLastName((String) row[2]);
            response.setEmail((String) row[3]);
            response.setDescription((String) row[4]);
            if (row[5] != null) {
                String[] links = (String[]) row[5];
                response.setSocialLinks(Arrays.asList(links));
            }
            if (row[6] instanceof LocalDateTime ldt) {
                response.setCreatedAt(ldt);
            }
            if (row[7] instanceof LocalDateTime ldt) {
                response.setUpdatedAt(ldt);
            }
            responses.add(response);
        }

        return responses;
    }

    private List<DocumentResponse> searchDocuments(String query) {
        List<DocumentResponse> responses = new ArrayList<>();

        try {
            float[] queryEmbedding = embeddingService.embed(query);
            String queryVectorString = embeddingService.toVectorString(queryEmbedding);

            List<Object[]> results = documentRepository.findBySemanticSimilarity(queryVectorString, defaultLimit);

            if (!results.isEmpty()) {
                Object[] topResult = results.get(0);
                //check top score
                Double topScore = toDouble(topResult[topResult.length - 1]);
                //return empty list if topScore is less than accepted minimumScore
                if (topScore != null && topScore < minimumScore) {
                    log.info("Top Score {} is less than minimum accepted score, returning empty", topScore);
                    return Collections.emptyList();
                }
            }

            int rowNumber = 0;
            for (Object[] row : results) {

                DocumentResponse response = new DocumentResponse();
                response.setId((UUID) row[0]);
                response.setClientId((UUID) row[1]);
                response.setTitle((String) row[2]);
                response.setContent((String) row[3]);
                // row[4] = content_vector (skip)
                response.setSummary((String) row[5]);
                if (row[6] instanceof LocalDateTime ldt) {
                    response.setCreatedAt(ldt);
                }
                // score is the last column
                Double score = toDouble(row[row.length - 1]);
                log.info("Row {} Score {}", rowNumber++, score);
                response.setScore(score);
                responses.add(response);
            }
        } catch (Exception e) {
            log.warn("Semantic search failed, returning empty document results: {}", e.getMessage());
        }

        return responses;
    }

    private Double toDouble(Object value) {
        if (value instanceof Double d) return d;
        if (value instanceof Float f) return f.doubleValue();
        if (value instanceof BigDecimal bd) return bd.doubleValue();
        if (value instanceof Number n) return n.doubleValue();
        return null;
    }
}
