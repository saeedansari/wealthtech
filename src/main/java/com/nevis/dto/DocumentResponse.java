package com.nevis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nevis.entity.Document;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DocumentResponse {

    private UUID id;
    private UUID clientId;
    private String title;
    private String content;
    private String summary;
    private LocalDateTime createdAt;
    private Double distance;

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

}
