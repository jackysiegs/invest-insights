package com.ii.backend.controller;

import com.ii.backend.model.Client;
import com.ii.backend.repository.ClientRepository;
import com.ii.backend.repository.AdvisorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "http://localhost:4200")
public class ClientController {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AdvisorRepository advisorRepository;

    // GET all clients
    @GetMapping
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    // POST a new client
    @PostMapping
    public Client createClient(@RequestBody Client client) {
        if (client.getAdvisor() != null && client.getAdvisor().getId() != null) {
            advisorRepository.findById(client.getAdvisor().getId())
                    .ifPresent(client::setAdvisor);
        }
        return clientRepository.save(client);
    }

    // PUT to update advisor assignment for a client
    @PutMapping("/{clientId}/assign-advisor/{advisorId}")
    public ResponseEntity<?> assignAdvisorToClient(@PathVariable Long clientId, @PathVariable Long advisorId) {
        try {
            // Find the client
            Optional<Client> clientOpt = clientRepository.findById(clientId);
            if (clientOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Client not found with ID: " + clientId);
            }

            // Find the advisor
            Optional<com.ii.backend.model.Advisor> advisorOpt = advisorRepository.findById(advisorId);
            if (advisorOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Advisor not found with ID: " + advisorId);
            }

            // Update the client's advisor
            Client client = clientOpt.get();
            client.setAdvisor(advisorOpt.get());
            Client savedClient = clientRepository.save(client);

            return ResponseEntity.ok(savedClient);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error assigning advisor: " + e.getMessage());
        }
    }

    // GET client by ID
    @GetMapping("/{clientId}")
    public ResponseEntity<Client> getClientById(@PathVariable Long clientId) {
        Optional<Client> client = clientRepository.findById(clientId);
        return client.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
