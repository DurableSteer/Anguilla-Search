package de.fernunihagen.dbis.anguillasearch.pagerank;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import static de.fernunihagen.dbis.anguillasearch.pagerank.PageRankConfig.*;

import de.fernunihagen.dbis.anguillasearch.helpers.HelperFunctions;

/**
 * A PageRankIndex object calculates and contains the page rank scores to a list
 * of given links.
 * lists of links from one source(website) can be added coninuously. When all
 * links have been added, the actual pageranks can be calculated by calling
 * calcPageRanks().
 * After the page ranks have been calculated no more links may be added, but the
 * final page ranks can be accessed.
 * 
 */
public class PageRankIndex {
    /**
     * An internal class to save the relevant information for each website bundled
     * into one object.
     * Each page holds their current page rank and a field for its page rank in the
     * next iteration.
     */
    private class Page {
        public List<String> linksIn;
        public String url;
        public int linksOut;
        public double pageRank;
        public double nextPageRank;

        /**
         * Get an empty Page object for the specified website.
         * 
         * @param url The url of the website.
         */
        public Page(String url) {
            this.url = url;
            this.linksIn = new LinkedList<>();
            this.linksOut = 0;
            this.pageRank = 0.0;
            this.nextPageRank = 0.0;
        }

        /**
         * Apply the changes of a finished iteration of the page-rank-algorithm for the
         * next iteration.
         */
        public void update() {
            this.pageRank = this.nextPageRank;
            this.nextPageRank = 0.0;
        }
    }

    private TreeMap<String, Page> pageIndex = new TreeMap<>();
    private int pageCount = 0;

    /**
     * Add a List of links from the source website to other websites to the index.
     * If source or links are not present yet in the index they will be added.
     * Links from the source to the source will be ignored.
     * 
     * @param source   The url of the source website.
     * @param newLinks The List of links to other websites.
     */
    public void addLinks(String source, List<String> newLinks) {
        // Find/create the node for the source and add the links.
        Page sourcePage = pageIndex.computeIfAbsent(source, k -> {
            pageCount++;
            return new Page(source);
        });

        // Find/create all linked pages and increase their outgoing links.
        for (String link : newLinks) {
            if (link.equals(source))
                continue;
            Page destPage = pageIndex.computeIfAbsent(link, k -> {
                pageCount++;
                return new Page(link);
            });
            destPage.linksIn.add(source);
            sourcePage.linksOut++;
        }
    }

    /**
     * Find the page rank for each of the added websites.
     * After calling this method no more links may be added to the index.
     * 
     * The algorithm will stop after timeout iterations to avoid an infinite loop if
     * the algorithm doesn't converge.
     * 
     * A dampening factor may be set to use a ranksource of (1-dFactor) values
     * of the dFactor may be in the range of 0 < dFactor < 1.
     * 
     * For debug purposes each iterations results may be printed to the console.
     * 
     * @param timeout The max number of iterations the algorithm may do.
     * @param dFactor The dampening/ranksource factor.
     * @param verbose If set true each iterations results will be printed to the
     *                console.
     */
    public void calcPageRanks(int timeout, double dFactor, boolean verbose) {
        Collection<Page> pages = pageIndex.values();
        // Initialize the pages with 1/n.
        double startRank = 1.0 / pageCount;
        for (Page page : pages) {
            page.pageRank = startRank;

        }

        // Recalculate the pagerank for each node until it converges on the actual
        // pagerank.
        double maxDiff = 1.0;
        int i = 0;
        while ((maxDiff > PAGERANKVALUE_MAX_DIFFERENCE) && (i < timeout)) {
            maxDiff = 0.0;
            if (verbose)
                System.out.println("--------------------------------- " + i + " ---------------------------------");
            for (Page page : pages) {
                double oldPageRank = page.pageRank;
                for (String link : page.linksIn) {
                    Page linkedPage = pageIndex.get(link);
                    page.nextPageRank = page.nextPageRank + dFactor * (linkedPage.pageRank / linkedPage.linksOut);
                }
                page.nextPageRank += (1 - dFactor) / this.pageCount;
                maxDiff = HelperFunctions.max(maxDiff, Math.abs(page.nextPageRank - oldPageRank));
                if (verbose)
                    System.out.println(String.format("Node:%-40s", page.url) + "pageRank:" + page.pageRank);
            }
            for (Page page : pages)
                page.update();
            i++;
        }
    }

    /**
     * Find the page rank for each of the added websites.
     * Using a default dampening factor of 0.85 thus a ranksource of 0.15.
     * After calling this method no more links may be added to the index.
     * The algorithm will stop after timeout iterations to avoid an infinite loop if
     * the algorithm doesn't converge.
     * 
     * @param timeout The max number of iterations the algorithm may do.
     */
    public void calcPageRanks(int timeout) {
        calcPageRanks(timeout, PAGERANK_STD_DFACTOR, false);
    }

    /**
     * Find the page rank for each of the added websites.
     * Using a default dampening factor of 0.85 thus a ranksource of 0.15.
     * After calling this method no more links may be added to the index.
     * The algorithm will stop after 100.000 iterations to avoid an infinite loop if
     * the algorithm doesn't converge.
     */
    public void calcPageRanks() {
        calcPageRanks(PAGERANK_STD_TIMEOUT, PAGERANK_STD_DFACTOR, false);
    }

    /**
     * Get the page rank of a given website.
     * This will only produce sensible results if calcPageRanks() was called before.
     * 
     * @param url The url of the site.
     * @return The page rank of the given site.
     */
    public Double getPageRankOf(String url) {
        Page page = pageIndex.getOrDefault(url, null);
        return page == null ? null : page.pageRank;
    }

    /**
     * Get a Map of all indexed websites urls and their page rank.
     * This will only produce sensible results if calcPageRanks() was called before.
     * 
     * @return The generated Map.
     */
    public Map<String, Double> getPageRankMap() {
        TreeMap<String, Double> result = new TreeMap<>();
        for (Entry<String, Page> entry : pageIndex.entrySet())
            result.put(entry.getKey(), entry.getValue().pageRank);
        return result;
    }

    /**
     * Get the number of sites saved in this index.
     * 
     * @return The number of sites indexed.
     */
    public int getNrOfSites() {
        return this.pageCount;
    }
}
