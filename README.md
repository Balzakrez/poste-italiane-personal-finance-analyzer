# Personal Finance Analyzer

Applicazione locale per importare gli estratti conto di Poste Italiane (formato `.xlsx`) — BancoPosta, PostePay o qualsiasi altro conto postale con lo stesso formato di esportazione — categorizzare automaticamente le spese e visualizzarle in una dashboard con riepiloghi mensili e per categoria.

Tutto gira sulla tua macchina: nessun dato finanziario lascia il computer, nessun servizio cloud coinvolto.

---

## Come si avvia

Servono due processi attivi in locale:

1. **Ollama** (opzionale, vedi sotto) — di solito parte da solo dopo l'installazione; se non è attivo: `ollama serve`
2. **L'applicazione Spring Boot** — dal tuo IDE, oppure `mvn spring-boot:run`

Poi apri `http://localhost:8080` nel browser.

Il database (H2) è **in-memory**: ad ogni riavvio dell'app tutti i dati importati si azzerano. Va reimportato l'estratto conto ogni volta.

---

## Come funziona la categorizzazione

Ogni transazione importata passa attraverso due livelli, in ordine:

### Livello 1 — Regole (`rules.yml`)

File YAML in `src/main/resources/rules.yml`. **Non è versionato** (è in `.gitignore`): il file reale contiene pattern legati ai tuoi dati personali (es. il nome del tuo datore di lavoro nella causale dello stipendio), quindi resta solo sulla tua macchina e non entra mai nella cronologia Git.

Se cloni il progetto da zero, crea `src/main/resources/rules.yml` copiando questo esempio come base, poi personalizzalo con le causali che vedi nei tuoi estratti conto:

```yaml
regole:
  - pattern: "STIPENDIO"
    categoria: STIPENDIO_COMPENSI
  - pattern: "COMMISSIONI"
    categoria: TASSE_COMMISSIONI
  - pattern: "CANONE"
    categoria: TASSE_COMMISSIONI
  - pattern: "P2P"
    categoria: TRASFERIMENTI_PRELIEVI
  - pattern: "PRELIEVO"
    categoria: TRASFERIMENTI_PRELIEVI
  - pattern: "GIROCONTO"
    categoria: TRASFERIMENTI_PRELIEVI
  - pattern: "AFFITTO"
    categoria: CASA_UTENZE_ASSICURAZIONI
  - pattern: "ENEL"
    categoria: CASA_UTENZE_ASSICURAZIONI
  - pattern: "ESSELUNGA"
    categoria: SPESA_QUOTIDIANA
  - pattern: "CONAD"
    categoria: SPESA_QUOTIDIANA
  - pattern: "FARMACIA"
    categoria: SALUTE
  - pattern: "AMAZON"
    categoria: SHOPPING_TEMPO_LIBERO
```

Senza questo file l'app non si avvia (`FILE_REGOLE` in `RuleBasedCategorizerService` lo richiede all'avvio).

- Le regole sono valutate **in ordine**, dall'alto verso il basso: vince la prima il cui `pattern` compare nella causale della transazione (case-insensitive).
- L'**ordine conta**: metti i pattern più specifici prima di quelli generici (es. `COMMISSIONI` prima di un ipotetico pattern più corto che lo includerebbe per errore).
- Le categorie disponibili sono quelle definite nell'enum `Category` (`CASA_UTENZE_ASSICURAZIONI`, `SPESA_QUOTIDIANA`, `TRASPORTI`, `SALUTE`, `SHOPPING_TEMPO_LIBERO`, `TASSE_COMMISSIONI`, `TRASFERIMENTI_PRELIEVI`, `STIPENDIO_COMPENSI`, `ALTRE_ENTRATE`) — se scrivi nel file una categoria che non esiste, l'app **non si avvia** (errore subito, non un fallimento silenzioso).
- Per aggiungere un nuovo merchant riconosciuto: aggiungi due righe al file e riavvia. Non serve toccare codice Java.

Le transazioni non coperte da nessuna regola passano al Livello 2.

### Livello 2 — Modello AI locale (Ollama)

Le causali rimaste senza categoria dopo il Livello 1 vengono inviate, in batch, a un modello linguistico che gira **in locale** tramite [Ollama](https://ollama.com) (es. Qwen 2.5). Nessun dato esce dalla macchina.

**Se Ollama è disponibile:** il modello assegna una categoria a ogni transazione residua, rispettando il vincolo entrata/uscita (non può assegnare una categoria di spesa a un'entrata o viceversa). Le transazioni categorizzate così sono riconoscibili in dashboard dal colore oro e da una piccola icona ✦.

**Se Ollama non è disponibile** (non installato, non avviato, o irraggiungibile): l'app se ne accorge con un controllo rapido *prima* di tentare la classificazione, e salta direttamente al risultato del solo Livello 1 — **l'import non si blocca né rallenta**. Le transazioni non coperte dalle regole restano semplicemente "Da categorizzare", modificabili in futuro (manualmente, o rilanciando l'import quando Ollama sarà attivo). Il messaggio a fine import indica sempre quale dei due scenari si è verificato.

---

## Struttura del progetto

```
src/main/java/com/financeanalyzer/
├── controller/    → endpoint REST
├── service/       → logica di business (import, categorizzazione, analytics)
├── model/         → entità JPA ed enum di dominio
├── dto/           → oggetti di trasferimento verso il frontend
├── repository/    → accesso dati (Spring Data JPA)
└── util/          → utility pure (calcolo hash)

src/main/resources/
├── application.yml               → configurazione (porta Ollama, modello, ecc.)
├── rules.yml                     → regole del Livello 1
└── static/index.html             → frontend (pagina singola, nessuna dipendenza esterna)
```

---

## Roadmap

- [x] Import estratto conto e deduplicazione
- [x] Categorizzazione a due livelli (regole + AI locale)
- [x] Dashboard con riepilogo mensile e per categoria
- [ ] Feedback loop: correggere manualmente una categoria e far sì che la correzione si riusi automaticamente nei prossimi import