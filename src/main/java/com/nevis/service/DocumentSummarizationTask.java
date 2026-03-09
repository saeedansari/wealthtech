package com.nevis.service;

import com.nevis.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentSummarizationTask {

    private static final Logger log = LoggerFactory.getLogger(DocumentSummarizationTask.class);

    private final SummarizationService summarizationService;
    private final DocumentRepository documentRepository;

    @Async
    @Transactional
    public void summarizeAndUpdate(UUID documentId, String content) {
        try {
            String summary = summarizationService.summarize(content);
            if (summary != null) {
                documentRepository.findById(documentId).ifPresent(doc -> {
                    doc.setSummary(summary);
                    documentRepository.save(doc);
                    log.info("Updated document {} with summary", documentId);
                });
            }
        } catch (Exception e) {
            log.warn("Failed to summarize document {}: {}", documentId, e.getMessage());
        }
    }
}
