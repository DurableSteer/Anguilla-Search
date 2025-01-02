package de.fernunihagen.dbis.anguillasearch;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.fernunihagen.dbis.anguillasearch.crawler.Crawler;
import de.fernunihagen.dbis.anguillasearch.index.ForwardIndex;

import de.fernunihagen.dbis.anguillasearch.index.VectorIndex;
import de.fernunihagen.dbis.anguillasearch.pagerank.PageRankIndex;

/**
 * Unit tests for the crawler.
 */
class CrawlerTests {
    static List<JsonObject> testJSONs = new ArrayList<>();

    @BeforeAll
    static void setUp() throws IOException {
        // Load the metadata from the JSON file
        testJSONs.add(Utils.parseJSONFile("intranet/cheesy1-f126d0d3.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/cheesy2-c79b0581.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/cheesy3-7fdaa098.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/cheesy4-a31d2f0d.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/cheesy5-d861877d.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/cheesy6-54ae2b2e.json"));
    }

    @Test
    void crawlAllWebsitesInProvidedNetwork() {
        // Iterate over all test JSON files
        for (JsonObject testJSON : testJSONs) {
            // Extract the seed URLs from the JSON file
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);
            // Get the crawled pages
            int numPagesCrawled = 0;

            // Add your code here to get the number of crawled pages
            Crawler crawler = new Crawler();
            try {
                crawler.setSeed(seedUrls);
                crawler.crawl();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            numPagesCrawled = crawler.getNrOfSitesCrawled();

            // Verify that the number of crawled pages is correct, i.e. the same as stated
            // in the JSON file
            assertEquals(testJSON.get("Num-Websites").getAsInt(), numPagesCrawled);
        }
    }

    @Test
    void findCorrectNumberOfLinks() {
        // Iterate over all test JSON files
        for (JsonObject testJSON : testJSONs) {
            // Extract the seed URLs from the JSON file
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);
            // Add your code here to get the number of links

            // Get the number of links
            int numLinks = 0;

            Crawler crawler = new Crawler();
            try {
                crawler.setSeed(seedUrls);
                crawler.crawl();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            numLinks = crawler.getNrOfLinksFound();

            // Verify that the number of links is correct, i.e. the same as stated in the
            // JSON file
            // "Num-Links": 314466, Was edited in the json due to the reported bug.
            assertEquals(testJSON.get("Num-Links").getAsInt(), numLinks);
        }
    }

    @Test
    void testIndexCreation() {
        ForwardIndex forwardIndex = new ForwardIndex();
        VectorIndex vectorIndex = new VectorIndex();
        PageRankIndex pageRankIndex = new PageRankIndex();
        Crawler crawler = new Crawler(forwardIndex, vectorIndex, pageRankIndex);

        try {
            JsonObject testJSON = Utils.parseJSONFile("intranet/cheesy4-a31d2f0d.json");
            // Extract the seed URLs from the JSON file
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);
            crawler.setSeed(seedUrls);
            crawler.crawl();
            // Now the reverse/forward/pageRank index each should contain each url in the
            // network or 1024 if the siteLimit is reached.
            int linksInNetwork = testJSON.get("Num-Websites").getAsInt();

            assertEquals(linksInNetwork, forwardIndex.getNrOfSites());
            assertEquals(linksInNetwork, vectorIndex.getNrOfSites());
            assertEquals(linksInNetwork, pageRankIndex.getNrOfSites());

            // Test if adding individual indices works as intended.

            crawler = new Crawler(new ForwardIndex());
            crawler.setSeed(seedUrls);
            crawler.crawl();

            assertEquals(linksInNetwork, crawler.getForwardIndex().getNrOfSites());

            crawler = new Crawler(new VectorIndex());
            crawler.setSeed(seedUrls);
            crawler.crawl();

            assertEquals(linksInNetwork, crawler.getVectorIndex().getNrOfSites());

            crawler = new Crawler(new PageRankIndex());
            crawler.setSeed(seedUrls);
            crawler.crawl();

            assertEquals(linksInNetwork, crawler.getPageRankIndex().getNrOfSites());

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testCrawl() {
        Crawler crawler;

        try {
            JsonObject testJSON = Utils.parseJSONFile("intranet/cheesy5-d861877d.json");
            // Extract the seed URLs from the JSON file
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);

            crawler = new Crawler();

            // Test if the sitelimit works.
            crawler.setSeed(seedUrls);
            crawler.crawl(0);
            assertEquals(0, crawler.getNrOfSitesCrawled());

            crawler.setSeed(seedUrls);
            crawler.crawl(1);
            assertEquals(1, crawler.getNrOfSitesCrawled());

            crawler.setSeed(seedUrls);
            crawler.crawl(32);

            crawler.setSeed(seedUrls);
            crawler.crawl(64);
            assertEquals(64, crawler.getNrOfSitesCrawled());

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Generate the Graphs for the extra tasks.
     */
    @Test
    void generateGraphs() {
        Crawler crawler = new Crawler(new ForwardIndex(), new VectorIndex(), new PageRankIndex());

        try {
            // Extract the seed URLs from the JSON file
            String[] seedUrls = new Gson().fromJson(
                    Utils.parseJSONFile("intranet/cheesy4-a31d2f0d.json").get("Seed-URLs"),
                    String[].class);

            // Generate the net-graph map.
            crawler.setSeed(seedUrls);
            crawler.map();
            // Generate the query map.
            crawler.setSeed(seedUrls);
            crawler.mapQuery("flavor");
            // Generate the page rank map.
            crawler.setSeed(seedUrls);
            crawler.mapPageRank();
            // Generate the top3 map.
            crawler.setSeed(seedUrls);
            crawler.mapTop3("flavor");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
