package com.financeanalyzer.service;

import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.FonteCategoria;
import com.financeanalyzer.model.TipoMovimento;
import com.financeanalyzer.model.Transazione;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AiCategorizerService {

    private static final int DIMENSIONE_BATCH = 15;

    private final ChatClient chatClient;
    private final OllamaCheckerService ollamaCheckerService;

    /* ********************************************************************************************** */

    public AiCategorizerService(ChatClient chatClient, OllamaCheckerService ollamaCheckerService) {
        this.chatClient = chatClient;
        this.ollamaCheckerService = ollamaCheckerService;
    }

    public boolean classifica(List<Transazione> nonCategorizzate) {
        if (!ollamaCheckerService.isDisponibile()) {
            return false;
        }

        for (List<Transazione> batch : suddividiInBatch(nonCategorizzate, DIMENSIONE_BATCH)) {
            classificaBatch(batch);
        }
        return true;
    }

    /* ********************************************************************************************** */

    private void classificaBatch(List<Transazione> batch) {
        List<RichiestaClassificazione> richieste = IntStream.range(0, batch.size())
                .mapToObj(i -> new RichiestaClassificazione(
                        i,
                        batch.get(i).getCausaleOriginale(),
                        batch.get(i).getTipoMovimento()
                ))
                .toList();

        List<RispostaClassificazione> risposte = interrogaModello(richieste);
        if (risposte == null) {
            return;
        }

        Map<Integer, Category> categoriaPerId = risposte.stream()
                .collect(Collectors.toMap(RispostaClassificazione::id, RispostaClassificazione::categoria));

        for (int i = 0; i < batch.size(); i++) {
            Category categoria = categoriaPerId.get(i);
            Transazione transazione = batch.get(i);

            if (categoria != null && categoria.isCompatibileCon(transazione.getTipoMovimento())) {
                transazione.assegnaCategoria(categoria, FonteCategoria.AI);
            }
        }
    }

    private List<RispostaClassificazione> interrogaModello(List<RichiestaClassificazione> richieste) {
        try {
            return chatClient.prompt()
                    .system(promptDiSistema())
                    .user(promptUtente(richieste))
                    .call()
                    .entity(new ParameterizedTypeReference<List<RispostaClassificazione>>() {
                    });
        } catch (Exception e) {
            return null;
        }
    }

    private List<List<Transazione>> suddividiInBatch(List<Transazione> transazioni, int dimensione) {
        List<List<Transazione>> batch = new ArrayList<>();
        for (int i = 0; i < transazioni.size(); i += dimensione) {
            batch.add(transazioni.subList(i, Math.min(i + dimensione, transazioni.size())));
        }
        return batch;
    }

    private String promptDiSistema() {
        String categorieUscita = String.join(", ", categorieCompatibiliCon(TipoMovimento.USCITA));
        String categorieEntrata = String.join(", ", categorieCompatibiliCon(TipoMovimento.ENTRATA));

        return """
                Sei un classificatore di causali di movimenti bancari italiani.
                Ogni riga indica tra parentesi quadre se il movimento è una USCITA o una ENTRATA.
                Per le righe USCITA usa esclusivamente una di queste categorie: %s
                Per le righe ENTRATA usa esclusivamente una di queste categorie: %s
                Non usare mai una categoria di entrata per una riga di uscita, o viceversa.
                Rispondi SOLO con un array JSON di oggetti con i campi "id" (numero) e "categoria".
                Nessun testo, spiegazione o markdown oltre al JSON.
                """.formatted(categorieUscita, categorieEntrata);
    }

    private List<String> categorieCompatibiliCon(TipoMovimento tipoMovimento) {
        return Arrays.stream(Category.values())
                .filter(categoria -> categoria.isCompatibileCon(tipoMovimento))
                .map(Enum::name)
                .toList();
    }

    private String promptUtente(List<RichiestaClassificazione> richieste) {
        return richieste.stream()
                .map(r -> r.id() + " [" + r.tipoMovimento() + "]: " + r.causale())
                .collect(Collectors.joining("\n"));
    }

    private record RichiestaClassificazione(int id, String causale, TipoMovimento tipoMovimento) {
    }

    private record RispostaClassificazione(int id, Category categoria) {
    }
}