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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void createDocument_withValidClient_savesDocument() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.existsById(clientId)).thenReturn(true);

        float[] fakeEmbedding = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingService.embed(any())).thenReturn(fakeEmbedding);
        when(embeddingService.toVectorString(fakeEmbedding)).thenReturn("[0.1,0.2,0.3]");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        DocumentRequest request = new DocumentRequest();
        request.setTitle("Utility Bill");
        request.setContent("Electric bill content");

        Document result = documentService.createDocument(clientId, request);

        assertNotNull(result.getId());
        assertEquals(clientId, result.getClientId());
        assertEquals("Utility Bill", result.getTitle());
        assertEquals("Electric bill content", result.getContent());
        assertEquals("[0.1,0.2,0.3]", result.getContentVector());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void createDocument_withNonexistentClient_throwsNotFoundException() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.existsById(clientId)).thenReturn(false);

        DocumentRequest request = new DocumentRequest();
        request.setTitle("Some Doc");
        request.setContent("Some content");

        assertThrows(ResourceNotFoundException.class,
                () -> documentService.createDocument(clientId, request));

        verify(documentRepository, never()).save(any());
    }

    @Test
    void createDocument_whenEmbeddingFails_savesWithoutVector() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(embeddingService.embed(any())).thenThrow(new RuntimeException("Ollama unavailable"));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        DocumentRequest request = new DocumentRequest();
        request.setTitle("Title");
        request.setContent("Content");

        Document result = documentService.createDocument(clientId, request);

        assertNotNull(result.getId());
        assertNull(result.getContentVector());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void createDocument_embedsConcatenatedTitleAndContent() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(embeddingService.embed(any())).thenReturn(new float[]{0.1f});
        when(embeddingService.toVectorString(any())).thenReturn("[0.1]");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentRequest request = new DocumentRequest();
        request.setTitle("My Title");
        request.setContent("My Content");

        documentService.createDocument(clientId, request);

        verify(embeddingService).embed("My Title\n\nMy Content");
    }
}
