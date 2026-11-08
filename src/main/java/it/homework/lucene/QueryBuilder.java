package it.homework.lucene;

import org.apache.lucene.search.Query;

/**
 * Translates the console syntax into a Lucene Query, e.g.
 *   nome data engineer
 *   contenuto "data scientist"
 * TODO: parse the field keyword, then terms / quoted phrases
 * (TermQuery/BooleanQuery for terms, PhraseQuery for quoted text).
 */
public class QueryBuilder {

    public Query parse(String input) {
        throw new UnsupportedOperationException("TODO");
    }
}
