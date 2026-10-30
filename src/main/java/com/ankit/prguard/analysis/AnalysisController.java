package com.ankit.prguard.analysis;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.net.URI;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AnalysisView> analyze(@Valid @RequestBody SubmitAnalysisRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            Authentication authentication) {
        AnalysisView job = service.submit(request.toSnapshot(), authentication.getName(), key);
        return ResponseEntity.accepted().location(URI.create("/api/analyses/" + job.id())).body(job);
    }

    @GetMapping
    public List<AnalysisView> recent(Authentication authentication) {
        return service.findRecent(authentication.getName());
    }

    @GetMapping("/{id}")
    public AnalysisView get(@PathVariable UUID id, Authentication authentication) {
        return service.findById(id, authentication.getName());
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<AnalysisView> retry(@PathVariable UUID id, Authentication authentication) {
        AnalysisView job = service.retry(id, authentication.getName());
        return ResponseEntity.accepted().location(URI.create("/api/analyses/" + id)).body(job);
    }
}
