package com.financeanalyzer.controller;

import com.financeanalyzer.dto.DisponibilitaAiDTO;
import com.financeanalyzer.service.OllamaCheckerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiStatusController {

    private final OllamaCheckerService ollamaCheckerService;

    public AiStatusController(OllamaCheckerService ollamaCheckerService) {
        this.ollamaCheckerService = ollamaCheckerService;
    }

    @GetMapping("/api/ai/disponibilita")
    public DisponibilitaAiDTO disponibilita() {
        return new DisponibilitaAiDTO(ollamaCheckerService.isDisponibile());
    }
}