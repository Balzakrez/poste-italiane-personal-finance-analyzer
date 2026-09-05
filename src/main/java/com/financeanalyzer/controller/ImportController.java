package com.financeanalyzer.controller;

import com.financeanalyzer.dto.RisultatoImportDTO;
import com.financeanalyzer.service.MovimentiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class ImportController {

    private final MovimentiService movimentiService;

    public ImportController(MovimentiService movimentiService) {
        this.movimentiService = movimentiService;
    }

    @PostMapping("/api/transazioni/import")
    public RisultatoImportDTO importa(@RequestParam("file") MultipartFile file) throws IOException {
        return movimentiService.importSheetFile(file);
    }
}
