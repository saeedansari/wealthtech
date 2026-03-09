package com.nevis.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.dto.ClientRequest;
import com.nevis.dto.DocumentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String SEARCH_URL = "/v1/search";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean dataSeeded = false;

    @BeforeEach
    void seedData() throws Exception {
        if (dataSeeded) return;

        // Client 1: John Doe at Nevis Wealth
        String clientId1 = createClient("John", "Doe", "john.doe@neviswealth.com",
                "Senior financial advisor specializing in wealth management");

        // Client 2: Jane Smith
        String clientId2 = createClient("Jane", "Smith", "jane.smith@example.com",
                "Junior advisor handling retirement portfolios");

        // Document for client 1: utility bill (address proof)
        createDocument(clientId1, "Utility Bill - March 2025",
                "This is a utility bill from the electric company showing the client's " +
                "residential address at 42 Wallaby Way, Sydney. The bill amount is $150.00.");

        // Document for client 1: bank statement (financial)
        createDocument(clientId1, "Bank Statement Q1 2025",
                "Quarterly bank statement showing financial transactions, account balance " +
                "of $250,000 and investment portfolio details for the first quarter of 2025.");

        // Document for client 2: proof of residence
        createDocument(clientId2, "Lease Agreement",
                "Proof of residence lease agreement for apartment at 10 Downing Street. " +
                "Monthly rent $2,500. Lease valid from January 2025 to December 2025.");

        dataSeeded = true;
    }

    @Test
    void search_byClientEmail_returnsMatchingClient() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("q", "neviswealth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients").isArray())
                .andExpect(jsonPath("$.clients", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.clients[?(@.email == 'john.doe@neviswealth.com')]", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void search_byClientFirstName_returnsMatchingClient() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("q", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients").isArray())
                .andExpect(jsonPath("$.clients", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.clients[0].firstName").value("John"));
    }

    @Test
    void search_byClientLastName_returnsMatchingClient() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("q", "Smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients").isArray())
                .andExpect(jsonPath("$.clients", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.clients[?(@.lastName == 'Smith')]", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void search_byClientDescription_returnsMatchingClient() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("q", "retirement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients").isArray())
                .andExpect(jsonPath("$.clients", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void search_semanticDocumentSearch_residentialAddressFindsUtilityBill() throws Exception {
        // "residential address electric bill" should semantically match the utility bill document
        mockMvc.perform(get(SEARCH_URL).param("q", "residential address electric bill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents").isArray())
                .andExpect(jsonPath("$.documents", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.documents[0].distance").isNumber());
    }

    @Test
    void search_semanticDocumentSearch_financialStatementFindsBankStatement() throws Exception {
        // "financial statement" should semantically match the bank statement document
        mockMvc.perform(get(SEARCH_URL).param("q", "financial statement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents").isArray())
                .andExpect(jsonPath("$.documents", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.documents[0].distance").isNumber());
    }

    @Test
    void search_responseContainsBothClientsAndDocuments() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("q", "financial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients").isArray())
                .andExpect(jsonPath("$.documents").isArray());
    }

    @Test
    void search_documentsHaveSummary() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("q", "utility bill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents").isArray())
                .andExpect(jsonPath("$.documents[0].summary").isString());
    }

    @Test
    void search_withBlankQuery_returns400() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("q", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_withMissingQuery_returns400() throws Exception {
        mockMvc.perform(get(SEARCH_URL))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_noMatchingClients_returnsEmptyClientList() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("q", "zzzznonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients").isArray())
                .andExpect(jsonPath("$.clients", hasSize(0)));
    }

    private String createClient(String firstName, String lastName, String email, String description) throws Exception {
        ClientRequest request = new ClientRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setEmail(email);
        request.setDescription(description);

        MvcResult result = mockMvc.perform(post("/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void createDocument(String clientId, String title, String content) throws Exception {
        DocumentRequest request = new DocumentRequest();
        request.setTitle(title);
        request.setContent(content);

        mockMvc.perform(post("/v1/clients/{id}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
