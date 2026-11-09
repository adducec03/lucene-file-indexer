package it.homework.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class FileSearcher implements AutoCloseable {

    private final IndexReader reader;
    private final IndexSearcher searcher;
    private final Directory directory;

    public FileSearcher(Path indexDir) throws IOException {
        // se la directory è vuota allora lancia eccezione
        if (!Files.isDirectory(indexDir)) {
            throw new IOException("Not a directory: " + indexDir);
        }
        directory = FSDirectory.open(indexDir);
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
    }

    public void search(Query query, int topK) throws IOException {
        // se la query è vuota (es. tutte stopwords) allora ritorna
        if (query.toString().isEmpty()){
            System.err.println("no useful term in the query");
            return;
        }

        // prende i primi k risultati della query
        TopDocs hits = searcher.search(query, topK);
        // numero di risultati
        Long totalHits = hits.totalHits.value();
        System.out.println("total number of results: " + totalHits);
        // se il numero di risultati è 0 allora non c'è bisogno di iterare
        if (totalHits == 0){
            return;
        }

        // variabile usata per leggere gli stored fields
        StoredFields storedFields = searcher.storedFields();
        // itera su tutti i documenti trovati (hits)
        for(int i = 0; i < hits.scoreDocs.length; i++){
            // position corrisponde alla posizione in classifica
            Integer position = i+1;
            // punteggio del documento
            ScoreDoc scoreDoc = hits.scoreDocs[i];
            Document doc = storedFields.document(scoreDoc.doc);

            // stampe
            System.out.println("name: " + doc.get(Analyzers.FIELD_NAME) + "; " +
                                "score: " + scoreDoc.score + "; " +
                                "position: " + position);
        }
    }

    /** Releases the index reader and the directory. Call it when done searching. */
    @Override
    public void close() throws IOException {
        try {
            reader.close();
        } finally {
            directory.close();
        }
    }
}
