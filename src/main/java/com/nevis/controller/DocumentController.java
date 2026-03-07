package com.nevis.controller;

import com.nevis.dto.DocumentRequest;
import com.nevis.dto.DocumentResponse;
import com.nevis.entity.Document;
import com.nevis.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/clients/{id}/documents")
@Tag(name = "Documents", description = "Document management endpoints")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @Operation(summary = "Create a document for a client")
    public ResponseEntity<DocumentResponse> createDocument(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentRequest request) {
        Document document = documentService.createDocument(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.fromEntity(document));
    }
}
