package de.fernunihagen.dbis.anguillasearch.helpers;

import java.util.List;

/**
 * Simple container to package a single websites text.
 */
public class Site {
    public final String url;
    public final String title;
    public final List<String> headings;
    public final String paragraphs;

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
}