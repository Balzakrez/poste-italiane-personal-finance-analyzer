package com.financeanalyzer.dto;

import java.math.BigDecimal;

public record RiepilogoDTO(
        BigDecimal totaleEntrate,
        BigDecimal totaleUscite,
        BigDecimal saldo) {
}
