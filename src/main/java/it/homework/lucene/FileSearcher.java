package it.homework.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/** Runs queries against the Lucene index and prints the results. */
public class FileSearcher {

    private IndexReader reader;
    private IndexSearcher searcher;

    public FileSearcher(Path indexDir) throws IOException {
        if (!Files.isDirectory(indexDir)) {
            throw new IOException("Not a directory: " + indexDir);
        }
        Directory directory = FSDirectory.open(indexDir);
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
    }

    /** TODO: execute the query and print rank, score, file name. */
    public void search(Query query, int topK) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }
}
