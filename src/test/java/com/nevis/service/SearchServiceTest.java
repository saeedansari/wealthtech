package com.nevis.service;

import com.nevis.dto.SearchResponse;
import com.nevis.repository.ClientRepository;
import com.nevis.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private EmbeddingService embeddingService;

private SearchService createService(int defaultLimit, double minimumScore) {
        return new SearchService(clientRepository, documentRepository,
                embeddingService, defaultLimit, minimumScore);
    }

    private void stubEmbedding() {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(embeddingService.toVectorString(any())).thenReturn("[0.1,0.2]");
    }

    private void stubEmptyClients() {
        when(clientRepository.searchClients(anyString(), anyInt()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void searchDocuments_whenTopScoreAboveMinimum_returnsDocuments() {
        SearchService service = createService(5, 0.65);
        UUID docId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // score = 1 - distance; score 0.80 means distance 0.20
        Object[] row = new Object[]{docId, clientId, "Bank Statement", "Content here",
                null, "A summary", now, 0.80};

        stubEmbedding();
        when(documentRepository.findBySemanticSimilarity(anyString(), anyInt()))
                .thenReturn(Collections.singletonList(row));
        stubEmptyClients();

        SearchResponse response = service.search("financial statement");

        assertEquals(1, response.getDocuments().size());
        assertEquals("Bank Statement", response.getDocuments().get(0).getTitle());
        assertEquals(0.80, response.getDocuments().get(0).getScore());
    }

    @Test
    void searchDocuments_whenTopScoreBelowMinimum_returnsEmptyList() {
        SearchService service = createService(5, 0.65);
        UUID docId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // score 0.30 is below minimumScore 0.65
        Object[] row = new Object[]{docId, clientId, "Irrelevant Doc", "Some text",
                null, null, now, 0.30};

        stubEmbedding();
        when(documentRepository.findBySemanticSimilarity(anyString(), anyInt()))
                .thenReturn(Collections.singletonList(row));
        stubEmptyClients();

        SearchResponse response = service.search("random query");

        assertTrue(response.getDocuments().isEmpty());
    }

    @Test
    void searchDocuments_whenTopScoreExactlyAtMinimum_returnsDocuments() {
        SearchService service = createService(5, 0.65);
        UUID docId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // score exactly at the threshold — should NOT be filtered out
        Object[] row = new Object[]{docId, clientId, "Threshold Doc", "Content",
                null, null, now, 0.65};

        stubEmbedding();
        when(documentRepository.findBySemanticSimilarity(anyString(), anyInt()))
                .thenReturn(Collections.singletonList(row));
        stubEmptyClients();

        SearchResponse response = service.search("threshold query");

        assertEquals(1, response.getDocuments().size());
    }

    @Test
    void searchDocuments_whenMultipleResults_onlyChecksTopScore() {
        SearchService service = createService(5, 0.65);
        UUID clientId = UUID.randomUUID();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // First result (top) has score above minimum, second has score below
        Object[] row1 = new Object[]{UUID.randomUUID(), clientId, "Best Match", "Content",
                null, null, now, 0.90};
        Object[] row2 = new Object[]{UUID.randomUUID(), clientId, "Weak Match", "Content",
                null, null, now, 0.40};

        stubEmbedding();
        when(documentRepository.findBySemanticSimilarity(anyString(), anyInt()))
                .thenReturn(List.of(row1, row2));
        stubEmptyClients();

        SearchResponse response = service.search("some query");

        // Both returned because the top score passes the threshold
        assertEquals(2, response.getDocuments().size());
    }

    @Test
    void searchDocuments_whenNoResults_returnsEmptyList() {
        SearchService service = createService(5, 0.65);

        stubEmbedding();
        when(documentRepository.findBySemanticSimilarity(anyString(), anyInt()))
                .thenReturn(Collections.emptyList());
        stubEmptyClients();

        SearchResponse response = service.search("nothing matches");

        assertTrue(response.getDocuments().isEmpty());
    }

    @Test
    void searchDocuments_whenEmbeddingFails_returnsEmptyList() {
        SearchService service = createService(5, 0.65);

        when(embeddingService.embed(anyString())).thenThrow(new RuntimeException("Ollama down"));
        stubEmptyClients();

        SearchResponse response = service.search("will fail");

        assertTrue(response.getDocuments().isEmpty());
    }

    @Test
    void searchDocuments_whenSummaryNull_returnsNullSummary() {
        SearchService service = createService(5, 0.65);
        UUID docId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // summary (row[5]) is null — SearchService no longer generates on demand
        Object[] row = new Object[]{docId, clientId, "No Summary Doc", "Document content here",
                null, null, now, 0.80};

        stubEmbedding();
        when(documentRepository.findBySemanticSimilarity(anyString(), anyInt()))
                .thenReturn(Collections.singletonList(row));
        stubEmptyClients();

        SearchResponse response = service.search("some query");

        assertEquals(1, response.getDocuments().size());
        assertNull(response.getDocuments().get(0).getSummary());
    }

    @Test
    void searchDocuments_whenSummaryExists_returnsSummaryAsIs() {
        SearchService service = createService(5, 0.65);
        UUID docId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // summary (row[5]) already present in DB
        Object[] row = new Object[]{docId, clientId, "Has Summary", "Content",
                null, "Existing summary", now, 0.80};

        stubEmbedding();
        when(documentRepository.findBySemanticSimilarity(anyString(), anyInt()))
                .thenReturn(Collections.singletonList(row));
        stubEmptyClients();

        SearchResponse response = service.search("some query");

        assertEquals("Existing summary", response.getDocuments().get(0).getSummary());
    }
}
