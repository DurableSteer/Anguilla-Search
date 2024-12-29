package de.fernunihagen.dbis.anguillasearch.index;

import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import de.fernunihagen.dbis.anguillasearch.helpers.Site;

/**
 * A ForwardIndex object is an index of Sites referenced by their URL.
 * For Each website a Site object as well as a vector of each token and their TF
 * value can be added and retrieved from the index.
 */
public class ForwardIndex {
    TreeMap<String, Site> index = new TreeMap<>();

    /**
     * Create a new empty ForwardIndex object.
     */
    public ForwardIndex() {

    }

    /**
     * Add a Site to the index. If a site with the same url has been added in the
     * past it will be overwritten.
     * 
     * @param site The site to be added.
     */
    public void addSite(Site site) {
        index.put(site.url, site);
    }

    /**
     * Add a vector containing a list of tokens to the Site with the given URL.
     * 
     * @param url    The URL of the Site to which the vector will be added.
     * @param vector The vector to be added.
     */
    public void addVector(String url, Set<Entry<String, Long>> vector) {
        if (index.containsKey(url))
            index.get(url).setVector(vector);
    }

    /**
     * Get the title of the Site with the given URL in the index.
     * 
     * @param url The URL of the site to take the title from.
     * @return The title of the found Site.
     */
    public String getTitleOf(String url) {
        return index.get(url).title;
    }
}
