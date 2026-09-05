package com.financeanalyzer.dto;

import com.financeanalyzer.model.Category;

import java.math.BigDecimal;

public record CategoriaTotaleDTO(Category categoria, BigDecimal totale) {
}