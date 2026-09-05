package com.financeanalyzer.controller;

import com.financeanalyzer.dto.CategoriaTotaleDTO;
import com.financeanalyzer.dto.RiepilogoDTO;
import com.financeanalyzer.dto.RiepilogoMensileDTO;
import com.financeanalyzer.dto.TransazioneDTO;
import com.financeanalyzer.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RiepilogoController {

    private final AnalyticsService analyticsService;

    /* ********************************************************************************************** */

    public RiepilogoController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/api/analytics/riepilogo")
    public RiepilogoDTO riepilogo() {
        return analyticsService.getGlobalSummary();
    }

    @GetMapping("/api/analytics/riepilogo/mensile")
    public List<RiepilogoMensileDTO> riepilogoMensile() {
        return analyticsService.getMonthlySummary();
    }

    @GetMapping("/api/analytics/riepilogo/categorie")
    public List<CategoriaTotaleDTO> riepilogoCategorie() {
        return analyticsService.getCategoryBreakdown();
    }

    @GetMapping("/api/analytics/transazioni/{mese}")
    public List<TransazioneDTO> transazioniDelMese(@PathVariable String mese) {
        return analyticsService.getMonthlyTransactions(mese);
    }
}