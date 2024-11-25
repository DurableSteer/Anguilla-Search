package de.fernunihagen.dbis.anguillasearch.index;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Arrays;
import java.util.Collections;

import de.fernunihagen.dbis.anguillasearch.helpers.Site;
import de.fernunihagen.dbis.anguillasearch.helpers.Token;
import de.fernunihagen.dbis.anguillasearch.helpers.VectorSite;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class VectorIndex {
    private TreeMap<String, Integer> documentIndex = new TreeMap<>();
    private HashMap<String, Integer> tokenIndex = new HashMap<>();
    private ArrayList<ArrayList<Double>> matrix = new ArrayList<>();
    private TreeMap<Integer, Integer> docsPerTokIndex = new TreeMap<>();

    private List<VectorSite> forwardIndex = new LinkedList<>();

    private List<String> specialCharacters;
    private List<String> stopwords;
    private StanfordCoreNLP pipeline;
    private double totalDocCount = 0;

    private void init() {
        this.specialCharacters = Arrays.asList(":", ",", ".", "!", "|", "&", "'", "[", "]", "?", "-", "–",
                "_", "/", "\\", "{", "}", "@", "^", "(", ")", "<", ">", "\"");

        this.stopwords = Arrays.asList("'s", "in", "further", "myself", "than", "below",
                "to", "should", "yours", "for", "have", "before", "my", "nor", "not", "s", "now", "their", "no",
                "against", "under", "if", "does", "during", "herself", "him", "same", "been", "other", "will",
                "who",
                "these", "themselves", "are", "how", "while", "is", "himself", "some", "an", "then", "a", "had",
                "can",
                "each", "me", "your", "his", "being", "above", "but", "it", "between", "by", "do", "its", "too",
                "only",
                "did", "up", "be", "this", "through", "down", "there", "her", "them", "so", "our", "he", "about",
                "they", "hers", "itself", "again", "as", "were", "when", "own", "until", "very", "theirs", "whom",
                "you", "any", "once", "because", "few", "or", "more", "don", "here", "t", "what", "with", "into",
                "from", "after", "has", "am", "ourselves", "out", "having", "at", "that", "all", "yourselves",
                "just",
                "over", "the", "such", "those", "yourself", "which", "where", "doing", "and", "of", "ours", "was",
                "most", "i", "she", "we", "why", "off", "on", "both");
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        pipeline = new StanfordCoreNLP(props);
    }

    public VectorIndex() {
        init();
    }

    // expects no duplicates in the sites.
    public void addSite(Site site) {
        TreeSet<Token> tokens = new TreeSet<>();

        DocInfo docInfo = new DocInfo();
        // Convert site content into one lowercase string.
        StringBuilder siteContent = new StringBuilder(site.title.toLowerCase());
        for (String heading : site.headings)
            siteContent.append(heading.toLowerCase());
        siteContent.append(site.paragraphs.toLowerCase());

        // Tokenize and lemmatize the sites.
        CoreDocument content = new CoreDocument(siteContent.toString());
        pipeline.annotate(content);

        for (CoreLabel tok : content.tokens()) {
            String lemma = tok.lemma();
            // Filter out stopwords and special characters.
            if (specialCharacters.contains(lemma))
                continue;
            docInfo.incrementTotalWordcount();
            if (stopwords.contains(lemma))
                continue;
            // Count how much each word has been used.
            docInfo.incrementWordCountOf(lemma);
            // Save processed tokens and filter out douplicates.
            tokens.add(new Token(lemma));
        }

        // Add a new row for the document to the matrix and save the rows index to the
        // document index.
        int thisDocumentsIndex = matrix.size();
        documentIndex.put(site.url, thisDocumentsIndex);
        int rowSize = 0;
        if (!matrix.isEmpty())
            rowSize = getVectorSize();
        matrix.add(new ArrayList<>(Collections.nCopies(rowSize, 0.0)));

        // Add each tokens TF to the matrix and tokenIndex.
        for (Token tok : tokens) {
            String word = tok.getToken();
            Integer foundIndex = tokenIndex.computeIfAbsent(word, k -> {
                int index = getVectorSize();
                // Add the token to all other documents in the matrix.
                for (ArrayList<Double> doc : matrix)
                    doc.add(0.0);
                // Add the token to the index and set foundIndex to the new index.
                return index;
            });

            // Set the TF score in the matrix.
            matrix.get(thisDocumentsIndex).set(tokenIndex.get(word), docInfo.getTfOf(word));

            // The token has been found in one more document to count.
            docsPerTokIndex.put(foundIndex, docsPerTokIndex.getOrDefault(foundIndex, 0) + 1);
        }
        this.forwardIndex.add(new VectorSite(site.url, docInfo.getDocVectorized()));
        this.totalDocCount++;
    }

    public void finish() {
        for (ArrayList<Double> doc : matrix) {
            for (int i = 0; i < doc.size(); i++) {
                double tfIdf = doc.get(i) * Math.log(this.totalDocCount / docsPerTokIndex.get(i));
                doc.set(i, tfIdf);
            }
        }
    }

    private LinkedList<String> formatQuery(String query) {
        LinkedList<String> tokens = new LinkedList<>();

        // Format tokenize and lemmatize the query string.
        CoreDocument content = new CoreDocument(query.toLowerCase());
        pipeline.annotate(content);

        // Remove stopwords and special characters.
        for (CoreLabel tok : content.tokens()) {
            String lemma = tok.lemma();
            if (specialCharacters.contains(lemma) || stopwords.contains(lemma))
                continue;
            tokens.addLast(lemma);
        }
        return tokens;
    }

    public List<String[]> searchQueryTfIdf(String query) {
        ArrayList<Entry<Double, String>> foundSites = new ArrayList<>();

        LinkedList<String> tokens = formatQuery(query);

        for (Entry<String, Integer> entry : documentIndex.entrySet()) {
            Integer docindex = documentIndex.get(entry.getKey());
            Double tfIdfScore = 0.0;

            for (String tok : tokens) {
                Integer tokIndex = tokenIndex.get(tok);
                if (tokIndex == null)
                    continue;
                tfIdfScore += matrix.get(docindex).get(tokIndex);
            }
            Entry<Double, String> site = new AbstractMap.SimpleEntry<>(tfIdfScore, entry.getKey());
            foundSites.add(site);
        }
        // Sort the found sites by TfIdf score.
        Collections.sort(foundSites, (entry, other) -> other.getKey().compareTo(entry.getKey()));

        // Format for the specification and return.
        return formatSearchOutput(foundSites);
    }

    public List<String[]> searchQueryCosine(String query) {

        LinkedList<String> tokens = formatQuery(query);
        int vectorSize = getVectorSize();

        // Create a vector in the same order as the matrix rows.
        double[] queryVector = new double[vectorSize];
        for (String tok : tokens)
            queryVector[tokenIndex.get(tok)] = 1;

        // Find the cosine similarity for each of the documents and save.
        ArrayList<Entry<Double, String>> foundSites = new ArrayList<>();
        for (Entry<String, Integer> entry : documentIndex.entrySet()) {
            String doc = entry.getKey();
            Integer docIndex = documentIndex.get(doc);

            // Find the cosine similarity between the querys and the documents vectors.
            double numerator = 0.0;
            double denomA = 0.0;
            double denomB = 0.0;
            for (int i = 0; i < vectorSize; i++) {
                double a = queryVector[i];
                double b = matrix.get(docIndex).get(i);
                numerator += a * b;
                denomA += Math.pow(a, 2);
                denomB += Math.pow(b, 2);
            }
            double cosineSimilarity = numerator / (Math.sqrt(denomA) * Math.sqrt(denomB));

            foundSites.add(new AbstractMap.SimpleEntry<>(cosineSimilarity, doc));
        }

        // Sort the results by cosine similarity.
        Collections.sort(foundSites, (entry, other) -> other.getKey().compareTo(entry.getKey()));

        // Format the ordered results to List<String[]> and return.
        return formatSearchOutput(foundSites);

    }

    public double calcCosineSimilarity(ArrayList<Double> a, ArrayList<Double> b) {
        double numerator = 0.0;
        double denomA = 0.0;
        double denomB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double ai = a.get(i);
            double bi = b.get(i);
            numerator += ai * bi;
            denomA += Math.pow(ai, 2);
            denomB += Math.pow(bi, 2);
        }
        return numerator / (Math.sqrt(denomA) * Math.sqrt(denomB));
    }

    private List<String[]> formatSearchOutput(List<Entry<Double, String>> foundSites) {
        LinkedList<String[]> results = new LinkedList<>();
        for (Entry<Double, String> entry : foundSites) {
            results.addLast(new String[] { entry.getValue(), entry.getKey().toString() });
        }
        return results;
    }

    private int getVectorSize() {
        if (matrix.isEmpty())
            return 0;
        return matrix.get(0).size();
    }

    public boolean containsToken(String token) {
        return tokenIndex.containsKey(token);
    }

    public Double getTfIdfOf(String tokenWord, String docId) {
        Integer tokIndex = tokenIndex.get(tokenWord);
        Integer docIndex = documentIndex.get(docId);

        if (tokIndex == null || docIndex == null)
            return null;

        return matrix.get(docIndex).get(tokIndex);
    }

    public boolean docHasToken(String tokenWord, String docId) {
        return getTfIdfOf(tokenWord, docId) != null;
    }

    public List<VectorSite> getForwardIndex() {
        return this.forwardIndex;
    }
}
