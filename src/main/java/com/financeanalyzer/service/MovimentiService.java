package com.financeanalyzer.service;

import com.financeanalyzer.dto.RisultatoImportDTO;
import com.financeanalyzer.model.EsitoClassificazioneAi;
import com.financeanalyzer.model.FonteCategoria;
import com.financeanalyzer.model.TipoMovimento;
import com.financeanalyzer.model.Transazione;
import com.financeanalyzer.repository.TransazioneRepository;
import com.financeanalyzer.util.HashRigaCalculator;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MovimentiService {

    private static final String INTESTAZIONE_ATTESA = "Data Contabile";
    private static final int COLONNA_DATA_CONTABILE = 0;
    private static final int COLONNA_DATA_VALUTA = 1;
    private static final int COLONNA_IMPORTO = 2;
    private static final int COLONNA_CAUSALE = 3;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    };

    private final TransazioneRepository repository;
    private final RuleBasedCategorizerService categorizerService;
    private final AiCategorizerService aiCategorizerService;

    /* ********************************************************************************************** */

    public MovimentiService(TransazioneRepository repository,
                            RuleBasedCategorizerService categorizerService,
                            AiCategorizerService aiCategorizerService) {
        this.repository = repository;
        this.categorizerService = categorizerService;
        this.aiCategorizerService = aiCategorizerService;
    }

    @Transactional
    public RisultatoImportDTO importSheetFile(MultipartFile file) throws IOException {
        int righeDuplicate = 0;
        int righeNonValide = 0;

        List<Transazione> daSalvare = new ArrayList<>();
        Map<String, Integer> conteggioOccorrenze = new HashMap<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            int primaRigaDati = findFirstRowFromSheet(sheet);

            for (int i = primaRigaDati; i <= sheet.getLastRowNum(); i++) {
                Row riga = sheet.getRow(i);
                if (isEmptyRow(riga)) {
                    continue;
                }

                try {
                    Transazione transazione = parseRow(riga, conteggioOccorrenze);

                    if (repository.existsByHashRiga(transazione.getHashRiga())) {
                        righeDuplicate++;
                        continue;
                    }

                    daSalvare.add(transazione);

                } catch (Exception e) {
                    righeNonValide++;
                }
            }
        }

        EsitoClassificazioneAi esitoAi = classificaConLivelloDue(daSalvare);

        if (!daSalvare.isEmpty()) {
            repository.saveAll(daSalvare);
        }

        return new RisultatoImportDTO(daSalvare.size(), righeDuplicate, righeNonValide, esitoAi);
    }

    private Transazione parseRow(Row riga, Map<String, Integer> conteggioOccorrenze) {
        LocalDate dataContabile = parseCellToDate(riga.getCell(COLONNA_DATA_CONTABILE));
        LocalDate dataValuta = parseCellToDate(riga.getCell(COLONNA_DATA_VALUTA));
        BigDecimal importoConSegno = parseCellToAmount(riga.getCell(COLONNA_IMPORTO));
        String causale = parseCellToDescription(riga.getCell(COLONNA_CAUSALE)).trim();

        if (causale.isEmpty()) {
            throw new IllegalArgumentException("Causale mancante");
        }

        TipoMovimento tipoMovimento = importoConSegno.signum() < 0 ? TipoMovimento.USCITA : TipoMovimento.ENTRATA;
        BigDecimal importoAbs = importoConSegno.abs();

        String key = dataContabile + "|" + importoAbs.toPlainString() + "|" + causale.toLowerCase();
        int occorrenza = conteggioOccorrenze.getOrDefault(key, 0) + 1;
        conteggioOccorrenze.put(key, occorrenza);

        String hash = HashRigaCalculator.calcola(dataContabile, importoAbs, causale, occorrenza);

        Transazione transazione = new Transazione(tipoMovimento, hash, dataContabile, dataValuta, causale, importoAbs);
        categorizerService.classifica(causale).ifPresent(categoria -> transazione.assegnaCategoria(categoria, FonteCategoria.REGOLA));

        return transazione;
    }

    private EsitoClassificazioneAi classificaConLivelloDue(List<Transazione> transazioni) {
        List<Transazione> nonCategorizzate = transazioni.stream()
                .filter(t -> t.getCategoria() == null)
                .toList();

        if (nonCategorizzate.isEmpty()) {
            return EsitoClassificazioneAi.NON_NECESSARIA;
        }

        boolean applicata = aiCategorizerService.classifica(nonCategorizzate);
        return applicata ? EsitoClassificazioneAi.APPLICATA : EsitoClassificazioneAi.OLLAMA_NON_DISPONIBILE;
    }

    /* ********************************************************************************************** */

    private int findFirstRowFromSheet(Sheet sheet) {
        for (Row riga : sheet) {
            Cell primaCella = riga.getCell(COLONNA_DATA_CONTABILE);
            if (primaCella != null && INTESTAZIONE_ATTESA.equalsIgnoreCase(parseCellToDescription(primaCella).trim())) {
                return riga.getRowNum() + 1;
            }
        }
        throw new IllegalStateException("Intestazione '" + INTESTAZIONE_ATTESA + "' non trovata nel file");
    }

    private boolean isEmptyRow(Row riga) {
        if (riga == null) {
            return true;
        }
        Cell primaCella = riga.getCell(COLONNA_DATA_CONTABILE);
        return primaCella == null
                || primaCella.getCellType() == CellType.BLANK
                || (primaCella.getCellType() == CellType.STRING && primaCella.getStringCellValue().isBlank());
    }

    private LocalDate parseCellToDate(Cell cella) {
        if (cella == null) {
            throw new IllegalArgumentException("Cella data nulla");
        }
        if (cella.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cella)) {
            return cella.getLocalDateTimeCellValue().toLocalDate();
        }
        if (cella.getCellType() == CellType.STRING) {
            String val = cella.getStringCellValue().trim();
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    return LocalDate.parse(val, formatter);
                } catch (Exception ignored) {
                }
            }
        }
        throw new IllegalArgumentException("Formato data non riconosciuto nella cella");
    }

    private BigDecimal parseCellToAmount(Cell cella) {
        if (cella == null) {
            throw new IllegalArgumentException("Cella importo nulla");
        }
        if (cella.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cella.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
        }
        if (cella.getCellType() == CellType.STRING) {
            String val = cella.getStringCellValue()
                    .replace("€", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .trim();
            return new BigDecimal(val).setScale(2, RoundingMode.HALF_UP);
        }
        throw new IllegalArgumentException("Formato importo non valido nella cella");
    }

    private String parseCellToDescription(Cell cella) {
        if (cella == null) {
            return "";
        }
        return switch (cella.getCellType()) {
            case STRING -> cella.getStringCellValue();
            case NUMERIC -> String.valueOf(cella.getNumericCellValue());
            case FORMULA -> cella.getCellFormula();
            default -> "";
        };
    }
}