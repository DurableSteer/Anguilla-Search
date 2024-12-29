package de.fernunihagen.dbis.anguillasearch.index;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Arrays;
import java.util.Collections;

import de.fernunihagen.dbis.anguillasearch.helpers.HelperFunctions;
import de.fernunihagen.dbis.anguillasearch.helpers.Site;
import de.fernunihagen.dbis.anguillasearch.helpers.Token;
import de.fernunihagen.dbis.anguillasearch.pagerank.PageRankIndex;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * 
 * The class represents a reverse index which maps a set of Sites to an index of
 * Tokens for use with a pagerank algorithm. The tokens are free of stopwords,
 * lemmatized and contain their TFIDF score for each of the Sites parsed.
 *
 * This implementation handles the tokens as vectors.
 * 
 * Each object contains a forward index of the added Sites.
 * The index may be searched by term-frequency-inverse-document-frequency or by
 * cosine similarity.
 * 
 * @author Nico Beyer
 */
public class VectorIndex {
    private TreeMap<String, Integer> documentIndex = new TreeMap<>();
    private HashMap<String, Integer> tokenIndex = new HashMap<>();
    private ArrayList<ArrayList<Double>> matrix = new ArrayList<>();
    private TreeMap<Integer, Integer> docsPerTokIndex = new TreeMap<>();

    private ForwardIndex forwardIndex = null;

    private List<String> specialCharacters;
    private List<String> stopwords;
    private StanfordCoreNLP pipeline;
    private double totalDocCount = 0;
    private boolean normalized = false;

    /**
     * Initialize basic lists and settings for the tokenization and lemmatization.
     */
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

    /**
     * Initialize a new empty vectorIndex object.
     */
    public VectorIndex() {
        init();
    }

    /**
     * Initialize a new empty vectorIndex object.
     */
    public VectorIndex(ForwardIndex forwardIndex) {
        this.forwardIndex = forwardIndex;
        init();
    }

    /**
     * Add a Site to the index.
     * Sites with the same URL will not be added.
     * 
     * @param site The Site to be added.
     */
    public void addSite(Site site) {
        if (documentIndex.containsKey(site.url))
            return;
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
        if (this.forwardIndex != null)
            this.forwardIndex.addVector(site.url, docInfo.getDocVectorized());
        this.totalDocCount++;
    }

    /**
     * Do some finishing steps to enable searching on the index. After finishing the
     * index no more sites may be added.
     * This needs to be called after adding all Sites to the index and before
     * calling any of the search methods, getTfIdfOf() or normalize().
     */
    public void finish() {
        for (ArrayList<Double> doc : matrix) {
            for (int i = 0; i < doc.size(); i++) {
                double tfIdf = doc.get(i) * Math.log(this.totalDocCount / docsPerTokIndex.get(i));
                doc.set(i, tfIdf);
            }
        }
    }

    /**
     * Normalize the index. This will increase search efficiency for the cosine
     * similarity based search algorithm.
     * TfIdf based search will not be accurate anymore after calling this method.
     * finish() needs to be called before using this method.
     * 
     */
    public void normalize() {
        for (ArrayList<Double> doc : matrix) {
            // Find the norm of the finished document vector
            Double norm = 0.0;
            for (Double tfIdf : doc)
                norm += Math.pow(tfIdf, 2);
            norm = Math.sqrt(norm);

            // Normalize the document vector for efficiency.
            for (int i = 0; i < doc.size(); i++)
                doc.set(i, doc.get(i) / norm);
        }
        this.normalized = true;
    }

    /**
     * Format a String into a list of lemmatized tokens free of stopwords and
     * special characters.
     * 
     * @param query The String to be formated.
     * @return A list of all tokens found in query.
     */
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

    /**
     * Find the sites of the index most relevant to the given search query.
     * This method uses the term frequency and Inverse Document frequency to
     * determine relevance.
     * The Index needs to be finished first.
     * 
     * @param query The search query to be used.
     * @return A List of String[2] sorted by TfIdf score in decending order.
     *         String[0] containins a sites url
     *         String[1] containins the sites calculated TfIdf score.
     */
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

    /**
     * Format the given weight distribution for a search query into a lemmatized
     * form.
     * 
     * @param weights A weight distribution containing a Weight for certain tokens.
     * @return A copy of weights in which each word has been replaced by its lemma.
     */
    private Map<String, Double> formatWeights(Map<String, Double> weights) {
        Map<String, Double> ret = new TreeMap<>();

        for (Entry<String, Double> entry : weights.entrySet()) {
            CoreDocument content = new CoreDocument(entry.getKey().toLowerCase());
            pipeline.annotate(content);
            String lemma = content.tokens().get(0).lemma();

            if (specialCharacters.contains(lemma) || stopwords.contains(lemma))
                continue;

            ret.put(lemma, entry.getValue());
        }

        // Normalize the given weights.
        Double sum = 0.0;
        for (Double value : ret.values())
            sum += value;

        // If invalid weights are given avoid divide by zero.
        if (sum == 0.0)
            sum = 1.0;
        for (Entry<String, Double> entry : ret.entrySet())
            ret.put(entry.getKey(), entry.getValue() / sum);

        return ret;
    }

    /**
     * Find the sites of the index most relevant to the given search query.
     * This method uses the cosine similarity between query and indexed sites to
     * determine relevance.
     * 
     * The search queries tokens can be weight by a Map of (token,weight) pairs
     * given. Each weight represents its tokens weight as a percentage e.g. 1.00
     * stands for 100% and 0.01 for 1%.
     * The weights are normalized, so if all weights add up to more than 100% the
     * ratio between all weights stays the same.
     * 
     * The Index needs to be finished first.
     * Normalize() may be called to increase this methods efficiency.
     * 
     * @param query   The search query to be used.
     * @param weights The weights to be applied to the search query.
     * @return A List of String[2] sorted by TfIdf score in decending order.
     *         String[0] containins a sites url
     *         String[1] containins the sites calculated TfIdf score.
     */
    public List<String[]> searchQueryCosine(String query, Map<String, Double> weights) {

        LinkedList<String> tokens = formatQuery(query);
        Map<String, Double> tokenizedWeights = formatWeights(weights);
        int vectorSize = getVectorSize();

        // Create a vector in the same order as the matrix rows and apply weights.
        ArrayList<Double> queryVector = new ArrayList<>(vectorSize);
        for (int i = 0; i < vectorSize; i++)
            queryVector.add(0.0);

        for (String tok : tokens) {
            Integer tokenIndx = tokenIndex.get(tok);
            if (tokenIndx == null)
                continue;

            queryVector.set(tokenIndx, tokenizedWeights.getOrDefault(tok, 0.0));
        }

        // Find the cosine similarity for each of the documents and save.
        ArrayList<Entry<Double, String>> foundSites = new ArrayList<>();
        for (Entry<String, Integer> entry : documentIndex.entrySet()) {
            String doc = entry.getKey();
            Integer docIndex = documentIndex.get(doc);

            // Find the cosine similarity between the querys and the documents vectors.
            double cosineSimilarity = 0.0;
            if (normalized)
                cosineSimilarity = calcCosineSimilarityNormalized(queryVector, matrix.get(docIndex));
            else
                cosineSimilarity = calcCosineSimilarity(queryVector, matrix.get(docIndex));

            // Only add pages that share similiarity with the query.
            if (cosineSimilarity != 0.0)
                foundSites.add(new AbstractMap.SimpleEntry<>(cosineSimilarity, doc));
        }

        // Sort the results by cosine similarity.
        Collections.sort(foundSites, (entry, other) -> other.getKey().compareTo(entry.getKey()));

        // Format the ordered results to List<String[]> and return.
        return formatSearchOutput(foundSites);

    }

    /**
     * Find the sites of the index most relevant to the given search query.
     * This method uses the cosine similarity between query and indexed sites to
     * determine relevance.
     * 
     * The Index needs to be finished first.
     * Normalize() may be called to increase this methods efficiency.
     * 
     * @param query The search query to be used.
     * @return A List of String[2] sorted by TfIdf score in decending order.
     *         String[0] containins a sites url
     *         String[1] containins the sites calculated TfIdf score.
     */
    public List<String[]> searchQueryCosine(String query) {
        Map<String, Double> weights = new TreeMap<>();
        for (String word : query.split(" "))
            weights.put(word, 1.0);
        return searchQueryCosine(query, weights);
    }

    /**
     * Find the sites of the index most relevant to the given search query.
     * This method uses the cosine similarity between query and indexed sites as
     * well as the page rank to
     * determine relevance.
     * 
     * The Index needs to be finished first.
     * Normalize() may be called to increase this methods efficiency.
     * 
     * @param query         The search query to be used.
     * @param pageRankIndex The PageRankIndex to be used. This has to contain values
     *                      for each website in the VectorIndex's network and
     *                      calcPageRanks() needs to be called prior to calling this
     *                      method.
     * @return A List of String[2] sorted by TfIdf score in decending order.
     *         String[0] containins a sites url
     *         String[1] containins the sites calculated TfIdf score.
     */
    public List<String[]> searchQueryCosinePageRank(String query, PageRankIndex pageRankIndex) {
        LinkedList<String[]> results = new LinkedList<>();
        List<String[]> cosineResults = searchQueryCosine(query);
        double maxSimilarity = Double.NEGATIVE_INFINITY;
        double maxPageRank = Double.NEGATIVE_INFINITY;

        // Find the maximum similarity and pageRank in the search results.
        for (String[] entry : cosineResults) {
            double similarityScore = Double.parseDouble(entry[1]);
            double pageRank = pageRankIndex.getPageRankOf(entry[0]);
            maxSimilarity = HelperFunctions.max(similarityScore, maxSimilarity);
            maxPageRank = HelperFunctions.max(pageRank, maxPageRank);
        }

        // Calculate the combined score
        for (String[] entry : cosineResults) {
            double similarityScore = Double.parseDouble(entry[1]);
            double pageRank = pageRankIndex.getPageRankOf(entry[0]);
            results.addLast(new String[] { entry[0],
                    Double.toString(similarityScore / maxSimilarity + pageRank / maxPageRank) });
        }

        // Sort the results by the new searchscore.
        Collections.sort(results, (entry, other) -> other[1].compareTo(entry[1]));

        return results;
    }

    /**
     * Find the cosine similarity between two given vectors.
     * 
     * Assumes that a and b are of equal lenght.
     * 
     * @param a The first vector.
     * @param b The second vector.
     * @return The cosine similarity score.
     */
    public double calcCosineSimilarity(List<Double> a, List<Double> b) {
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

    /**
     * Find the cosine similarity between two given normalized vectors.
     * 
     * Assumes that a and b are of equal lenght.
     * 
     * @param a The first vector.
     * @param b The second vector.
     * @return The cosine similarity score.
     */
    public double calcCosineSimilarityNormalized(List<Double> a, List<Double> b) {
        double sum = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double ai = a.get(i);
            double bi = b.get(i);
            sum += ai * bi;
        }
        return sum;
    }

    /**
     * Format foundSites into a List of String[2]with
     * String[1] = each sites url
     * String[2] = each sites search score
     * 
     * @param foundSites A list of tuples (searchScore, url) to be formated.
     * @return The formated list of String[2].
     */
    private List<String[]> formatSearchOutput(List<Entry<Double, String>> foundSites) {
        LinkedList<String[]> results = new LinkedList<>();
        for (Entry<Double, String> entry : foundSites)
            results.addLast(new String[] { entry.getValue(), entry.getKey().toString() });
        return results;
    }

    /**
     * Get the size of all vectors saved in the index.
     * 
     * @return The size of all vectors.
     */
    private int getVectorSize() {
        if (matrix.isEmpty())
            return 0;
        return matrix.get(0).size();
    }

    /**
     * Check if token is contained in the index.
     * 
     * @param token The token to be checked for.
     * @return True if the token is contained, else false.
     */
    public boolean containsToken(String token) {
        return tokenIndex.containsKey(token);
    }

    /**
     * Get the TfIdf score for the given token for the given document id.
     * 
     * @param token The token to be checked for.
     * @param docId The document id of the TfIdf score.
     * @return The found TfIdf score or null if either the token or document is not
     *         in the index.
     */
    public Double getTfIdfOf(String token, String docId) {
        Integer tokIndex = tokenIndex.get(token);
        Integer docIndex = documentIndex.get(docId);

        if (tokIndex == null || docIndex == null)
            return null;

        return matrix.get(docIndex).get(tokIndex);
    }

    /**
     * Check if the given token is contained in the specified document.
     * 
     * @param token The token to be checked for.
     * @param docId The documents id.
     * @return True if token is contained in the document, false if not.
     */
    public boolean docHasToken(String token, String docId) {
        return getTfIdfOf(token, docId) != null;
    }
}
