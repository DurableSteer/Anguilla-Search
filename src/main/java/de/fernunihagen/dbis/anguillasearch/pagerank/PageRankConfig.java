package de.fernunihagen.dbis.anguillasearch.pagerank;

/**
 * The configuration values for the PageRankIndex class.
 */
public final class PageRankConfig {
    private PageRankConfig() {
    }

    // Maximum difference between two iterations of the pagerank algorithm.
    static final double PAGERANKVALUE_MAX_DIFFERENCE = 0.0001;

    static final double PAGERANK_STD_DFACTOR = 0.85;
    static final int PAGERANK_STD_TIMEOUT = 100000;
}
