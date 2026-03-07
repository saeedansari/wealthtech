package com.nevis.service;

import com.nevis.dto.DocumentRequest;
import com.nevis.entity.Document;
import com.nevis.exception.ResourceNotFoundException;
import com.nevis.repository.ClientRepository;
import com.nevis.repository.DocumentRepository;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final EmbeddingService embeddingService;

    public DocumentService(DocumentRepository documentRepository,
                           ClientRepository clientRepository,
                           EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public Document createDocument(UUID clientId, DocumentRequest request) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found with id: " + clientId);
        }

        Document document = new Document();
        document.setClientId(clientId);
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());

        try {
            String textToEmbed = request.getTitle() + "\n\n" + request.getContent();
            float[] embedding = embeddingService.embed(textToEmbed);

            document.setContentVector(embeddingService.toVectorString(embedding));
        } catch (Exception e) {
            log.warn("Failed to generate embedding for document, saving without vector: {}", e.getMessage());
        }

        return documentRepository.save(document);
    }
}
