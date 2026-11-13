package it.homework.lucene;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

/**
 * Evaluates the search quality with precision, recall and F1 against a lexical ground truth.
 *
 * The ground truth ("oracle") does NOT use Lucene: every file is read, accents are removed,
 * the text is lowercased and cut on every character that is not a letter or a digit; a file is
 * relevant if it contains the query terms (any of them, like the default OR of the QueryParser)
 * or, for a quoted phrase, the words one after the other. This is what grep -iw would find.
 *
 * Two analyzer configurations are compared on the same queries and the same files:
 *   baseline: first version (StandardAnalyzer + Italian/English stopwords, WordDelimiterGraphFilter on names);
 *   final: the analyzers of the project (Analyzers.perField()).
 *
 * Usage: Evaluator [corpusDir=data/corpus] [outputFile=results/evaluation.md]
 */
public final class Evaluator {

    /** Queries in the same format of the console: "&lt;field&gt; &lt;text&gt;". */
    private static final List<String> QUERIES = List.of(
            "contenuto napoleone",
            "contenuto napoleon",
            "contenuto terremoto",
            "contenuto earthquake",
            "contenuto vulcano",
            "contenuto volcano",
            "contenuto \"world war\"",
            "contenuto \"seconda guerra mondiale\"",
            "contenuto \"roman empire\"",
            "contenuto \"machine learning\"",
            "contenuto impero",
            "contenuto uomo",
            "contenuto italia",
            "contenuto citta",
            "contenuto king",
            "contenuto era",
            "contenuto anno",
            "contenuto NAPOLEONE",
            "contenuto teorema theorem",
            "contenuto il the",
            "nome napoleone",
            "nome roma",
            "nome battle",
            "nome \"world war\"",
            "nome example1",
            "nome vesuvio");

    private record Doc(String name, List<String> tokens) {}

    private record Result(int relevant, int retrieved, int hits) {
        // nothing relevant and nothing retrieved is a correct answer: precision 1
        double precision() { return retrieved == 0 ? (relevant == 0 ? 1 : 0) : (double) hits / retrieved; }
        double recall() { return relevant == 0 ? 1 : (double) hits / relevant; }
        double f1() {
            double p = precision(), r = recall();
            return p + r == 0 ? 0 : 2 * p * r / (p + r);
        }
    }

    private Evaluator() {}

    public static void main(String[] args) throws Exception {
        Path corpus = Path.of(args.length > 0 ? args[0] : "data/corpus");
        Path output = Path.of(args.length > 1 ? args[1] : "results/evaluation.md");

        List<Path> files;
        try (Stream<Path> s = Files.walk(corpus)) {
            files = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                    .sorted().toList();
        }
        System.out.println("Reading " + files.size() + " files...");
        List<Doc> docs = new ArrayList<>();
        Map<String, String> texts = new HashMap<>();
        for (Path p : files) {
            String name = baseName(p);
            String text = Files.readString(p, StandardCharsets.UTF_8);
            texts.put(name, text);
            docs.add(new Doc(name, tokens(text)));
        }
        Map<String, List<String>> nameTokens = new HashMap<>();
        for (Doc d : docs) {
            nameTokens.put(d.name(), tokens(d.name()));
        }

        System.out.println("Indexing with both configurations...");
        try (Directory baselineDir = buildIndex(texts, baselineAnalyzer());
             Directory finalDir = buildIndex(texts, Analyzers.perField());
             DirectoryReader baselineReader = DirectoryReader.open(baselineDir);
             DirectoryReader finalReader = DirectoryReader.open(finalDir)) {

            IndexSearcher baselineSearcher = new IndexSearcher(baselineReader);
            IndexSearcher finalSearcher = new IndexSearcher(finalReader);
            QueryBuilder finalBuilder = new QueryBuilder();
            Analyzer baseline = baselineAnalyzer();

            StringBuilder md = new StringBuilder();
            md.append("| Query | Relevant | Baseline retrieved | P | R | F1 | Final retrieved | P | R | F1 |\n");
            md.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
            double[] sumBase = new double[3];
            double[] sumFinal = new double[3];
            StringBuilder diagnostics = new StringBuilder();
            // the relevant files of every query, to be cross-checked with grep (tools/crosscheck_oracle.py)
            StringBuilder oracleSets = new StringBuilder();

            for (String q : QUERIES) {
                String field = q.substring(0, q.indexOf(' '));
                String text = q.substring(q.indexOf(' ') + 1);

                Set<String> relevant = oracle(field, text, docs, nameTokens);
                oracleSets.append(q).append('\t').append(relevant.size()).append('\t')
                        .append(String.join(";", relevant.stream().sorted().toList())).append('\n');
                Set<String> baseHits = retrieve(baselineSearcher, parseWith(baseline, field, text));
                Set<String> finalHits = retrieve(finalSearcher, finalBuilder.parse(q));

                Result rb = score(relevant, baseHits);
                Result rf = score(relevant, finalHits);
                add(sumBase, rb);
                add(sumFinal, rf);
                md.append(String.format(Locale.ROOT, "| `%s` | %d | %d | %.2f | %.2f | %.2f | %d | %.2f | %.2f | %.2f |%n",
                        q, relevant.size(), rb.retrieved(), rb.precision(), rb.recall(), rb.f1(),
                        rf.retrieved(), rf.precision(), rf.recall(), rf.f1()));
                if (rf.hits() != relevant.size() || rf.retrieved() != rf.hits()) {
                    diagnostics.append(String.format("%s: final misses %s; spurious %s%n", q,
                            first(difference(relevant, finalHits)), first(difference(finalHits, relevant))));
                }
            }
            int n = QUERIES.size();
            md.append(String.format(Locale.ROOT, "| **Macro average** | | | %.2f | %.2f | %.2f | | %.2f | %.2f | %.2f |%n",
                    sumBase[0] / n, sumBase[1] / n, sumBase[2] / n,
                    sumFinal[0] / n, sumFinal[1] / n, sumFinal[2] / n));

            Files.createDirectories(output.toAbsolutePath().getParent());
            Files.writeString(output, md.toString(), StandardCharsets.UTF_8);
            Files.writeString(output.resolveSibling("oracle-sets.tsv"), oracleSets.toString(), StandardCharsets.UTF_8);
            System.out.println(md);
            if (diagnostics.length() > 0) {
                System.out.println("Differences of the final version from the oracle:");
                System.out.println(diagnostics);
            }
            System.out.println("saved " + output);
        }
    }

    // ---------------------------------------------------------------- oracle (no Lucene)

    /** Lowercase, no accents, split on everything that is not a letter or a digit. */
    static List<String> tokens(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(n.split("[^\\p{L}\\p{N}]+")).filter(t -> !t.isEmpty()).toList();
    }

    private static final Pattern CLAUSE = Pattern.compile("\"([^\"]+)\"|(\\S+)");

    private static Set<String> oracle(String field, String text, List<Doc> docs, Map<String, List<String>> nameTokens) {
        List<List<String>> clauses = new ArrayList<>();
        Matcher m = CLAUSE.matcher(text);
        while (m.find()) {
            clauses.add(tokens(m.group(1) != null ? m.group(1) : m.group(2)));
        }
        Set<String> result = new HashSet<>();
        for (Doc d : docs) {
            List<String> toks = field.equals(Analyzers.FIELD_NAME) ? nameTokens.get(d.name()) : d.tokens();
            for (List<String> clause : clauses) {
                if (!clause.isEmpty() && contains(toks, clause)) {
                    result.add(d.name());
                    break;
                }
            }
        }
        return result;
    }

    /** True if the words of the phrase appear consecutively in the token list. */
    private static boolean contains(List<String> toks, List<String> phrase) {
        int n = phrase.size();
        outer:
        for (int i = 0; i + n <= toks.size(); i++) {
            for (int j = 0; j < n; j++) {
                if (!toks.get(i + j).equals(phrase.get(j))) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- Lucene side

    private static Analyzer baselineAnalyzer() throws IOException {
        CharArraySet stop = new CharArraySet(100, true);
        stop.addAll(EnglishAnalyzer.getDefaultStopSet());
        stop.addAll(ItalianAnalyzer.getDefaultStopSet());
        Analyzer name = CustomAnalyzer.builder()
                .withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                .build();
        return new PerFieldAnalyzerWrapper(new StandardAnalyzer(),
                Map.of(Analyzers.FIELD_NAME, name, Analyzers.FIELD_CONTENT, new StandardAnalyzer(stop)));
    }

    private static Directory buildIndex(Map<String, String> texts, Analyzer analyzer) throws IOException {
        Directory dir = new ByteBuffersDirectory();
        try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(analyzer))) {
            for (Map.Entry<String, String> e : texts.entrySet()) {
                Document doc = new Document();
                doc.add(new TextField(Analyzers.FIELD_NAME, e.getKey(), Field.Store.YES));
                doc.add(new TextField(Analyzers.FIELD_CONTENT, e.getValue(), Field.Store.NO));
                writer.addDocument(doc);
            }
        }
        return dir;
    }

    private static Query parseWith(Analyzer analyzer, String field, String text) throws ParseException {
        return new QueryParser(field, analyzer).parse(text);
    }

    /** All the matching documents (not only the first k), identified by file name. */
    private static Set<String> retrieve(IndexSearcher searcher, Query query) throws IOException {
        int max = Math.max(1, searcher.getIndexReader().numDocs());
        Set<String> names = new HashSet<>();
        for (ScoreDoc sd : searcher.search(query, max).scoreDocs) {
            names.add(searcher.storedFields().document(sd.doc).get(Analyzers.FIELD_NAME));
        }
        return names;
    }

    // ---------------------------------------------------------------- metrics

    private static Result score(Set<String> relevant, Set<String> retrieved) {
        int hits = 0;
        for (String r : retrieved) {
            if (relevant.contains(r)) {
                hits++;
            }
        }
        return new Result(relevant.size(), retrieved.size(), hits);
    }

    private static void add(double[] sum, Result r) {
        sum[0] += r.precision();
        sum[1] += r.recall();
        sum[2] += r.f1();
    }

    private static Set<String> difference(Set<String> a, Set<String> b) {
        Set<String> d = new HashSet<>(a);
        d.removeAll(b);
        return d;
    }

    private static List<String> first(Set<String> s) {
        return s.stream().sorted().limit(3).toList();
    }

    private static String baseName(Path p) {
        String n = p.getFileName().toString();
        return n.substring(0, n.lastIndexOf('.'));
    }
}
