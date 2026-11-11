package it.homework.lucene;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.util.ElisionFilter;

/**
 * Analyzer dei due campi indicizzati (analyzer = tokenizer + catena di token filter).
 * Gli stessi analyzer sono usati sia in indicizzazione sia nell'analisi della query.
 *
 * <ul>
 *   <li><b>nome</b>: "_" diventa uno spazio (lo standard tokenizer terrebbe
 *       "Relazione_Finale" come un unico token), poi standard tokenizer, minuscolo e
 *       normalizzazione degli accenti. Niente stopword, niente stemming. Lettere e
 *       numeri non vengono separati, quindi "example1" resta un unico termine.</li>
 *   <li><b>contenuto</b>: il corpus è bilingue (italiano + inglese), quindi niente
 *       stemming e nessuna lista di stopword (le liste delle due lingue si scontrano,
 *       ad esempio "era" o "dove" in italiano sono parole comuni inglesi). La catena
 *       rimuove le elisioni italiane ("dell'uomo" -&gt; "uomo") e il possessivo inglese
 *       ("dog's" -&gt; "dog"), porta in minuscolo e normalizza gli accenti
 *       ("citta" = "città").</li>
 * </ul>
 */
public final class Analyzers {

    public static final String FIELD_NAME = "nome";
    public static final String FIELD_CONTENT = "contenuto";

    /** Articoli e preposizioni italiane che si elidono davanti a una vocale (l'uomo, dell'anno, ...). */
    private static final CharArraySet ITALIAN_ARTICLES = new CharArraySet(Arrays.asList(
            "c", "l", "all", "dall", "dell", "nell", "sull", "coll", "pell",
            "gl", "agl", "dagl", "degl", "negl", "sugl", "un", "m", "t", "s", "v", "d"), true);

    private Analyzers() {}

    public static Analyzer perField() throws IOException {
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
        perFieldAnalyzers.put(FIELD_NAME, nameAnalyzer());
        perFieldAnalyzers.put(FIELD_CONTENT, contentAnalyzer());

        // L'analyzer di default non viene mai usato: i campi sono solo i due sopra.
        return new PerFieldAnalyzerWrapper(new StandardAnalyzer(), perFieldAnalyzers);
    }

    /** Nomi dei file: i separatori ('_', '-', '.', ' ') dividono le parole; niente stopword né stemming. */
    private static Analyzer nameAnalyzer() throws IOException {
        return CustomAnalyzer.builder()
                // CharFilter: lavora sul testo PRIMA del tokenizer. Sostituisce "_" con uno spazio,
                // perché lo standard tokenizer non spezza su "_" (es. "Relazione_Finale" -> "Relazione Finale")
                .addCharFilter(PatternReplaceCharFilterFactory.class, "pattern", "_", "replacement", " ")
                // Tokenizer: divide il testo in token sui confini di parola (standard Unicode UAX#29):
                // spezza su spazi, trattini e punti e scarta la punteggiatura
                .withTokenizer(StandardTokenizerFactory.class)
                // Token filter: porta ogni token in minuscolo, così la ricerca ignora le maiuscole
                .addTokenFilter(LowerCaseFilterFactory.class)
                // Token filter: converte i caratteri accentati nel carattere base ASCII
                // (es. "città" -> "citta"), così si trova il termine anche scritto senza accento
                .addTokenFilter(ASCIIFoldingFilterFactory.class)
                .build();
    }

    /** Testo bilingue: rimuove elisioni italiane e possessivi inglesi; niente stemming né stopword. */
    private static Analyzer contentAnalyzer() {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                // Tokenizer: divide il testo in token sui confini di parola (standard Unicode UAX#29)
                // e scarta la punteggiatura. Non spezza però sugli apostrofi ("dell'uomo" resta un token)
                Tokenizer source = new StandardTokenizer();
                // Token filter: toglie l'articolo davanti all'apostrofo (elisione), solo se è nell'elenco
                // ITALIAN_ARTICLES (es. "dell'uomo" -> "uomo", "l'Italia" -> "Italia")
                TokenStream chain = new ElisionFilter(source, ITALIAN_ARTICLES);
                // Token filter: toglie il possessivo inglese "'s" (es. "dog's" -> "dog")
                chain = new EnglishPossessiveFilter(chain);
                // Token filter: porta ogni token in minuscolo, così la ricerca ignora le maiuscole
                chain = new LowerCaseFilter(chain);
                // Token filter: converte i caratteri accentati nel carattere base ASCII
                // (es. "città" -> "citta"), così si trova il termine anche scritto senza accento
                chain = new ASCIIFoldingFilter(chain);
                // l'ordine conta: i filtri si applicano nell'ordine in cui sono concatenati
                return new TokenStreamComponents(source, chain);
            }
        };
    }
}
