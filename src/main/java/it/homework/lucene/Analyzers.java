package it.homework.lucene;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

/**
 * Analyzer choices for the two indexed fields.
 * TODO: decide and justify the analyzers (needed for the report).
 */
public final class Analyzers {

    public static final String FIELD_NAME = "nome";
    public static final String FIELD_CONTENT = "contenuto";

    private Analyzers() {}

    /** TODO: build a PerFieldAnalyzerWrapper with one analyzer per field. */
    public static Analyzer perField() throws IOException{
        Map<String,Analyzer> perFieldAnalyzers = new HashMap<>();
        CharArraySet stopWords = new CharArraySet(ItalianAnalyzer.getDefaultStopSet(), true);
        stopWords.addAll(EnglishAnalyzer.getDefaultStopSet());

        Analyzer nameAnalyzer = CustomAnalyzer.builder()
                    .withTokenizer(StandardTokenizerFactory.class)
                    .addTokenFilter(LowerCaseFilterFactory.class)
                    .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                    .build();


        perFieldAnalyzers.put(FIELD_NAME, nameAnalyzer);
        perFieldAnalyzers.put(FIELD_CONTENT, new StandardAnalyzer(stopWords));
        
        Analyzer perFiAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), perFieldAnalyzers);
        return perFiAnalyzer;

    }
}
