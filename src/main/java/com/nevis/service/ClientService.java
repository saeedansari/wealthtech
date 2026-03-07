package com.nevis.service;

import com.nevis.dto.ClientRequest;
import com.nevis.entity.Client;
import com.nevis.exception.ResourceNotFoundException;
import com.nevis.repository.ClientRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Client createClient(ClientRequest request) {
        Optional<Client> existingClient = clientRepository.findByEmail(request.getEmail());
        if (existingClient.isPresent()) {
            return existingClient.get();
        }
        Client client = new Client();
        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        client.setEmail(request.getEmail());
        client.setDescription(request.getDescription());
        if (request.getSocialLinks() != null) {
            client.setSocialLinks(request.getSocialLinks().toArray(new String[0]));
        }
        return clientRepository.save(client);
    }

    public Client getClient(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
    }
}
