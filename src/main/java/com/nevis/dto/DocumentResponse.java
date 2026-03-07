package com.nevis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nevis.entity.Document;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentResponse {

    private UUID id;
    private UUID clientId;
    private String title;
    private String content;
    private String summary;
    private LocalDateTime createdAt;
    private Double score;

    public static DocumentResponse fromEntity(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setClientId(document.getClientId());
        response.setTitle(document.getTitle());
        response.setContent(document.getContent());
        response.setSummary(document.getSummary());
        response.setCreatedAt(document.getCreatedAt());
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
