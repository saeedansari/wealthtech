package com.nevis.dto;

import java.util.List;
import lombok.Data;

@Data
public class SearchResponse {

    private List<ClientResponse> clients;
    private List<DocumentResponse> documents;

    public SearchResponse(List<ClientResponse> clients, List<DocumentResponse> documents) {
        this.clients = clients;
        this.documents = documents;
    }
}
