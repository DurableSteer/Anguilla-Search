package de.fernunihagen.dbis.anguillasearch.helpers;

import java.util.Map.Entry;
import java.util.Set;

/**
 * A VectorSite object represents a website as a vector.
 */
public class VectorSite {
    private Set<Entry<String, Long>> vector;
    private String url;

    /**
     * Get a new VectorSite object for the given url and vector.
     * 
     * @param url    The url of the website.
     * @param vector The vector of tuples (token, Tf score)
     */
    public VectorSite(String url, Set<Entry<String, Long>> vector) {
        this.url = url;
        this.vector = vector;
    }

    /**
     * Get the VectorSites url.
     * 
     * @return The sites url.
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Get the vector of tokens.
     * 
     * @return The vector of (token, Tf score) tuples of this VectorSite.
     */
    public Set<Entry<String, Long>> getVector() {
        return vector;
    }
}
