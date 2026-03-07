package com.nevis.service;

import com.nevis.dto.ClientRequest;
import com.nevis.entity.Client;
import com.nevis.exception.ResourceNotFoundException;
import com.nevis.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    void createClient_savesAndReturnsClient() {
        ClientRequest request = new ClientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setDescription("A description");
        request.setSocialLinks(List.of("https://linkedin.com/in/john"));

        Client savedClient = new Client();
        savedClient.setId(UUID.randomUUID());
        savedClient.setFirstName("John");
        savedClient.setLastName("Doe");
        savedClient.setEmail("john@example.com");

        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);

        Client result = clientService.createClient(request);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void createClient_mapsSocialLinksToArray() {
        ClientRequest request = new ClientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setSocialLinks(List.of("link1", "link2"));

        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        Client result = clientService.createClient(request);

        assertNotNull(result.getSocialLinks());
        assertEquals(2, result.getSocialLinks().length);
        assertEquals("link1", result.getSocialLinks()[0]);
        assertEquals("link2", result.getSocialLinks()[1]);
    }

    @Test
    void createClient_withNullSocialLinks_savesWithoutLinks() {
        ClientRequest request = new ClientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setSocialLinks(null);

        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        Client result = clientService.createClient(request);

        assertNull(result.getSocialLinks());
    }

    @Test
    void getClient_existingId_returnsClient() {
        UUID id = UUID.randomUUID();
        Client client = new Client();
        client.setId(id);
        client.setFirstName("John");

        when(clientRepository.findById(id)).thenReturn(Optional.of(client));

        Client result = clientService.getClient(id);

        assertEquals(id, result.getId());
        assertEquals("John", result.getFirstName());
    }

    @Test
    void getClient_nonexistentId_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(clientRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clientService.getClient(id));
    }

    @Test
    void createClient_returns_existing_client() {
        String email = "john@example.com";
        Client savedClient = new Client();
        savedClient.setId(UUID.randomUUID());
        savedClient.setFirstName("John");
        savedClient.setLastName("Doe");
        savedClient.setEmail(email);
        savedClient.setDescription("Some description");

        ClientRequest request = new ClientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");


        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(savedClient));

        Client client = clientService.createClient(request);

        assertEquals(client.getEmail(), savedClient.getEmail());
        assertEquals(client.getDescription(), savedClient.getDescription());
        assertEquals(client.getFirstName(), savedClient.getFirstName());
        assertEquals(client.getLastName(), savedClient.getLastName());
    }
}
