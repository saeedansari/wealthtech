package com.nevis.service;

import com.nevis.dto.DocumentRequest;
import com.nevis.entity.Document;
import com.nevis.exception.ResourceNotFoundException;
import com.nevis.repository.ClientRepository;
import com.nevis.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private SummarizationService summarizationService;

    @InjectMocks
    private DocumentService documentService;

    private DocumentRequest createRequest(String title, String content) {
        DocumentRequest request = new DocumentRequest();
        request.setTitle(title);
        request.setContent(content);
        return request;
    }

    private void stubClientExists(UUID clientId) {
        when(clientRepository.existsById(clientId)).thenReturn(true);
    }

    private void stubSave() {
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
    }

    @Test
    void createDocument_withNonexistentClient_throwsNotFoundException() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.existsById(clientId)).thenReturn(false);

        DocumentRequest request = createRequest("Some Doc", "Some content");

        assertThrows(ResourceNotFoundException.class,
                () -> documentService.createDocument(clientId, request));

        verify(documentRepository, never()).save(any());
    }

    @Test
    void createDocument_whenSummarizationSucceeds_embedsSummary() {
        UUID clientId = UUID.randomUUID();
        stubClientExists(clientId);
        stubSave();

        float[] summaryEmbedding = new float[]{0.5f, 0.6f};
        when(summarizationService.summarize("Electric bill content")).thenReturn("Summary of bill");
        when(embeddingService.embed("Summary of bill")).thenReturn(summaryEmbedding);
        when(embeddingService.toVectorString(summaryEmbedding)).thenReturn("[0.5,0.6]");

        DocumentRequest request = createRequest("Utility Bill", "Electric bill content");
        Document result = documentService.createDocument(clientId, request);

        assertEquals("[0.5,0.6]", result.getContentVector());
        verify(embeddingService).embed("Summary of bill");
        verify(embeddingService, never()).embed("Utility Bill\n\nElectric bill content");
    }

    @Test
    void createDocument_whenSummarizationReturnsNull_embedsOriginalDocument() {
        UUID clientId = UUID.randomUUID();
        stubClientExists(clientId);
        stubSave();

        float[] fallbackEmbedding = new float[]{0.1f, 0.2f};
        when(summarizationService.summarize(anyString())).thenReturn(null);
        when(embeddingService.embed("Title\n\nContent")).thenReturn(fallbackEmbedding);
        when(embeddingService.toVectorString(fallbackEmbedding)).thenReturn("[0.1,0.2]");

        DocumentRequest request = createRequest("Title", "Content");
        Document result = documentService.createDocument(clientId, request);

        assertEquals("[0.1,0.2]", result.getContentVector());
        verify(embeddingService).embed("Title\n\nContent");
    }


    @Test
    void createDocument_whenSummarizationFails_embedsOriginalDocument() {
        UUID clientId = UUID.randomUUID();
        stubClientExists(clientId);
        stubSave();

        float[] fallbackEmbedding = new float[]{0.1f, 0.2f};
        when(summarizationService.summarize(anyString()))
                .thenThrow(new RuntimeException("Ollama summarization unavailable"));
        when(embeddingService.embed("My Title\n\nMy Content")).thenReturn(fallbackEmbedding);
        when(embeddingService.toVectorString(fallbackEmbedding)).thenReturn("[0.1,0.2]");

        DocumentRequest request = createRequest("My Title", "My Content");
        Document result = documentService.createDocument(clientId, request);

        assertEquals("[0.1,0.2]", result.getContentVector());
        verify(embeddingService).embed("My Title\n\nMy Content");
    }


    @Test
    void createDocument_whenSummarizationAndEmbeddingBothFail_savesWithoutVector() {
        UUID clientId = UUID.randomUUID();
        stubClientExists(clientId);
        stubSave();

        when(summarizationService.summarize(anyString()))
                .thenThrow(new RuntimeException("Summarization failed"));
        when(embeddingService.embed(anyString()))
                .thenThrow(new RuntimeException("Embedding failed"));

        DocumentRequest request = createRequest("Title", "Content");
        Document result = documentService.createDocument(clientId, request);

        assertNotNull(result.getId());
        assertNull(result.getContentVector());
        verify(documentRepository).save(any(Document.class));
    }


    @Test
    void createDocument_whenSummarizationSucceedsButEmbeddingFails_savesWithoutVector() {
        UUID clientId = UUID.randomUUID();
        stubClientExists(clientId);
        stubSave();

        when(summarizationService.summarize("Content")).thenReturn("A summary");
        when(embeddingService.embed("A summary"))
                .thenThrow(new RuntimeException("Embedding failed"));

        DocumentRequest request = createRequest("Title", "Content");
        Document result = documentService.createDocument(clientId, request);

        assertNull(result.getContentVector());
        // embed is called once with the summary; no redundant retry with original text
        verify(embeddingService).embed("A summary");
        verify(embeddingService, never()).embed("Title\n\nContent");
    }


    @Test
    void createDocument_setsFieldsCorrectly() {
        UUID clientId = UUID.randomUUID();
        stubClientExists(clientId);
        stubSave();

        when(summarizationService.summarize(anyString())).thenReturn("Summary");
        when(embeddingService.embed("Summary")).thenReturn(new float[]{0.1f});
        when(embeddingService.toVectorString(any())).thenReturn("[0.1]");

        DocumentRequest request = createRequest("Utility Bill", "Electric bill content");
        Document result = documentService.createDocument(clientId, request);

        assertNotNull(result.getId());
        assertEquals(clientId, result.getClientId());
        assertEquals("Utility Bill", result.getTitle());
        assertEquals("Electric bill content", result.getContent());
        verify(documentRepository).save(any(Document.class));
    }
}
