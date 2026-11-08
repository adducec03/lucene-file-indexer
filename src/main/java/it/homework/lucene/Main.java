package it.homework.lucene;

/**
 * Entry point.
 *   index  &lt;docsDir&gt; &lt;indexDir&gt;  -> build the index
 *   search &lt;indexDir&gt;               -> interactive query loop on console
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // TODO: dispatch on args[0] ("index" | "search")
        System.out.println("Usage: index <docsDir> <indexDir> | search <indexDir>");
    }
}
