package com.financeanalyzer.service;

import com.financeanalyzer.dto.CategoriaTotaleDTO;
import com.financeanalyzer.dto.RiepilogoDTO;
import com.financeanalyzer.dto.RiepilogoMensileDTO;
import com.financeanalyzer.dto.TransazioneDTO;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.TipoMovimento;
import com.financeanalyzer.model.Transazione;
import com.financeanalyzer.repository.TransazioneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final DateTimeFormatter FORMATO_MESE = DateTimeFormatter.ofPattern("yyyy-MM");

    private final TransazioneRepository transazioneRepository;

    /* ********************************************************************************************** */

    public AnalyticsService(TransazioneRepository transazioneRepository) {
        this.transazioneRepository = transazioneRepository;
    }

    public RiepilogoDTO getGlobalSummary() {
        BigDecimal totaleEntrate = defaultZero(transazioneRepository.sommaPerTipo(TipoMovimento.ENTRATA));
        BigDecimal totaleUscite = defaultZero(transazioneRepository.sommaPerTipo(TipoMovimento.USCITA));

        return new RiepilogoDTO(totaleEntrate, totaleUscite, totaleEntrate.subtract(totaleUscite));
    }

    public List<RiepilogoMensileDTO> getMonthlySummary() {
        List<Transazione> transazioni = transazioneRepository.findAll();

        Map<YearMonth, List<Transazione>> transazioniPerMese = transazioni.stream()
                .filter(t -> t.getDataValuta() != null)
                .collect(Collectors.groupingBy(
                                t -> YearMonth.from(t.getDataValuta()),
                                TreeMap::new,
                                Collectors.toList()
                        )
                );

        return transazioniPerMese.entrySet().stream()
                .map(entry -> computeMonthlySummary(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<TransazioneDTO> getMonthlyTransactions(String meseChiave) {
        YearMonth mese = YearMonth.parse(meseChiave, FORMATO_MESE);
        LocalDate inizio = mese.atDay(1);
        LocalDate fine = mese.atEndOfMonth();

        return transazioneRepository.findByDataValutaBetweenOrderByDataValutaAsc(inizio, fine)
                .stream()
                .map(this::mapTransactionToDTO)
                .toList();
    }

    public List<CategoriaTotaleDTO> getCategoryBreakdown() {
        List<Transazione> uscite = transazioneRepository.findAll().stream()
                .filter(t -> t.getTipoMovimento() == TipoMovimento.USCITA)
                .toList();

        Map<Optional<Category>, BigDecimal> totalePerCategoria = uscite.stream()
                .collect(Collectors.groupingBy(
                        t -> Optional.ofNullable(t.getCategoria()),
                        Collectors.reducing(BigDecimal.ZERO, Transazione::getImporto, BigDecimal::add)
                ));

        return totalePerCategoria.entrySet().stream()
                .map(entry -> new CategoriaTotaleDTO(entry.getKey().orElse(null), entry.getValue()))
                .sorted(Comparator.comparing(CategoriaTotaleDTO::totale).reversed())
                .toList();
    }

    /* ********************************************************************************************** */

    private RiepilogoMensileDTO computeMonthlySummary(YearMonth mese, List<Transazione> transazioni) {
        BigDecimal totaleEntrate = computeSumForTypeMovement(transazioni, TipoMovimento.ENTRATA);
        BigDecimal totaleUscite = computeSumForTypeMovement(transazioni, TipoMovimento.USCITA);
        return new RiepilogoMensileDTO(
                mese.format(FORMATO_MESE),
                totaleEntrate,
                totaleUscite,
                totaleEntrate.subtract(totaleUscite)
        );
    }

    private BigDecimal computeSumForTypeMovement(List<Transazione> transazioni, TipoMovimento tipo) {
        return transazioni.stream()
                .filter(t -> t.getTipoMovimento() == tipo && t.getImporto() != null)
                .map(Transazione::getImporto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private TransazioneDTO mapTransactionToDTO(Transazione t) {
        return new TransazioneDTO(
                t.getDataValuta(),
                t.getCausaleOriginale(),
                t.getImporto(),
                t.getTipoMovimento(),
                t.getCategoria(),
                t.getFonteCategoria()
        );
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }
}