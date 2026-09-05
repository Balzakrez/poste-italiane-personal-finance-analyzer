package com.financeanalyzer.repository;

import com.financeanalyzer.model.Transazione;
import com.financeanalyzer.model.TipoMovimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransazioneRepository extends JpaRepository<Transazione, Long> {

    boolean existsByHashRiga(String hashRiga);

    List<Transazione> findByDataValutaBetweenOrderByDataValutaAsc(LocalDate inizio, LocalDate fine);

    @Query("SELECT coalesce(sum(t.importo), 0) FROM Transazione t WHERE t.tipoMovimento = :tipo")
    BigDecimal sommaPerTipo(@Param("tipo") TipoMovimento tipo);
}
