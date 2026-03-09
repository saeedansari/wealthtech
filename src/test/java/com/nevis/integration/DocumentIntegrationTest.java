package com.nevis.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.dto.ClientRequest;
import com.nevis.dto.DocumentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DOCUMENTS_URL = "/v1/clients/{id}/documents";

    @Test
    void createDocument_withValidData_returns201() throws Exception {
        String clientId = createTestClient("Alice", "Wonder", "alice@example.com");

        DocumentRequest request = new DocumentRequest();
        request.setTitle("Utility Bill");
        request.setContent("Monthly utility bill showing residential address at 123 Main St.");

        mockMvc.perform(post(DOCUMENTS_URL, clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.title").value("Utility Bill"))
                .andExpect(jsonPath("$.content").value("Monthly utility bill showing residential address at 123 Main St."))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createDocument_withNonexistentClient_returns404() throws Exception {
        UUID fakeId = UUID.randomUUID();
        DocumentRequest request = new DocumentRequest();
        request.setTitle("Some Document");
        request.setContent("Some content");

        mockMvc.perform(post(DOCUMENTS_URL, fakeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Client not found with id: " + fakeId));
    }

    @Test
    void createDocument_withMissingTitle_returns400() throws Exception {
        String clientId = createTestClient("Bob", "Builder", "bob@example.com");

        DocumentRequest request = new DocumentRequest();
        request.setContent("Some content");

        mockMvc.perform(post(DOCUMENTS_URL, clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages").isArray());
    }

    @Test
    void createDocument_withMissingContent_returns400() throws Exception {
        String clientId = createTestClient("Carol", "Danvers", "carol@example.com");

        DocumentRequest request = new DocumentRequest();
        request.setTitle("A Title");

        mockMvc.perform(post(DOCUMENTS_URL, clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages").isArray());
    }

    @Test
    void createDocument_withEmptyBody_returns400() throws Exception {
        String clientId = createTestClient("Dave", "Grohl", "dave@example.com");

        mockMvc.perform(post(DOCUMENTS_URL, clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String createTestClient(String firstName, String lastName, String email) throws Exception {
        ClientRequest clientRequest = new ClientRequest();
        clientRequest.setFirstName(firstName);
        clientRequest.setLastName(lastName);
        clientRequest.setEmail(email);

        MvcResult result = mockMvc.perform(post("/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
