package de.fernunihagen.dbis.anguillasearch.index;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.Collections;

import static de.fernunihagen.dbis.anguillasearch.index.IndexConfig.*;
import de.fernunihagen.dbis.anguillasearch.helpers.Site;

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
    // Saves the index in the matrix for each added document.
    private TreeMap<String, Integer> documentIndex = new TreeMap<>();
    // Saves the matrix index for each token.
    private HashMap<String, Integer> tokenIndex = new HashMap<>();
    // This is matrix[i,j] with i = documents and j = tokens of that document.
    private ArrayList<ArrayList<Double>> matrix = new ArrayList<>();
    // The number of documents for each token.
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
        this.specialCharacters = REVERSEINDEX_SPECIAL_CHARACTERS;
        this.stopwords = REVERSEINDEX_STOPWORDS;
        Properties props = new Properties();
        props.setProperty("annotators", REVERSEINDEX_PIPELINE_ANNOTATORS);
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
            siteContent.append(" " + heading.toLowerCase());
        siteContent.append(" " + site.paragraphs.toLowerCase());

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
     * 
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

            // Normalize the document vector.
            for (int i = 0; i < doc.size(); i++)
                doc.set(i, doc.get(i) / norm);
        }
        this.normalized = true;
    }

    /**
     * Get the size of all vectors saved in the index.
     * 
     * @return The size of all vectors.
     */
    public int getVectorSize() {
        if (matrix.isEmpty())
            return 0;
        return matrix.get(0).size();
    }

    /**
     * Check if this index has been normalized by calling normalize().
     * 
     * @return True if the index is normalized, false otherwise.
     */
    public boolean isNormalized() {
        return this.normalized;
    }

    /*
     * Get a Set of all document identifiers stored in this index.
     * 
     * @return A Set of all document identifiers in this index.
     */
    public Set<String> getStoredDocumentIds() {
        return documentIndex.keySet();
    }

    /**
     * Format a given List of lemmatized tokens with weights into a vector in the
     * same order as all of the documents in the index.
     * 
     * The tokens can be weight by a Map of (token,weight) pairs
     * given. Each weight represents its tokens weight as a percentage e.g. 1.00
     * stands for 100% and 0.01 for 1%.
     * The weights are normalized, so if all weights add up to more than 100% the
     * ratio between all weights stays the same.
     * 
     * @param tokens  A list of lemmatized tokens to be formated.
     * @param weights A Map that maps a weight from onto each token.
     * @return A vector of weight tokens
     */
    public List<Double> getQueryVectorFor(List<String> tokens, Map<String, Double> weights) {
        int vectorSize = getVectorSize();

        // Create a vector in the same order as the matrix rows and apply weights.
        ArrayList<Double> queryVector = new ArrayList<>(vectorSize);
        for (int i = 0; i < vectorSize; i++)
            queryVector.add(0.0);

        for (String tok : tokens) {
            Integer tokenIndx = tokenIndex.get(tok);
            if (tokenIndx == null)
                continue;

            queryVector.set(tokenIndx, weights.getOrDefault(tok, 0.0));
        }

        return queryVector;
    }

    /**
     * Get the vector of TFIDF values of a given document.
     * 
     * @param documentId The identifier of the document.
     * @return A List of TFIDF values, one for each token in the index.
     */
    public List<Double> getVectorOf(String documentId) {
        return matrix.get(documentIndex.get(documentId));
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
     * Get the sum of the TFIDF values in a document of a list of lemmatized
     * token-(words).
     * 
     * @param docId  The identifier of the document.
     * @param tokens The List of lemmatized tokens.
     * @return The sum of all tokens TFIDF in the given document.
     */
    public double getTfIdfSumOf(String docId, List<String> tokens) {
        Integer docIndex = documentIndex.get(docId);
        double tfIdfScore = 0.0;
        for (String token : tokens) {
            Integer tokIndex = tokenIndex.get(token);
            if (tokIndex == null)
                continue;
            tfIdfScore += matrix.get(docIndex).get(tokIndex);
        }
        return tfIdfScore;
    }

    /**
     * Get the TfIdf score for the given token for the given document id.
     * 
     * @param token The (token-) word to be checked for.
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

    /**
     * Get the special characters filtered out by this index.
     * 
     * @return A List of special characters used by this index.
     */
    public List<String> getSpecialCharacters() {
        return this.specialCharacters;
    }

    /**
     * Get the stopwords filtered out by this index.
     * 
     * @return A List of stopwords used by this index.
     */
    public List<String> getStopWords() {
        return this.stopwords;
    }

    /**
     * Get the StanfordCoreNLP pipeline of this index.
     * 
     * @return The StanfordCoreNLP pipeline of this index.
     */
    public StanfordCoreNLP getNlpPipeline() {
        return this.pipeline;
    }

    public Set<String> getTokens() {
        return tokenIndex.keySet();
    }

    /**
     * Get the number of sites saved in this index.
     * 
     * @return The number of sites indexed.
     */
    public int getNrOfSites() {
        return (int) totalDocCount;
    }
}
