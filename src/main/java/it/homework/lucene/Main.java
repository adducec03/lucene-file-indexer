package it.homework.lucene;

import java.nio.file.Path;

/**
 * Entry point.
 *   index  &lt;docsDir&gt; &lt;indexDir&gt;  -> build the index
 *   search &lt;indexDir&gt;               -> interactive query loop on console
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // se chiama index
        if (args.length == 3 && args[0].equals("index")){
            // costruisce i path a partire dagli argomenti
            Path docsDir = Path.of(args[1]);
            Path indexDir = Path.of(args[2]);

            // crea l'indexer e gli passa docsDir e indexDir
            FileIndexer indexer = new FileIndexer();
            indexer.index(docsDir, indexDir);
        }
        // se chiama search
        else if (args.length == 2 && args[0].equals("search")) {
            // TODO
        }
        // altrimenti se il comando è sbagliato stampa usage
        else{
            System.out.println("Usage: index <docsDir> <indexDir> | search <indexDir>");
        }


    }
}
