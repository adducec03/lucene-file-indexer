# Note per la relazione

File di appunti: raccoglie le scelte fatte durante lo sviluppo, con le motivazioni,
da riutilizzare nella relazione finale. Si aggiorna man mano.
Stato: **in corso** (indicizzazione e ricerca implementate; mancano le query di test e la relazione).

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
- `search <indexDir>` avvia il ciclo di ricerca interattivo (vedi sezione 7).
- Il numero di argomenti viene controllato per ciascun comando (3 per `index`, 2 per `search`);
  negli altri casi stampa un messaggio d'uso con la descrizione dei comandi e due esempi.
- All'avvio di `search`, dopo l'apertura dell'indice, stampa un breve promemoria della sintassi
  (campi `nome` / `contenuto`, esempi con più termini e con frase tra virgolette, numero di
  risultati mostrati, come uscire). Il promemoria compare solo per `search`, non per `index`.
- Se l'indice non si può aprire (cartella inesistente o senza indice) stampa un messaggio
  di errore chiaro invece di uno stack trace.
- Esecuzione: `./mvnw exec:java -Dexec.args="index data/docs index"` e
  `./mvnw exec:java -Dexec.args="search index"`.

## 6. Costruzione della query (`QueryBuilder`): fatto

- **Sintassi della query:** `nome <termini>` oppure `contenuto <termini>`,
  con le virgolette per le frasi (richiesto dalla consegna).
- **Parsing:** si usa il `QueryParser` di Lucene. A mano si estrae solo la parola chiave
  iniziale (`split(" ", 2)`: campo + resto della riga); il resto viene passato al parser con
  il campo scelto come **campo di default** e l'analyzer per campo (`Analyzers.perField()`).
  Così i termini della query vengono analizzati come quelli dell'indice (minuscole, stopword,
  separatori), altrimenti non coinciderebbero con i termini indicizzati.
  Valutare più avanti un parsing manuale se questa scelta mostra dei limiti.
- **Parola chiave:** non distingue maiuscole/minuscole (`Nome`, `NOME` valgono come `nome`).
- **Più termini senza virgolette: OR.** È il comportamento standard del `QueryParser`
  e coincide con le slide (termini senza modificatori "optional", `Occur.SHOULD`).
  I documenti che contengono più termini ottengono un punteggio più alto.
- **Frasi:** il testo tra virgolette diventa una phrase query.
- **Errori di input:** il metodo lancia sempre `IllegalArgumentException` con un messaggio
  chiaro, che il chiamante (ciclo di ricerca) stampa senza chiudere il programma:
  campo sconosciuto, testo della query mancante, sintassi non valida (es. virgolette non chiuse).
- **Ranking:** similarity di default di Lucene (BM25), non modificata.

### Verifica (output reale di `query.toString()`)
| Riga digitata | Query Lucene |
|---|---|
| `nome roma` | `nome:roma` |
| `contenuto data scientist` | `contenuto:data contenuto:scientist` |
| `contenuto "data scientist"` | `contenuto:"data scientist"` |
| `contenuto Data` | `contenuto:data` |
| `nome Relazione_Finale` | `nome:relazione nome:finale` |
| `titolo qualcosa` | errore: campo sconosciuto |
| `contenuto` | errore: manca il testo della query |
| `contenuto "frase aperta` | errore: sintassi non valida |

### Limiti noti (da citare nella relazione)
- Se tutti i termini sono stopword o simboli (es. `contenuto il the`, `contenuto c++`) l'analyzer
  li elimina e la query risulta **vuota**: non dà errore ma non trova nulla. La ricerca dovrà
  segnalarlo all'utente.
- Il `QueryParser` accetta la sintassi `campo:valore`, quindi `nome foo:bar` cerca `bar` nel campo
  `foo` (inesistente) e non trova nulla. Anche gli operatori di Lucene (`+`, `-`, `AND`, `OR`,
  `*`, `?`) restano attivi: i caratteri speciali non vengono neutralizzati.
- Nessuno stemming: cercare `libro` non trova `libri`.

## 7. Ricerca (`FileSearcher`) e ciclo in `Main`: fatto

### `FileSearcher`
- Apre l'indice in lettura (`FSDirectory` + `DirectoryReader` + `IndexSearcher`) e controlla
  prima che la cartella esista.
- Implementa `AutoCloseable` (`close()` chiude reader e directory) ed è usato con
  `try-with-resources` nel `Main`: viene chiuso una volta sola a fine sessione, non a ogni query.
- **Top-K = 5** risultati (costante `TOP_K` in `Main`). Scelta pratica: con 200 file e query
  su una parola, pochi risultati bastano per verificare il ranking.
- **Output:** numero totale di risultati trovati (anche oltre il top-K) e, per ogni file,
  nome, punteggio e posizione in classifica. Con zero risultati stampa solo il totale.
- Il nome viene letto dal campo stored `nome`; il contenuto non si può stampare perché
  non è salvato nell'indice (scelta della sezione 4).
- **Query senza termini utili** (solo stopword o simboli): stampa un avviso su `stderr`
  e non esegue la ricerca.
- **Ranking:** punteggio di default di Lucene (BM25), non modificato.

### Ciclo interattivo in `Main`
- Prompt `> `, lettura di una riga alla volta con `Scanner`.
- Righe vuote ignorate; `exit` o `quit` (anche in maiuscolo) terminano il programma;
  anche la fine dell'input (Ctrl+D) termina il ciclo.
- Gli errori di input (`IllegalArgumentException` del `QueryBuilder`) vengono stampati
  come `Error: ...` e il ciclo continua.

## 8. Come testare

1. Costruire l'indice: `./mvnw exec:java -Dexec.args="index data/docs index"`
   (atteso: 202 file indicizzati, 0 saltati).
2. Avviare la ricerca: `./mvnw exec:java -Dexec.args="search index"`.
3. Verificare i risultati con `grep` sui file originali, ad esempio
   `grep -il "parola" data/docs/*.txt` (il numero di file deve coincidere con il totale
   stampato). Attenzione: il confronto può differire per le stopword e per le forme diverse
   della stessa parola (nessuno stemming).
4. Per rieseguire una serie di query senza digitarle:
   `printf 'nome roma\nexit\n' | ./mvnw -q exec:java -Dexec.args="search index"`.

Nota: all'avvio compaiono avvisi `WARNING` di Java/Lucene sul modulo vettoriale: sono innocui.

### Query di test (da completare dopo l'esecuzione)
| # | Query | Risultati | Primo file | Commento |
|---|---|---|---|---|
| 1 | | | | |

Casi da coprire: un termine su `nome`; un termine su `contenuto`; più termini (OR);
frase tra virgolette; maiuscole diverse; parola in italiano e in inglese; nessun risultato;
solo stopword; campo sconosciuto; errore di sintassi.

## 9. Punti ancora aperti

- Eseguire una decina di query di test e compilare la tabella della sezione 8.
- Refactor di `Main` (due metodi, uno per `index` e uno per `search`), se fatto.
- URL del repository GitHub.
- Modello di computer usato per le misure dei tempi.
