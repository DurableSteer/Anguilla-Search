package de.fernunihagen.dbis.anguillasearch.helpers;

import java.util.List;

/**
 * Simple container to package a single websites text.
 */
public class Site {
    public final String url;
    public final String title;
    public final List<String> headers;
    public final String body;

    /**
     * Instantiate a new website container.
     * 
     * @param url     The URL of the website.
     * @param title   The title of the website.
     * @param headers The Headers found on the website.
     * @param body    All text in the body of the website.
     */
    public Site(String url, String title, List<String> headers, String body) {
        this.url = url;
        this.title = title;
        this.headers = headers;
        this.body = body;
    }
}