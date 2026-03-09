package com.nevis.service;

import com.nevis.dto.DocumentRequest;
import com.nevis.entity.Document;
import com.nevis.exception.ResourceNotFoundException;
import com.nevis.repository.ClientRepository;
import com.nevis.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final EmbeddingService embeddingService;
    private final DocumentSummarizationTask documentSummarizationTask;

    @Transactional
    public Document createDocument(UUID clientId, DocumentRequest request) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found with id: " + clientId);
        }

        Document document = new Document();
        document.setClientId(clientId);
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        float[] embedding = generateEmbedding(request);
        if (embedding != null) {
            document.setContentVector(embeddingService.toVectorString(embedding));
        }
        Document saved = documentRepository.save(document);
        documentSummarizationTask.summarizeAndUpdate(saved.getId(), request.getContent());
        return saved;
    }

    private float[] generateEmbedding(DocumentRequest request) {
        String textToEmbed = request.getTitle() + "\n\n" + request.getContent();
        try {
            return embeddingService.embed(textToEmbed);
        } catch (Exception e) {
            log.warn("Failed to generate embedding for document: {}", e.getMessage());
            return null;
        }
    }

}
