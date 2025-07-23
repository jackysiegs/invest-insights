package com.ii.backend.service;

import com.ii.backend.model.Advisor;
import com.ii.backend.model.Client;
import com.ii.backend.repository.AdvisorRepository;
import com.ii.backend.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private AdvisorRepository advisorRepository;

    @Autowired
    private ClientRepository clientRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse authenticateAdvisor(String username, String password) throws Exception {
        // Find advisor by username
        Advisor advisor = advisorRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("Invalid username or password"));

        // BCrypt password check
        if (!passwordEncoder.matches(password, advisor.getPasswordHash())) {
            throw new Exception("Invalid username or password");
        }

        // Get advisor's clients
        List<Client> clients = clientRepository.findByAdvisorId(advisor.getId());

        // Create login response
        LoginResponse response = new LoginResponse();
        response.setAdvisorId(advisor.getId());
        response.setAdvisorName(advisor.getName());
        response.setEmail(advisor.getEmail());
        response.setToken(generateToken());
        response.setClients(clients);

        return response;
    }

    public List<Client> getAdvisorClients(Long advisorId) throws Exception {
        // Verify advisor exists
        if (!advisorRepository.existsById(advisorId)) {
            throw new Exception("Advisor not found");
        }

        return clientRepository.findByAdvisorId(advisorId);
    }

    private String generateToken() {
        // Simple token generation (in production, use JWT)
        return UUID.randomUUID().toString();
    }

    // Login Response class
    public static class LoginResponse {
        private Long advisorId;
        private String advisorName;
        private String email;
        private String token;
        private List<Client> clients;

        // Getters and setters
        public Long getAdvisorId() { return advisorId; }
        public void setAdvisorId(Long advisorId) { this.advisorId = advisorId; }
        public String getAdvisorName() { return advisorName; }
        public void setAdvisorName(String advisorName) { this.advisorName = advisorName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public List<Client> getClients() { return clients; }
        public void setClients(List<Client> clients) { this.clients = clients; }
    }
} 