package de.fernunihagen.dbis.anguillasearch.index;

import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An object of the DocInfo class saves all relevant information for calculating
 * TF scores for a single document.
 */
public class DocInfo {

    private TreeMap<String, Long> words = new TreeMap<>();
    private double totalWordCount = 0;

    /**
     * Increment the total word counter for this document.
     */
    public void incrementTotalWordcount() {
        this.totalWordCount++;
    }

    /**
     * Increment the word counter for the given word on this document.
     * 
     * @param word The word whose counter shall be increased.
     */
    public void incrementWordCountOf(String word) {
        long wordCount = this.words.getOrDefault(word, 0l);
        wordCount++;
        this.words.put(word, wordCount);
    }

    /**
     * Get the TF score of the the given word.
     * 
     * @param word This words TF score will be returned.
     * @return The TF score of word.
     */
    public double getTfOf(String word) {
        if (this.totalWordCount == 0)
            return 0;
        long wordCount = words.get(word);
        return wordCount / totalWordCount;
    }

    public Set<Entry<String, Long>> getDocVectorized() {
        return words.entrySet();
    }
}