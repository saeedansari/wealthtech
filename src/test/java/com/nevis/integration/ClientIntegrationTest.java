package com.nevis.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.dto.ClientRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(properties = "api.key=test-api-key")
class ClientIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CLIENTS_URL = "/v1/clients";

    @Test
    void createClient_withValidData_returns201() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe.test@neviswealth.com");
        request.setDescription("A high-net-worth client");
        request.setSocialLinks(List.of("https://linkedin.com/in/johndoe"));

        mockMvc.perform(post(CLIENTS_URL).header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe.test@neviswealth.com"))
                .andExpect(jsonPath("$.description").value("A high-net-worth client"))
                .andExpect(jsonPath("$.socialLinks[0]").value("https://linkedin.com/in/johndoe"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createClient_withMissingFirstName_returns400() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setLastName("Doe");
        request.setEmail("john@example.com");

        mockMvc.perform(post(CLIENTS_URL).header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createClient_withMissingLastName_returns400() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setFirstName("John");
        request.setEmail("john@example.com");

        mockMvc.perform(post(CLIENTS_URL).header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createClient_withMissingEmail_returns400() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");

        mockMvc.perform(post(CLIENTS_URL).header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createClient_withInvalidEmail_returns400() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("not-an-email@mail");

        mockMvc.perform(post(CLIENTS_URL).header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createClient_withMinimalData_returns201() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setEmail("jane@example.com");

        mockMvc.perform(post(CLIENTS_URL).header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void createClient_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post(CLIENTS_URL).header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
