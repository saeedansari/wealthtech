package com.nevis.dto;

import java.util.List;

public class SearchResponse {

    private List<ClientResponse> clients;
    private List<DocumentResponse> documents;

    public SearchResponse(List<ClientResponse> clients, List<DocumentResponse> documents) {
        this.clients = clients;
        this.documents = documents;
    }

    public List<ClientResponse> getClients() {
        return clients;
    }

    public void setClients(List<ClientResponse> clients) {
        this.clients = clients;
    }

    public List<DocumentResponse> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentResponse> documents) {
        this.documents = documents;
    }
}
