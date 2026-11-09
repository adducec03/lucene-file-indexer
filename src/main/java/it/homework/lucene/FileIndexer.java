package it.homework.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Indexes the .txt files of a directory into a Lucene index.
 * One Lucene Document is created per file, with two fields: the file name
 * (without extension) and the file content. The index is rebuilt from scratch
 * at every run.
 */
public class FileIndexer {

    /**
     * Indexes every .txt file found (recursively) under docsDir into indexDir,
     * then prints the number of indexed files and the elapsed time.
     */

    // se il percorso non è valido lancia eccezione
    public void index(Path docsDir, Path indexDir) throws IOException {
        if (!Files.isDirectory(docsDir)) {
            throw new IOException("Not a directory: " + docsDir);
        }

        // variabile per il tempo di indicizzazione
        long start = System.nanoTime();
        // variabili per contatore
        int indexed = 0;
        int skipped = 0;
        
        // CONFIGURAZIONE WRITER
        // dice a lucene come trattare il testo
        IndexWriterConfig config = new IndexWriterConfig(Analyzers.perField());
        // CREATE fa ripartire l'indicizzazione da 0
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        // APERTURA INDICE
        // apre o crea la cartella dei file di lucene
        try (Directory directory = FSDirectory.open(indexDir);
            // aggiunge i documenti al writer
             IndexWriter writer = new IndexWriter(directory, config)) {

            // RICERCA DEI FILE .TXT
            List<Path> txtFiles;
            // Files.walk() produce tutti i percorsi sotto docsdir
            try (Stream<Path> walk = Files.walk(docsDir)) {
                txtFiles = walk
                        .filter(Files::isRegularFile)       // filtro che scarta le cartelle
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".txt"))     // filtro che mantiene solo i file .txt
                        .toList();      // mette tutti i nomi dei file in una lista (possiamo usare un for per iterare)
            }

            // CREAZIONE DEI DOCUMENTI
            // per ogni file
            for (Path file : txtFiles) {
                try {
                    // legge il contenuto dei file in UTF-8
                    String content = Files.readString(file);
                    // si crea un documento per ogni file
                    Document doc = new Document();
                    // il nome del file viene conservato nell'indice (senza estensione)
                    doc.add(new TextField(Analyzers.FIELD_NAME, nameWithoutExtension(file), Field.Store.YES));
                    // il contenuto del file viene analizzato e reso ricercabile ma non viene salvato nell'indice
                    doc.add(new TextField(Analyzers.FIELD_CONTENT, content, Field.Store.NO));
                    // passa il ducumento a lucene
                    writer.addDocument(doc);
                    indexed++;
                } catch (IOException e) {
                    // eccezione che viene catturata se la lettura fallisce
                    // ad esempio file non codificato in UTF-8
                    System.err.println("Skipped " + file + ": " + e.getMessage());
                    // tiene conto del numero i file skippati
                    skipped++;
                }
            }
            // COMMIT 
            // scrive su disco l'indice
            writer.commit();
        }

        // REPORT
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("Indexed %d files (%d skipped) in %d ms%n", indexed, skipped, elapsedMs);
    }

    private static String nameWithoutExtension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
