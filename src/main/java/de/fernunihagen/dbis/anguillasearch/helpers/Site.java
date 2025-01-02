package de.fernunihagen.dbis.anguillasearch.helpers;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Simple container to package a single websites text divided by URL, title,
 * list of Headings and paragraphs containing all text on the website.
 * A vector of tokens with their TF value may also be added.
 */
public class Site {
    public final String url;
    public final String title;
    public final List<String> headings;
    public final String paragraphs;
    private Set<Entry<String, Integer>> vector;

    /**
     * Instantiate a new website container.
     * 
     * @param url        The URL of the website.
     * @param title      The title of the website.
     * @param headings   The Headers found on the website.
     * @param paragraphs All text in the body of the website.
     */
    public Site(String url, String title, List<String> headings, String paragraphs) {
        this.url = url;
        this.title = title;
        this.headings = headings;
        this.paragraphs = paragraphs;

    }

    /**
     * Set the vector of tokens for this Site object.
     * 
     * @param newVector The new vector of tokens.
     */
    public void setVector(Set<Entry<String, Integer>> newVector) {
        this.vector = newVector;
    }
}