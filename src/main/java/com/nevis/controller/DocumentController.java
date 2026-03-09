package com.nevis.controller;

import com.nevis.dto.DocumentRequest;
import com.nevis.dto.DocumentResponse;
import com.nevis.entity.Document;
import com.nevis.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/v1/clients/{id}/documents")
@Tag(name = "Documents", description = "Document management endpoints")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @Operation(
            summary = "Create a document for a client",
            parameters = @Parameter(
                    name = "id",
                    description = "Client UUID",
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            ),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DocumentRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "title": "Utility Bill - March 2025",
                                              "content": "This is a utility bill from the electric company showing the client's residential address at 42 Wallaby Way, Sydney. The bill amount is $150.00."
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Document created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = DocumentResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                                                      "clientId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                      "title": "Utility Bill - March 2025",
                                                      "content": "This is a utility bill from the electric company showing the client's residential address at 42 Wallaby Way, Sydney. The bill amount is $150.00.",
                                                      "createdAt": "2025-03-15T10:35:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2025-03-15T10:35:00",
                                                      "status": 400,
                                                      "error": "Validation failed",
                                                      "messages": [
                                                        "title: title is required"
                                                      ]
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Client not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2025-03-15T10:35:00",
                                                      "status": 404,
                                                      "error": "Not Found",
                                                      "message": "Client not found with id: a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<DocumentResponse> createDocument(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentRequest request) {
        Document document = documentService.createDocument(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.fromEntity(document));
    }
}
