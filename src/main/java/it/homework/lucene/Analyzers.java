package it.homework.lucene;

import org.apache.lucene.analysis.Analyzer;

/**
 * Analyzer choices for the two indexed fields.
 * TODO: decide and justify the analyzers (needed for the report).
 */
public final class Analyzers {

    public static final String FIELD_NAME = "nome";
    public static final String FIELD_CONTENT = "contenuto";

    private Analyzers() {}

    /** TODO: build a PerFieldAnalyzerWrapper with one analyzer per field. */
    public static Analyzer perField() {
        throw new UnsupportedOperationException("TODO");
    }
}
