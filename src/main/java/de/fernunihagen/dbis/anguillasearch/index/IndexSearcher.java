package de.fernunihagen.dbis.anguillasearch.index;

import static de.fernunihagen.dbis.anguillasearch.helpers.HelperFunctions.max;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.fernunihagen.dbis.anguillasearch.pagerank.PageRankIndex;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * This class contains algorithms to perform search operations on a reverse
 * index.
 */
public class IndexSearcher {

    private VectorIndex index;
    private StanfordCoreNLP pipeline;
    private List<String> specialCharacters;
    private List<String> stopwords;

    /**
     * Get a new IndexSearcher object.
     * 
     * @param vectorIndex
     */
    public IndexSearcher(VectorIndex vectorIndex) {
        this.index = vectorIndex;
        this.specialCharacters = index.getSpecialCharacters();
        this.stopwords = index.getStopWords();
        pipeline = index.getNlpPipeline();
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
     * This method uses the term frequency and Inverse Document frequency (TFIDF) to
     * determine relevance.
     * 
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

        // Get the TFIDF sum for each website in the index.
        for (String docId : index.getStoredDocumentIds()) {
            Double tfIdfScore = 0.0;
            tfIdfScore = index.getTfIdfSumOf(docId, tokens);
            if (tfIdfScore == 0.0)
                continue;
            Entry<Double, String> site = new AbstractMap.SimpleEntry<>(tfIdfScore, docId);
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
            for (CoreLabel label : content.tokens()) {
                String lemma = label.lemma();

                if (specialCharacters.contains(lemma) || stopwords.contains(lemma))
                    continue;

                ret.put(lemma, entry.getValue());
            }
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
        List<Double> queryVector = index.getQueryVectorFor(tokens, tokenizedWeights);

        // Find the cosine similarity for each of the documents and save.
        ArrayList<Entry<Double, String>> foundSites = new ArrayList<>();
        for (String docId : index.getStoredDocumentIds()) {
            List<Double> documentVector = index.getVectorOf(docId);

            // Find the cosine similarity between the querys and the documents vectors.
            double cosineSimilarity = 0.0;
            if (index.isNormalized())
                cosineSimilarity = calcCosineSimilarityNormalized(queryVector, documentVector);
            else
                cosineSimilarity = calcCosineSimilarity(queryVector, documentVector);

            // Only add pages that share similiarity with the query.
            if (cosineSimilarity != 0.0)
                foundSites.add(new AbstractMap.SimpleEntry<>(cosineSimilarity, docId));
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
        for (String word : query.split(" ")) {
            weights.put(word, 1.0);
        }

        return searchQueryCosine(query, weights);
    }

    /**
     * Find the sites of the index most relevant to the given search query.
     * This method uses the cosine similarity between query and indexed sites as
     * well as the page rank to determine relevance.
     * 
     * The Index needs to be finished first.
     * Normalize() may be called to increase this methods efficiency.
     * 
     * @param query         The search query to be used.
     * @param pageRankIndex The PageRankIndex to be used. This has to contain values
     *                      for each website in the VectorIndex's network and
     *                      calcPageRanks() needs to be called prior to calling this
     *                      method.
     * @return A List of String[2] sorted by search score in decending order.
     *         String[0] containins a sites url
     *         String[1] containins the sites calculated search score.
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
            maxSimilarity = max(similarityScore, maxSimilarity);
            maxPageRank = max(pageRank, maxPageRank);
        }

        // Calculate the combined score.
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
     * Assumes that a and b have the same number of entries.
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
        if ((denomA == 0.0) || (denomB == 0.0) || (numerator == 0.0))
            return 0.0;
        return numerator / (Math.sqrt(denomA) * Math.sqrt(denomB));
    }

    /**
     * Find the cosine similarity between two given normalized vectors.
     * 
     * Assumes that a and b have the same number of entries.
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
     * Format foundSites into a List of String[2] with
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
}
