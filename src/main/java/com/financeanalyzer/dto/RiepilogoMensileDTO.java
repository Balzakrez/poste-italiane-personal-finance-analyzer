package com.financeanalyzer.dto;

import java.math.BigDecimal;

public record RiepilogoMensileDTO(
        String mese,
        BigDecimal totaleEntrate,
        BigDecimal totaleUscite,
        BigDecimal saldo) {
}
