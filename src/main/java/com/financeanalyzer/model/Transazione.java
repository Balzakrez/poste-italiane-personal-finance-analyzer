package com.financeanalyzer.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "transazione",
        uniqueConstraints = @UniqueConstraint(name = "uk_hash_riga", columnNames = "hashRiga")
)
public class Transazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoMovimento tipoMovimento;

    @Enumerated(EnumType.STRING)
    private Category categoria;

    @Enumerated(EnumType.STRING)
    private FonteCategoria fonteCategoria;

    @Column(nullable = false, length = 64)
    private String hashRiga;

    private LocalDate dataContabile;
    private LocalDate dataValuta;
    private String causaleOriginale;
    private BigDecimal importo;

    /* ********************************************************************************************** */

    protected Transazione() {
    }

    public Transazione(TipoMovimento tipoMovimento, String hashRiga,
                       LocalDate dataContabile, LocalDate dataValuta,
                       String causaleOriginale, BigDecimal importo) {
        this.tipoMovimento = tipoMovimento;
        this.hashRiga = hashRiga;
        this.dataContabile = dataContabile;
        this.dataValuta = dataValuta;
        this.causaleOriginale = causaleOriginale;
        this.importo = importo;
    }

    public void assegnaCategoria(Category categoria, FonteCategoria fonteCategoria) {
        this.categoria = categoria;
        this.fonteCategoria = fonteCategoria;
    }

    public Long getId() {
        return id;
    }

    public TipoMovimento getTipoMovimento() {
        return tipoMovimento;
    }

    public Category getCategoria() {
        return categoria;
    }

    public FonteCategoria getFonteCategoria() {
        return fonteCategoria;
    }

    public String getHashRiga() {
        return hashRiga;
    }

    public LocalDate getDataContabile() {
        return dataContabile;
    }

    public LocalDate getDataValuta() {
        return dataValuta;
    }

    public String getCausaleOriginale() {
        return causaleOriginale;
    }

    public BigDecimal getImporto() {
        return importo;
    }
}