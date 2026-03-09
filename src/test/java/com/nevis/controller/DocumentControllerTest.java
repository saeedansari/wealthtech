package com.nevis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.dto.DocumentRequest;
import com.nevis.entity.Document;
import com.nevis.exception.ResourceNotFoundException;
import com.nevis.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DOCUMENT_URL = "/v1/clients/{id}/documents";

    @Test
    void createDocument_returnsCreatedWithDocumentResponse() throws Exception {
        UUID clientId = UUID.randomUUID();
        Document saved = new Document();
        saved.setId(UUID.randomUUID());
        saved.setClientId(clientId);
        saved.setTitle("Bank Statement");
        saved.setContent("Q1 2025 bank statement details.");

        when(documentService.createDocument(eq(clientId), any(DocumentRequest.class))).thenReturn(saved);

        DocumentRequest request = new DocumentRequest();
        request.setTitle("Bank Statement");
        request.setContent("Q1 2025 bank statement details.");

        mockMvc.perform(post(DOCUMENT_URL, clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.title").value("Bank Statement"))
                .andExpect(jsonPath("$.content").value("Q1 2025 bank statement details."));
    }

    @Test
    void createDocument_withNonexistentClient_returns404() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(documentService.createDocument(eq(clientId), any(DocumentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Client not found with id: " + clientId));

        DocumentRequest request = new DocumentRequest();
        request.setTitle("Some Doc");
        request.setContent("Some content");

        mockMvc.perform(post(DOCUMENT_URL, clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Client not found with id: " + clientId));
    }

    @Test
    void createDocument_withMissingTitle_returns400() throws Exception {
        UUID clientId = UUID.randomUUID();
        DocumentRequest request = new DocumentRequest();
        request.setContent("Some content");

        mockMvc.perform(post(DOCUMENT_URL, clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocument_withMissingContent_returns400() throws Exception {
        UUID clientId = UUID.randomUUID();
        DocumentRequest request = new DocumentRequest();
        request.setTitle("A Title");

        mockMvc.perform(post(DOCUMENT_URL, clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
