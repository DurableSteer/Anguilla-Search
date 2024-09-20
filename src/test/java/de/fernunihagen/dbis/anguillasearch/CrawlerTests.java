package de.fernunihagen.dbis.anguillasearch;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
        // Big net testJSONs.add(Utils.parseJSONFile("intranet/cheesy5-d861877d.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/cheesy6-54ae2b2e.json"));
    }

    @Test
    void crawlAllWebsitesInProvidedNetwork() {
        // Iterate over all test JSON files
        for (JsonObject testJSON : testJSONs) {
            // Extract the seed URLs from the JSON file
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);
            
     
            // Add your code here to get the number of crawled pages

            // Get the crawled pages
            int numPagesCrawled = 0;
            
            
            // Verify that the number of crawled pages is correct, i.e. the same as stated in the JSON file
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
   


            // Verify that the number of links is correct, i.e. the same as stated in the JSON file
            assertEquals(testJSON.get("Num-Links").getAsInt(), numLinks);
        }
    }
}