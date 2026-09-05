package com.financeanalyzer.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Objects;

public final class HashRigaCalculator {

    private HashRigaCalculator() {
    }

    /* ********************************************************************************************** */

    public static String calcola(LocalDate dataContabile, BigDecimal importo, String causale, int progressivoOccorrenza) {
        Objects.requireNonNull(dataContabile, "dataContabile non può essere null");
        Objects.requireNonNull(importo, "importo non può essere null");
        Objects.requireNonNull(causale, "causale non può essere null");

        String importoNormalizzato = importo.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String causaleNormalizzata = causale.trim().replaceAll("\\s+", " ");

        // Includiamo il progressivo nel payload di hashing
        String contenuto = dataContabile + "|" + importoNormalizzato + "|" + causaleNormalizzata + "|#" + progressivoOccorrenza;

        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(contenuto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 non disponibile", e);
        }
    }

    /* ********************************************************************************************** */
}