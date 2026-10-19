package com.ankit.prguard.analysis;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisView analyze(@Valid @RequestBody SubmitAnalysisRequest request) {
        return service.analyze(request.toSnapshot());
    }

    @GetMapping
    public List<AnalysisView> recent() {
        return service.findRecent();
    }

    @GetMapping("/{id}")
    public AnalysisView get(@PathVariable UUID id) {
        return service.findById(id);
    }
}
