package de.fernunihagen.dbis.anguillasearch.index;

import java.util.List;
import java.util.TreeMap;

import de.fernunihagen.dbis.anguillasearch.helpers.AVLTree;

/**
 * This class was used with the ReverseIndex class and is archaic as the crawler
 * uses the VectorIndex instead now.
 * 
 * A Token object represents a tokenized and lemmatized word for use with a
 * pagerank algorithm.
 * Each token contains a list of document ids in which the word has been found
 * and each token
 * may save an TFIDF score for each of these documents.
 */
public class Token implements Comparable<Token> {
    private TreeMap<String, Double> tfIdfScores;
    private String tokenWord;
    private AVLTree<String> documents;

    /**
     * Create a new Token object for the given token word.
     * 
     * @param tokenWord The word represented by the token.
     */
    public Token(String tokenWord) {
        this.tokenWord = tokenWord;
        this.tfIdfScores = new TreeMap<>();
        documents = new AVLTree<>();
    }

    /**
     * Set a new TFIDF score for a documentId of this token.
     * If an old score is set it will be overwritten.
     * 
     * @param documentId The documents id.
     * @param newScore   The new score to be set.
     */
    public void setTfIdfScore(String documentId, double newScore) {
        this.tfIdfScores.put(documentId, newScore);
    }

    /**
     * Get the TFIDF score of this token for the given document id.
     * 
     * @param documentId The id of the document.
     * @return The TFIDF score of this token for the given documentId or null if the
     *         document id is not contained.
     */
    public double getTfIdfScore(String documentId) {
        return this.tfIdfScores.get(documentId);
    }

    /**
     * Add a document id to the token.
     * 
     * @param document the document id to be saved.
     */
    public void addDocument(String documentId) {
        this.documents.insert(documentId);
    }

    /**
     * Get the word that this token represents.
     * 
     * @return The word represented by the Token.
     */
    public String getToken() {
        return this.tokenWord;
    }

    /**
     * Get a List of all document ids saved for this token.
     * 
     * @return The List of document ids or null if no documents have been saved.
     */
    public List<String> getDocuments() {
        return this.documents.getValuesInorder();
    }

    /**
     * Get the total number of documents this token contains.
     * 
     * @return The number of documents.
     */
    public long getNrOfDocuments() {
        return this.documents.size();
    }

    /**
     * Check if the token contains the given document id.
     * 
     * @param documentId The document id to be checked for.
     * @return true if the token contains documentId, false otherwise.
     */
    public boolean containsUrl(String documentId) {
        return this.documents.contains(documentId);
    }

    /**
     * Define a comparison function between tokens to establish a total order.
     */
    @Override
    public int compareTo(Token other) {
        return this.tokenWord.compareTo(other.getToken());
    }

    /**
     * Just added for compliance with the Comparable contract.
     */
    @Override
    public boolean equals(Object other) {
        return this.tokenWord.equals(other);
    }

    /**
     * Just added for compliance with the Comparable contract.
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
