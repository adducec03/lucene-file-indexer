# Note per la relazione

File di appunti: raccoglie le scelte fatte durante lo sviluppo, con le motivazioni,
da riutilizzare nella relazione finale. Si aggiorna man mano.
Stato: **in corso** (indicizzazione fatta, ricerca da implementare).

## 1. Setup del progetto

- **Linguaggio / build:** Java 21, Maven (con wrapper `./mvnw`, nessuna installazione richiesta).
- **Lucene 10.3.1**, la stessa versione delle slide del corso.
  Dipendenze: `lucene-core`, `lucene-analysis-common`, `lucene-queryparser`.
- **Struttura:** package `it.homework.lucene` con le classi
  `Main`, `Analyzers`, `FileIndexer`, `QueryBuilder`, `FileSearcher`.
- **Lingua del codice e del README:** inglese.

## 2. Dataset di prova

- **202 file `.txt`, circa 3 MB** in `data/docs/`:
  - 100 articoli di Wikipedia in italiano (prefisso `it_`)
  - 100 articoli di Wikipedia in inglese (prefisso `en_`)
  - 2 file di esempio scritti a mano (`example1.txt`, `example2.txt`)
- **Perché Wikipedia:** serviva un corpus multilingua (italiano + inglese) di testo reale.
  Project Gutenberg non era raggiungibile dall'ambiente di sviluppo.
- **Come sono stati scelti:** articoli casuali tramite le API di Wikipedia, scartando quelli
  troppo corti (< 12000 byte di pagina, < 5000 caratteri di testo estratto).
  Ogni file contiene il titolo nella prima riga, poi il testo in chiaro.
- **Nomi dei file:** `<lingua>_<Titolo_con_underscore>.txt`, così il campo `nome`
  contiene parole significative da cercare.
- **Licenza:** i testi di Wikipedia sono CC BY-SA, da citare nella relazione.

## 3. Analyzer scelti (campo per campo)

Combinati con un `PerFieldAnalyzerWrapper` (un analyzer diverso per campo, come nella slide 28).
Il default (`StandardAnalyzer`) non viene mai usato perché i campi sono solo due.

### Campo `nome`
- **Analyzer:** `CustomAnalyzer` = `StandardTokenizer` + `LowerCaseFilter` + `WordDelimiterGraphFilter`.
- **Motivazione:**
  - il tokenizer standard spezza su spazi, trattini e punti, ma **non sul carattere `_`**,
    che nei nomi dei file è il separatore più comune: il `WordDelimiterGraphFilter` lo risolve
    (spezza anche tra lettere e numeri);
  - minuscole, per cercare senza badare alle maiuscole;
  - **nessuna stopword e nessuno stemming**: nei nomi anche le parole "di" o "il" possono
    essere significative e lo stemming altera nomi propri e sigle.
- **Estensione `.txt`:** rimossa prima di indicizzare il nome, altrimenti "txt" sarebbe un
  termine presente in tutti i documenti.
- **Verifica (output reale):**
  - `Relazione_Finale-2024.txt` → `relazione`, `finale`, `2024`, `txt`
  - `iPhone15 Notes` → `iphone`, `15`, `notes`

### Campo `contenuto`
- **Analyzer:** `StandardAnalyzer` con un insieme di stopword che è **l'unione** delle
  stopword italiane (`ItalianAnalyzer`) e inglesi (`EnglishAnalyzer`).
- **Motivazione:** i testi sono in due lingue. `ItalianAnalyzer` o `EnglishAnalyzer` da soli
  applicherebbero lo stemming di una lingua anche all'altra, storpiando le parole.
  Il tokenizer Unicode e le minuscole vanno bene per entrambe.
- **Limiti da dichiarare:**
  - **nessuno stemming**: "libro" e "libri" restano termini diversi;
  - l'unione delle stopword può eliminare una parola che in una lingua è stopword e
    nell'altra è significativa.
- **Verifica (output reale):** `Il Data Scientist e the quick brown fox, perché?`
  → `data`, `scientist`, `quick`, `brown`, `fox`.

## 4. Indicizzazione (`FileIndexer`)

- **Un `Document` Lucene per file**, con due campi (due posting list):
  - `nome` → `TextField`, `Store.YES` (serve per stamparlo nei risultati)
  - `contenuto` → `TextField`, `Store.NO` (solo ricercabile, il testo non viene salvato
    nell'indice: risparmia spazio e non serve rileggerlo)
- **Visita ricorsiva** della cartella; accetta `.txt` senza distinguere maiuscole/minuscole.
- **Indice ricreato da zero a ogni esecuzione** (`OpenMode.CREATE`): niente duplicati.
- **File illeggibili** (es. codifica non UTF-8): vengono saltati con un avviso e contati a parte,
  senza interrompere l'indicizzazione.
- **Statistiche stampate:** file indicizzati, file saltati, millisecondi totali.

### Risultati misurati
| Esecuzione | File indicizzati | Saltati | Tempo |
|---|---|---|---|
| 1 | 202 | 0 | 565 ms |
| 2 | 202 | 0 | 466 ms |
| 3 | 202 | 0 | 477 ms |

- Dimensione dell'indice: circa 1,2 MB (dataset di 3,0 MB).
- Il tempo include lettura dei file e analisi, ed è misurato in una JVM appena avviata
  (nessun warm-up): conviene citare l'ordine di grandezza (~0,5 s) e non il singolo valore.
- Macchina: da indicare nella relazione (modello, CPU, RAM).

## 5. Interfaccia da riga di comando (`Main`)

- `index <docsDir> <indexDir>` costruisce l'indice.
- `search <indexDir>` avvia il ciclo di ricerca interattivo (**da implementare**).
- Il numero di argomenti viene controllato per ciascun comando; negli altri casi stampa l'uso.

## 6. Ricerca (`QueryBuilder`, `FileSearcher`): scelte decise, implementazione da fare

- **Sintassi della query:** `nome <termini>` oppure `contenuto <termini>`,
  con le virgolette per le frasi (richiesto dalla consegna).
- **Parsing:** si usa il `QueryParser` di Lucene, con il campo scelto come campo di default
  e l'analyzer di quel campo. Si estrae a mano solo la parola chiave iniziale.
  Valutare più avanti un parsing manuale se questa scelta mostra dei limiti.
- **Più termini senza virgolette: OR.** È il comportamento standard del `QueryParser`
  e coincide con le slide (termini senza modificatori "optional", `Occur.SHOULD`).
  I documenti che contengono più termini ottengono un punteggio più alto.
- **Ranking:** similarity di default di Lucene (BM25), non modificata.

## 7. Punti ancora aperti

- Escape dei caratteri speciali del `QueryParser` (`:`, `+`, `-`, `*`, ...): da decidere
  se lasciarli attivi o neutralizzarli.
- Gestione degli errori di sintassi nella query (virgolette non chiuse, campo sconosciuto).
- Numero di risultati stampati (top-K).
- Una decina di query di test, con risultati e commento.
- URL del repository GitHub.
