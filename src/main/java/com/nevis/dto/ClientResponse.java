package com.nevis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nevis.entity.Client;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ClientResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String description;
    private List<String> socialLinks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double score;

    public static ClientResponse fromEntity(Client client) {
        ClientResponse response = new ClientResponse();
        response.setId(client.getId());
        response.setFirstName(client.getFirstName());
        response.setLastName(client.getLastName());
        response.setEmail(client.getEmail());
        response.setDescription(client.getDescription());
        if (client.getSocialLinks() != null) {
            response.setSocialLinks(Arrays.asList(client.getSocialLinks()));
        }
        response.setCreatedAt(client.getCreatedAt());
        response.setUpdatedAt(client.getUpdatedAt());
        return response;
    }

}