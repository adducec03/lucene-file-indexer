# Note per la relazione

File di appunti: raccoglie le scelte fatte durante lo sviluppo, con le motivazioni e i numeri
misurati, da riutilizzare nella relazione finale. Si aggiorna man mano.

**Stato:** indicizzazione, ricerca, 21 query di test e relazione (bozza completa in
`relazione/relazione.tex` / `.pdf`) pronte. In corso: dataset più grande e valutazione con
metriche (consiglio del professore). Ultimo aggiornamento: 12 novembre 2026.

## 1. Setup del progetto

- **Linguaggio / build:** Java 21, Maven (con wrapper `./mvnw`, nessuna installazione richiesta).
- **Lucene 10.3.1**, la stessa versione delle slide del corso.
  Dipendenze: `lucene-core`, `lucene-analysis-common`, `lucene-queryparser`.
- **Classi** (package `it.homework.lucene`): `Main`, `Analyzers`, `FileIndexer`, `QueryBuilder`,
  `FileSearcher`.
- **Script nella radice:** `index.sh`, `search.sh` (scorciatoie per i comandi Maven) e
  `run-queries.sh` (esegue `queries.txt` e scrive `results/query-results.md`).
- **Script in `tools/` (Python, solo libreria standard):**
  - `build_corpus.py`: scarica il corpus da Wikipedia e scrive `data/corpus-manifest.csv`;
  - `benchmark_indexing.py`: misura i tempi di indicizzazione al crescere dei file.
- **Lingua del codice e del README:** inglese; commenti in `Analyzers.java` in italiano.
- **Relazione:** `cd relazione && pdflatex relazione.tex` (due volte, per i riferimenti).
  La cartella `relazione/` è nel `.gitignore` (scelta dell'autore).

## 2. Dataset

### 2.1 Corpus piccolo (`data/docs/`, usato finora)
- **202 file `.txt`, circa 2,8 MB**: 100 articoli di Wikipedia in italiano (`it_`), 100 in inglese
  (`en_`), più 2 file scritti a mano (`example1.txt`, `example2.txt`). Circa 425.000 parole
  (215.000 italiane, 209.000 inglesi); dimensione mediana 10 KB, massima 103 KB.
- **Perché Wikipedia:** serviva testo reale multilingua (italiano + inglese). Project Gutenberg
  non era raggiungibile dall'ambiente di sviluppo.
- **Come sono stati scelti:** articoli casuali tramite le API di Wikipedia, scartando quelli
  troppo corti (< 12000 byte di pagina, < 5000 caratteri di testo estratto).
- **Formato:** `<lingua>_<Titolo_con_underscore>.txt`, prima riga = titolo, poi testo in chiaro.
  Il nome contiene parole significative da cercare.
- **Licenza:** CC BY-SA, da citare nella relazione.

### 2.2 Corpus grande (`data/corpus/`, in costruzione)
Motivo: il professore ha osservato che molte relazioni usano pochi documenti; con più file si
valutano meglio i tempi e si può parlare di recall.
- **Script riproducibile:** `tools/build_corpus.py` (nel repository). Usa le API di Wikipedia
  (it/en), rispetta il limite di richieste (`Retry-After`), 3 richieste in parallelo, ripartenza
  senza riscaricare.
- **Composizione prevista (~2.800 file):** per lingua 14 argomenti × 80 articoli presi da
  categorie di Wikipedia (terremoti, battaglie della seconda guerra mondiale, guerre napoleoniche,
  squadre di calcio, teoremi, pittura, composti chimici, vulcani, fiumi, imperatori romani, malattie
  infettive, opera, aerei, linguaggi di programmazione) + 400 articoli casuali per lingua come
  "rumore". Scartati gli articoli sotto i 3000 caratteri.
- **Manifest** (`data/corpus-manifest.csv`): lingua, titolo, file, argomento di provenienza,
  lunghezza e categorie di Wikipedia di ogni articolo. Permette di costruire in seguito una verità
  tematica senza riscaricare.
- **Git:** il testo (`data/corpus/`) è escluso dal repository; si versionano script e manifest.
- **Da riportare nella relazione appena finito:** numero di file, MB, parole per lingua.
- Le categorie italiane "Battaglie della seconda guerra mondiale" e "Aerei da caccia" non
  esistono: usate "Battaglie e operazioni della seconda guerra mondiale" e "Aerei militari della
  seconda guerra mondiale".

## 3. Analyzer

Combinati con un `PerFieldAnalyzerWrapper` (un analyzer per campo, slide 28). Il default
(`StandardAnalyzer`) non viene mai usato: i campi sono solo due.

### 3.1 Prima versione (scartata) e problemi trovati
- `contenuto`: `StandardAnalyzer` + unione delle stopword italiane e inglesi.
- `nome`: `StandardTokenizer` + minuscolo + `WordDelimiterGraphFilter`.
- **Il confronto con `grep -ilw` ha mostrato file non trovati:**

| Termine | `grep` | Prima | Definitiva |
|---|---:|---:|---:|
| uomo | 19 | 10 | 19 |
| italia | 38 | 28 | 38 |
| impero | 10 | 5 | 10 |
| anno | 55 | 43 | 53 |
| king | 21 | 18 | 21 |
| citta (senza accento) | 34 | 0 | 34 |
| era | 95 | nessun termine utile | 95 |

  Recall della prima versione, ad esempio: `uomo` 10/19 = 0,53; `citta` 0/34 = 0.
- **Tre cause:** (1) lo `StandardTokenizer` non spezza sugli apostrofi (`dell'uomo`, `l'Italia`,
  `king's` restano un solo token); (2) gli accenti non sono normalizzati (`città` ≠ `citta`);
  (3) le stopword delle due lingue si scontrano: 27 stopword italiane sono anche parole inglesi
  (`era`, `dove`, `all`).
- **Nome:** `WordDelimiterGraphFilter` separa lettere e numeri → `nome example1` = `example` OR `1`
  e trovava 4 file, 3 spuri (`example2`, `en_2025_GN1`, `it_..._DHC_1_...`).
- **Lezione:** un analyzer si valuta confrontando i risultati con una verità indipendente (`grep`),
  non guardando i token di due frasi.

### 3.2 Versione definitiva
- **`nome`:** `CustomAnalyzer` = `PatternReplaceCharFilter` (`_` → spazio) + `StandardTokenizer` +
  `LowerCaseFilter` + `ASCIIFoldingFilter`. Senza stopword, stemming né `WordDelimiterGraphFilter`.
  `example1` resta un termine solo; `nome example` non lo trova più (compromesso dichiarato).
- **`contenuto`:** sottoclasse di `Analyzer` = `StandardTokenizer` + `ElisionFilter` (articoli
  italiani) + `EnglishPossessiveFilter` + `LowerCaseFilter` + `ASCIIFoldingFilter`.
  **Senza stopword e senza stemming** (corpus bilingue).
- **Perché una sottoclasse e non `CustomAnalyzer`:** `ElisionFilterFactory` vuole l'elenco degli
  articoli in un file di risorse, altrimenti usa quelli francesi; la sottoclasse passa il
  `CharArraySet` direttamente. Risultato identico (verificato).
- **Estensione `.txt`:** tolta prima di indicizzare il nome.
- **Limiti:** nessuno stemming (`libro` ≠ `libri`); elenco articoli incompleto (`quell'anno`
  7 volte non trattato → `anno` 53 contro 55); `contenuto il the` trova 199 file su 202.

### 3.3 Alternative sulle stopword (misurate)
Indice dei 202 file, miglior tempo di 5 prove nello stesso processo (senza avvio JVM):

| Variante | Parole eliminate | Indice | Tempo |
|---|---:|---:|---:|
| Nessuna (**scelta**) | 0 | 1299 KB | 148 ms |
| Solo articoli | 11 | 1261 KB | 132 ms |
| Selettiva (tutte tranne 8 ambigue) | 302 | 1133 KB | 124 ms |
| Completa (prima versione) | 310 | 1128 KB | 123 ms |

- Parole ambigue (frequenti nella lingua sbagliata): `all`, `i`, `era`, `ed`, `c`, `io`, `mi` + `dove`.
- Anche la selettiva toglie parole inglesi vere: `non` in 29 file inglesi, `per` in 22, `come` in 16
  (per `non-profit` il tokenizer produce `non`). Dipende dal corpus.
- **Riconoscimento della lingua** con conta di parole funzionali: 200/200 sul corpus piccolo, margine
  minimo 6 punti percentuali. Non adottato: in Lucene l'analyzer è per campo, servirebbero un campo
  per lingua o un `TokenStream` per documento e query analizzate con entrambi gli analyzer.
- **Decisione:** nessuna stopword (guadagno piccolo, costo = ricerca scorretta; BM25 pesa già poco
  i termini frequenti).
- Misure fatte con programmi temporanei (non nel repository): se servono nella relazione finale come
  riproducibili, vanno trasformate in uno script.

## 4. Indicizzazione (`FileIndexer`)
- Un `Document` per file con due campi: `nome` (`TextField`, `Store.YES`, per stamparlo) e
  `contenuto` (`TextField`, `Store.NO`, solo ricercabile).
- Visita ricorsiva; accetta `.txt` senza distinguere maiuscole. Indice ricreato da zero a ogni
  esecuzione (`OpenMode.CREATE`). File illeggibili saltati con avviso e contati.
- Stampa file indicizzati, saltati e millisecondi.

### Tempi misurati (corpus piccolo, 202 file)
| Analyzer | Esecuzioni (ms) | Media | Indice |
|---|---|---:|---:|
| Prima versione | 565, 466, 477 | 503 | 1,2 MB |
| Definitivo | 696, 691, 577, 489, 502 | 591 | 1,3 MB |

- Tempo comprensivo di lettura e analisi, in JVM appena avviata (senza warm-up).
- **Macchina:** MacBook Pro (`MacBookPro17,1`), Apple M1 8 core (4 prestazioni + 4 efficienza),
  8 GB RAM, macOS 26.6, OpenJDK 21.0.10.
- **Da fare:** misure su più dimensioni con `tools/benchmark_indexing.py` (grafico tempo/file).

## 5. Interfaccia da riga di comando (`Main`)
- `index <docsDir> <indexDir>` e `search <indexDir>`; argomenti controllati per comando; messaggio
  d'uso con esempi; promemoria della sintassi solo per `search`; errore chiaro se l'indice non si
  apre. Refactor fatto in `runIndex` / `runSearch`.
- Esecuzione: `./mvnw exec:java -Dexec.args="index data/docs index"` e `"search index"`
  (oppure `./index.sh`, `./search.sh`).

## 6. Costruzione della query (`QueryBuilder`)
- Sintassi: `nome <termini>` oppure `contenuto <termini>`, virgolette per le frasi.
- **`QueryParser` di Lucene** con il campo scelto come campo di default e l'analyzer per campo; a
  mano si estrae solo la parola chiave (`split(" ", 2)`, non distingue maiuscole).
- **Più termini: OR** (default del `QueryParser`, come nelle slide: `Occur.SHOULD`).
- Errori sempre come `IllegalArgumentException` con messaggio (campo sconosciuto, testo mancante,
  sintassi non valida).
- **Sintassi extra gratuita** (verificata sull'indice): `+guerra +mondiale` (AND, 19 file),
  `guerra -mondiale` (17), `+guerra mondiale` (36), `"world war"~5` (slop, 17 contro 16 della frase
  esatta), `napol*` (wildcard, 13), `guerra^5 mondiale` (boost). Vedi anche **sezione 9**.
- **Limiti:** `nome foo:bar` cerca nel campo `foo`; operatori e caratteri speciali di Lucene restano
  attivi. Alternativa non scelta: query costruite a mano (`TermQuery`, `PhraseQuery`,
  `BooleanQuery`): più controllo (AND/OR configurabile, sintassi limitata, boost per campo) ma più
  codice e analisi dei termini a carico nostro.
- Ranking: BM25 di default.

## 7. Ricerca (`FileSearcher`)
- Apre l'indice (`FSDirectory`, `DirectoryReader`, `IndexSearcher`); `AutoCloseable` usato con
  `try-with-resources`. **Top-K = 5**. Output: totale dei risultati (anche oltre il top-K) e, per
  file, nome, punteggio, posizione. Query senza termini utili: avviso e nessuna ricerca.
- Il contenuto non si può mostrare (non è memorizzato).
- Ciclo interattivo: prompt `> `, righe vuote ignorate, `exit`/`quit`, errori di input stampati senza
  uscire.

## 8. Test con 21 query
- File `queries.txt` (descrizioni incluse) eseguito da `./run-queries.sh`, che produce
  `results/query-results.md` (tabella riassuntiva + output di ogni query). Riproducibile.
- Per le query 5-11 e 14-18 il totale coincide con `grep -il`.
- **Risultati notevoli:** `guerra mondiale` (OR) 44 file contro `"world war"` 16; `nome list` 6 file
  ma ne mostra 5 (top-K); query 5 e 9 (`napoleone`/`NAPOLEONE`) identiche; `uomo` 19, `italia` 38,
  `citta` 34, `king` 21, `era` 95 (= `grep`); `nome example1` 1; `il the` 199; errori gestiti.

## 9. Valutazione con metriche (in corso)

Suggerimenti del professore: più documenti (tempi e recall), spiegare come è costruito il dataset,
usare metriche.

- **Verità lessicale (scelta attuale):** per un termine o una frase i file rilevanti sono quelli che
  lo contengono (calcolati indipendentemente da Lucene). Precision = trovati corretti / trovati;
  recall = trovati corretti / rilevanti. Permette di confrontare varianti (es. prima versione contro
  definitiva: recall `uomo` 0,53 → 1,00).
- **Verità tematica (se avanza tempo):** rilevanti = articoli di una categoria di Wikipedia (campo
  `source` e `categories` del manifest); metriche precision@5, recall, MAP.
- **Da decidere:** per calcolare la recall serve l'elenco completo dei risultati (la CLI stampa solo
  5). Ipotesi: nuova classe `Evaluator` che usa `QueryBuilder` e l'indice, senza toccare le altre.
- **Da fare:** corpus grande → indicizzare → tempi su 200/500/1000/2000/tutto → query con verità
  lessicale → tabelle precision/recall → aggiornare la relazione.

## 10. Punti ancora aperti
- Finire il download del corpus e riportare numeri e composizione nella relazione.
- Benchmark dei tempi e valutazione con precision/recall; aggiornare grafici e tabelle.
- Rifare le 21 query (o quelle nuove) sul corpus grande e rigenerare `results/query-results.md`.
- Aggiornare la relazione: dataset, metodo di valutazione, metriche.
- Facoltativo: test JUnit; query costruite a mano; completare l'elenco degli articoli italiani;
  commenti in lingua uniforme (`TODO.md`).
- Commit e push finali (URL nella relazione: https://github.com/adducec03/lucene-file-indexer).
