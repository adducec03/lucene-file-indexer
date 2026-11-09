package it.homework.lucene;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

import org.apache.lucene.search.Query;

/**
 * Entry point.
 *   index  &lt;docsDir&gt; &lt;indexDir&gt;  -> build the index
 *   search &lt;indexDir&gt;               -> interactive query loop on console
 */
public class Main {

    private final static int TOP_K = 5;

    private static void runIndex(Path docsDir, Path indexDir) throws Exception{
        // crea l'indexer e gli passa docsDir e indexDir
        try {
            FileIndexer indexer = new FileIndexer();
            indexer.index(docsDir, indexDir);
        } catch (IOException e) {
            System.err.println("cannot index " + docsDir + ": " + e.getMessage());
        }
    }
    
    private static void runSearch(Path indexDir) throws Exception{
        
        QueryBuilder queryBuilder = new QueryBuilder();

        try (FileSearcher searcher = new FileSearcher(indexDir);
            Scanner in = new Scanner(System.in)) {

            System.out.println("Lucene file searcher - index: " + indexDir);
            System.out.println("Query syntax:  <field> <terms>");
            System.out.println("  fields:      nome (file name) | contenuto (file content)");
            System.out.println("  examples:    nome relazione");
            System.out.println("               contenuto data scientist      (files with any of the words)");
            System.out.println("               contenuto \"data scientist\"    (exact phrase)");
            System.out.println("Shows the top " + TOP_K + " results. Type 'exit' or 'quit' to leave.");

            while (true) {

                System.out.print("> ");
                if (!in.hasNextLine()){
                    break;
                }

                String line = in.nextLine().trim();
                if(line.isEmpty()){
                    continue;
                }

                if(line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")){
                    break;
                }
                
                try {
                    Query query = queryBuilder.parse(line);
                    searcher.search(query, TOP_K);
                } catch (IllegalArgumentException e) {
                    System.err.println("Error: " + e.getMessage());
                }
            
            }
            
        } catch (IOException e) {
            System.err.println("Cannot open index in " + indexDir + ": " + e.getMessage());
        }
    }
    

    public static void main(String[] args) throws Exception {

        // se chiama index
        if (args.length == 3 && args[0].equals("index")){
            // costruisce i path a partire dagli argomenti
            Path docsDir = Path.of(args[1]);
            Path indexDir = Path.of(args[2]);
            runIndex(docsDir,indexDir);
        }
        // se chiama search
        else if (args.length == 2 && args[0].equals("search")) {
            // costruisce il path a partire dagli argomenti
            Path indexDir = Path.of(args[1]);
            runSearch(indexDir);
        }
        // altrimenti se il comando è sbagliato stampa usage
        else{
            System.out.println("Lucene file indexer");
            System.out.println("Usage:");
            System.out.println("  index  <docsDir> <indexDir>   index the .txt files found in <docsDir> into <indexDir>");
            System.out.println("  search <indexDir>             query an existing index interactively");
            System.out.println("Example:");
            System.out.println("  ./mvnw exec:java -Dexec.args=\"index data/docs index\"");
            System.out.println("  ./mvnw exec:java -Dexec.args=\"search index\"");
        }


    }
}
