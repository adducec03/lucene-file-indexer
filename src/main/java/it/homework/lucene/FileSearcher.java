package it.homework.lucene;

import java.io.IOException;
import java.nio.file.Path;

/** Runs queries against the Lucene index and prints the results. */
public class FileSearcher {

    public FileSearcher(Path indexDir) throws IOException {
        // TODO: open DirectoryReader + IndexSearcher
    }

    /** TODO: execute the query and print rank, score, file name. */
    public void search(org.apache.lucene.search.Query query, int topK) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }
}
