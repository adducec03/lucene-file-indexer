package it.homework.lucene;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * Translates the console syntax into a Lucene Query, e.g.
 *   nome roma
 *   contenuto data scientist
 *   contenuto "data scientist"
 *
 * The first word selects the field (nome | contenuto); the rest is parsed by
 * Lucene's QueryParser with that field as default and the per-field analyzer.
 * Terms without modifiers are optional (OR); quoted text is a phrase query.
 *
 * Invalid input is reported with an IllegalArgumentException.
 */
public class QueryBuilder {
    private final Analyzer analyzer;

    public QueryBuilder() throws IOException {
        analyzer = Analyzers.perField();
    }

    public Query parse(String input) {
        // trim rimuove eventuali spazi bianchi all'inizio/fine della stringa
        String line = input.trim();
        // divide la stringa in due parti [campo, testo]
        String[] parts = line.split(" ", 2);

        String field = parts[0].toLowerCase();
        // se il campo field non corrisponde ne a nome ne a contenuto allora lancia eccezione
        if (!field.equals(Analyzers.FIELD_NAME) && !field.equals(Analyzers.FIELD_CONTENT)) {
            throw new IllegalArgumentException(
                    "unknown field, use '" + Analyzers.FIELD_NAME + "' or '" + Analyzers.FIELD_CONTENT + "'");
        }
        // se non è presente il testo lancia un'eccezione
        if (parts.length != 2 || parts[1].isBlank()) {
            throw new IllegalArgumentException("missing query text after '" + field + "'");
        }

        // crea un parser sulla base del campo e dell'analyzer
        QueryParser parser = new QueryParser(field, analyzer);
        try {
            // fa il parsing sul testo
            return parser.parse(parts[1]);
        } catch (ParseException e) {
            // se c'è un errore lancia un'eccezione
            throw new IllegalArgumentException("invalid syntax: " + e.getMessage());
        }
    }
}
