package com.nevis.controller;

import com.nevis.dto.ClientRequest;
import com.nevis.dto.ClientResponse;
import com.nevis.entity.Client;
import com.nevis.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/clients")
@Tag(name = "Clients", description = "Client management endpoints")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new client",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ClientRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Full client",
                                            summary = "Client with all fields",
                                            value = """
                                                    {
                                                      "firstName": "John",
                                                      "lastName": "Doe",
                                                      "email": "john.doe@neviswealth.com",
                                                      "description": "Senior financial advisor specializing in wealth management",
                                                      "socialLinks": ["https://linkedin.com/in/johndoe"]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Minimal client",
                                            summary = "Client with required fields only",
                                            value = """
                                                    {
                                                      "firstName": "Jane",
                                                      "lastName": "Smith",
                                                      "email": "jane.smith@example.com"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Client created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ClientResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                      "firstName": "John",
                                                      "lastName": "Doe",
                                                      "email": "john.doe@neviswealth.com",
                                                      "description": "Senior financial advisor specializing in wealth management",
                                                      "socialLinks": ["https://linkedin.com/in/johndoe"],
                                                      "createdAt": "2025-03-15T10:30:00",
                                                      "updatedAt": "2025-03-15T10:30:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2025-03-15T10:30:00",
                                                      "status": 400,
                                                      "error": "Validation failed",
                                                      "messages": [
                                                        "firstName: firstName is required",
                                                        "email: email is required"
                                                      ]
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientRequest request) {
        Client client = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponse.fromEntity(client));
    }
}
