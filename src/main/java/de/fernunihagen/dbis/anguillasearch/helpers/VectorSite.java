package de.fernunihagen.dbis.anguillasearch.helpers;

import java.util.Map.Entry;
import java.util.Set;

public class VectorSite {
    private Set<Entry<String, Long>> vector;
    private String url;

    public VectorSite(String url, Set<Entry<String, Long>> vector) {
        this.url = url;
        this.vector = vector;
    }

    public String getUrl() {
        return this.url;
    }

    public Set<Entry<String, Long>> getVector() {
        return vector;
    }
}
