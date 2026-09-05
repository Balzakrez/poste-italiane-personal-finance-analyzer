package com.financeanalyzer.dto;

import com.financeanalyzer.model.EsitoClassificazioneAi;

public record RisultatoImportDTO(
        int righeImportate,
        int righeDuplicate,
        int righeNonValide,
        EsitoClassificazioneAi esitoClassificazioneAi) {
}