package com.nevis.controller;

import com.nevis.dto.ClientRequest;
import com.nevis.dto.ClientResponse;
import com.nevis.entity.Client;
import com.nevis.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
@Tag(name = "Clients", description = "Client management endpoints")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @Operation(summary = "Create a new client")
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientRequest request) {
        Client client = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponse.fromEntity(client));
    }
}
