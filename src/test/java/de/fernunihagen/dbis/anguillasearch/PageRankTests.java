package de.fernunihagen.dbis.anguillasearch;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import java.util.logging.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.fernunihagen.dbis.anguillasearch.crawler.Crawler;
import de.fernunihagen.dbis.anguillasearch.pagerank.PageRankIndex;

/**
 * Unit tests for the page rank algorithm.
 */
class PageRankTests {
    private static final Logger logger = Logger.getLogger(PageRankTests.class.getName());
    static List<JsonObject> testJSONs = new ArrayList<>();
    static List<Map<String, Double>> pageRankForAllIntranets;

    @BeforeAll
    static void setUp() throws IOException {
        // Load the metadata from the JSON file
        testJSONs.add(Utils.parseJSONFile("intranet/cheesy1-f126d0d3.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/cheesy4-a31d2f0d.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/cheesy5-d861877d.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/cheesy6-54ae2b2e.json"));

        // Create a map of crawler instances and page rank instances
        pageRankForAllIntranets = new ArrayList<>();
        for (JsonObject testJSON : testJSONs) {
            // Extract the seed URLs from the JSON file
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);

            // Add your code here to calculate the page rank
            PageRankIndex pageRankIndex = new PageRankIndex();
            Crawler crawler = new Crawler(pageRankIndex);
            crawler.setSeed(seedUrls);
            try {
                crawler.crawl();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            pageRankIndex.calcPageRanks();

            // Add the crawler and page rank instances to the map
            pageRankForAllIntranets.add(pageRankIndex.getPageRankMap());
        }
    }

    @Test
    void sumOfPageRank() {
        for (Map<String, Double> pageRank : pageRankForAllIntranets) {
            // Get the sum of the page ranks
            double pageRankSum = pageRank.values().stream().mapToDouble(Double::doubleValue).sum();
            // Log the sum of the page ranks
            logger.log(Level.INFO, "Sum of PageRank: {0}", pageRankSum);
            // Verify that the sum of the page ranks is close to 1
            assertTrue(Math.abs(pageRankSum - 1.0) < 0.001);
        }
    }

    @Test
    void seedPageRank() {
        for (JsonObject testJSON : testJSONs) {
            // Extract the seed URLs from the JSON file
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);

            int numPages = new Gson().fromJson(testJSON.get("Num-Websites"), Integer.class);

            PageRankIndex pageRankIndex = new PageRankIndex();
            Crawler crawler = new Crawler(pageRankIndex);
            crawler.setSeed(seedUrls);
            try {
                crawler.crawl();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            pageRankIndex.calcPageRanks();

            // Get the page rank of the seed URLs
            for (String seedUrl : seedUrls) {

                double seedPageRank = pageRankIndex.getPageRankOf(seedUrl);
                // Adjust the damping factor to match your implementation
                double rankSource = (1.0 - 0.85) * (1.0 / numPages);

                assertTrue(Math.abs(seedPageRank - rankSource) < 0.001);
            }
        }
    }

    @Test
    void correctPageRankScores() throws IOException {
        // Create a map with URLs and the correct page rank scores;
        // These scores will be used to verify the correctness of the page rank
        // algorithm
        Map<String, Double> correctPageRankScores = Map.of(
                "http://cheddar24.cheesy6", 0.0375,
                "http://brie24.cheesy6", 0.3326,
                "http://crumbly-cheddar.cheesy6", 0.3097,
                "http://nutty-cheddar24.cheesy6", 0.3202);

        JsonObject testJSON = Utils.parseJSONFile("intranet/cheesy6-54ae2b2e.json");
        // Extract the seed URLs from the JSON file
        String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);

        // Add your code here to calculate the page rank
        PageRankIndex pageRankIndex = new PageRankIndex();
        Crawler crawler = new Crawler(pageRankIndex);
        crawler.setSeed(seedUrls);
        try {
            crawler.crawl();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        pageRankIndex.calcPageRanks();

        // Verify that the page rank scores are correct
        for (Map.Entry<String, Double> entry : correctPageRankScores.entrySet()) {
            String url = entry.getKey();
            double correctPageRank = entry.getValue();

            double pageRankScore = pageRankIndex.getPageRankOf(url);

            assertTrue(Math.abs(pageRankScore - correctPageRank) < 0.001);
        }
    }

    /*
     * The nodes seem to be in the following loop: http://brie24.cheesy6 ->
     * http://nutty-cheddar24.cheesy6 -> http://crumbly-cheddar.cheesy6 ->
     * http://brie24.cheesy6 ...
     * Where -> denotes linksTo.
     * If no dampening factor was applied the loop would go on for ever, but with
     * the dampening factor of 0.85 the nodes konverge towards
     * pageRank(http://brie24.cheesy6) = pageRank(http://nutty-cheddar24.cheesy6) =
     * pageRank(http://crumbly-cheddar.cheesy6).
     */
    @Test
    void analyseNetwork() throws IOException {

        JsonObject testJSON = Utils.parseJSONFile("intranet/cheesy6-54ae2b2e.json");
        // Extract the seed URLs from the JSON file
        String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);

        // Add your code here to calculate the page rank
        PageRankIndex pageRankIndex = new PageRankIndex();
        Crawler crawler = new Crawler(pageRankIndex);
        crawler.setSeed(seedUrls);
        try {
            crawler.crawl();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        pageRankIndex.calcPageRanks(1000, 0.85, true);
    }
}