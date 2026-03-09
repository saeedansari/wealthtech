package com.nevis.controller;

import com.nevis.dto.ClientResponse;
import com.nevis.dto.DocumentResponse;
import com.nevis.dto.SearchResponse;
import com.nevis.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    private static final String SEARCH_URL = "/v1/search";

    @Test
    void search_returnsClientsAndDocuments() throws Exception {
        ClientResponse client = new ClientResponse();
        client.setId(UUID.randomUUID());
        client.setFirstName("John");
        client.setLastName("Doe");
        client.setEmail("john@neviswealth.com");
        client.setScore(0.9);

        DocumentResponse doc = new DocumentResponse();
        doc.setId(UUID.randomUUID());
        doc.setClientId(client.getId());
        doc.setTitle("Utility Bill");
        doc.setContent("Electric bill for March");
        doc.setSummary("A utility bill for March.");
        doc.setDistance(0.85);

        when(searchService.search("neviswealth"))
                .thenReturn(new SearchResponse(List.of(client), List.of(doc)));

        mockMvc.perform(get(SEARCH_URL).param("q", "neviswealth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients[0].firstName").value("John"))
                .andExpect(jsonPath("$.clients[0].score").value(0.9))
                .andExpect(jsonPath("$.documents[0].title").value("Utility Bill"))
                .andExpect(jsonPath("$.documents[0].summary").value("A utility bill for March."))
                .andExpect(jsonPath("$.documents[0].distance").value(0.85));
    }

    @Test
    void search_withEmptyResults_returnsEmptyLists() throws Exception {
        when(searchService.search("zzzzz"))
                .thenReturn(new SearchResponse(List.of(), List.of()));

        mockMvc.perform(get(SEARCH_URL).param("q", "zzzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients").isEmpty())
                .andExpect(jsonPath("$.documents").isEmpty());
    }

    @Test
    void search_withBlankQuery_returns400() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("q", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_withMissingQueryParam_returns400() throws Exception {
        mockMvc.perform(get(SEARCH_URL))
                .andExpect(status().isBadRequest());
    }
}
