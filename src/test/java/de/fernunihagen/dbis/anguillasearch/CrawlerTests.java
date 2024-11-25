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
import de.fernunihagen.dbis.anguillasearch.index.VectorIndex;

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
                crawler.crawlWithoutIndexing();
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
                crawler.crawlWithoutIndexing();
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
    void mapTests() {
        // Iterate over all test JSON files
        for (JsonObject testJSON : testJSONs) {
            // Extract the seed URLs from the JSON file
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);
            // Get the crawled pages

            // Add your code here to get the number of crawled pages
            Crawler crawler = new Crawler();

            try {
                crawler.setSeed(seedUrls);
                crawler.map("flavor");
                crawler.map();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}