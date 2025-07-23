package com.ii.backend.controller;

import com.ii.backend.model.Advisor;
import com.ii.backend.repository.AdvisorRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/advisors")
@CrossOrigin(origins = "http://localhost:4200")
public class AdvisorController {
    private final AdvisorRepository advisorRepository;

    public AdvisorController(AdvisorRepository advisorRepository) {
        this.advisorRepository = advisorRepository;
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "Backend service is healthy";
    }

    @GetMapping
    public List<Advisor> getAllAdvisors() {
        return advisorRepository.findAll();
    }

    @PostMapping
    public Advisor createAdvisor(@RequestBody Advisor advisor) {
        return advisorRepository.save(advisor);
    }
}
