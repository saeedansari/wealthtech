package com.nevis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.dto.ClientRequest;
import com.nevis.entity.Client;
import com.nevis.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@TestPropertySource(properties = "api.key=test-api-key")
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CLIENT_URL = "/v1/clients";
    private static final String API_KEY = "test-api-key";

    @Test
    void createClient_returnsCreatedWithClientResponse() throws Exception {
        Client saved = new Client();
        saved.setId(UUID.randomUUID());
        saved.setFirstName("John");
        saved.setLastName("Doe");
        saved.setEmail("john@example.com");
        saved.setDescription("Test client");
        saved.setSocialLinks(new String[]{"https://linkedin.com/in/john"});

        when(clientService.createClient(any(ClientRequest.class))).thenReturn(saved);

        ClientRequest request = new ClientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setDescription("Test client");
        request.setSocialLinks(List.of("https://linkedin.com/in/john"));

        mockMvc.perform(post(CLIENT_URL)
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void createClient_withInvalidEmail_returns400() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("invalid-email");

        mockMvc.perform(post(CLIENT_URL)
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClient_withEmptyFirstName_returns400() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setFirstName("");
        request.setLastName("Doe");
        request.setEmail("john@example.com");

        mockMvc.perform(post(CLIENT_URL)
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClient_withNullBody_returns400() throws Exception {
        mockMvc.perform(post(CLIENT_URL)
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }
}
