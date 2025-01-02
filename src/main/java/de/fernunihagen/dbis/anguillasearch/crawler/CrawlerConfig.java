package de.fernunihagen.dbis.anguillasearch.crawler;

/**
 * The Configuration values for the Crawler class.
 */
public final class CrawlerConfig {
    private CrawlerConfig() {
    }

    // ** Crawler **//

    // Styling information of the networkgraph created by the Crawler.map() method.
    static final int NETWORKGRAPH_VERTEX_WIDTH = 260;
    static final int NETWORKGRAPH_VERTEX_HEIGHT = 50;
    static final String NETWORKGRAPH_HIGHLIGHT_COLOR = "#FFE9C3";
    static final String NETWORKGRAPH_SCORE_FORMAT = "%.6s";
    static final String NETWORKGRAPH_TITLE_FONT_NAME = "Serif";
    static final int NETWORKGRAPH_TITLE_FONT_SIZE = 40;
    static final int NETWORKGRAPH_TITLE_FONT_POSX = 100;
    static final int NETWORKGRAPH_TITLE_FONT_POSY = 100;

    // Standard sitelimits for the crawler.
    static final int CRAWLER_STD_SITELIMIT = 1024;
    static final int CRAWLER_MAP_SITELIMIT = 16;

}
