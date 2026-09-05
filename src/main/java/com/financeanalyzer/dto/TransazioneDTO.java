package com.financeanalyzer.dto;

import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.FonteCategoria;
import com.financeanalyzer.model.TipoMovimento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransazioneDTO(
        LocalDate dataValuta,
        String causale,
        BigDecimal importo,
        TipoMovimento tipoMovimento,
        Category categoria,
        FonteCategoria fonteCategoria) {
}