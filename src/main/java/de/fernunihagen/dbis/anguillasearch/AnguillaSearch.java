package de.fernunihagen.dbis.anguillasearch;

import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.fernunihagen.dbis.anguillasearch.crawler.Crawler;
import de.fernunihagen.dbis.anguillasearch.index.ForwardIndex;
import de.fernunihagen.dbis.anguillasearch.index.VectorIndex;
import de.fernunihagen.dbis.anguillasearch.pagerank.PageRankIndex;

/**
 * Main class of the AnguillaSearch project.
 */
public final class AnguillaSearch {
    // colors for better log visibility
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final Logger LOGGER = LoggerFactory.getLogger(AnguillaSearch.class);

    private AnguillaSearch() {
    }

    /**
     * Main method.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {

        // Print start message to logger
        LOGGER.info("Starting AnguillaSearch...");

        // Create a Crawler and let it crawl the given network.
        ForwardIndex forwardIndex = new ForwardIndex();
        VectorIndex reverseIndex = new VectorIndex();
        PageRankIndex pageRankIndex = new PageRankIndex();
        Crawler crawler = new Crawler(forwardIndex, reverseIndex, pageRankIndex);

        try {
            String[] seed = new Gson().fromJson(Utils.parseJSONFile("intranet/cheesy4-a31d2f0d.json").get("Seed-URLs"),
                    String[].class);
            crawler.setSeed(seed);
            crawler.crawl();

        } catch (Exception e) {
            LOGGER.error(ANSI_RED + e.getMessage() + ANSI_RESET);
        }
        LOGGER.info("Successfully crawled " + crawler.getNrOfSitesCrawled() + " pages.");

        // Finish building the required indices.
        reverseIndex.finish();
        reverseIndex.normalize();
        LOGGER.info("Reverse index built.");

        pageRankIndex.calcPageRanks();
        LOGGER.info("Page rank index built.");

        // Start the main loop.
        LOGGER.info("You can now start searching.\n");
        Scanner scanner = new Scanner(System.in);
        LOGGER.info("Enter search query (or 'exit' to quit):");
        String input = scanner.nextLine();

        while (!input.equals("exit")) {
            int i = 1;
            List<String[]> queryResults = reverseIndex.searchQueryCosinePageRank(input, pageRankIndex);
            if (queryResults.size() == 0)
                LOGGER.info("No relevant websites were found.");
            for (String[] entry : queryResults) {
                LOGGER.info("Result " + i);
                LOGGER.info("URL: " + entry[0]);
                LOGGER.info("Title: " + forwardIndex.getTitleOf(entry[0]));
                i++;
            }
            LOGGER.info("Enter search query (or 'exit' to quit):");
            input = scanner.nextLine();
        }

        scanner.close();

        /*
         * Set the java.awt.headless property to true to prevent awt from opening
         * windows.
         * If the property is not set to true, the program will throw an exception when
         * trying to
         * generate the graph visualizations in a headless environment.
         */
        System.setProperty("java.awt.headless", "true");
        LOGGER.info("Java awt GraphicsEnvironment headless: {}", java.awt.GraphicsEnvironment.isHeadless());

    }
}
