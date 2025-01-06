package de.fernunihagen.dbis.anguillasearch;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.fernunihagen.dbis.anguillasearch.index.IndexSearcher;
import de.fernunihagen.dbis.anguillasearch.crawler.Crawler;
import de.fernunihagen.dbis.anguillasearch.index.VectorIndex;

/**
 * Unit tests for the search.
 */
class SearchTests {

    @Test
    void findCorrectURLs() throws IOException {
        JsonObject testJSON = Utils.parseJSONFile("intranet/cheesy1-f126d0d3.json");
        // Extract the seed URLs from the JSON file
        String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);
        // Extract the query from the JSON file
        String[] query = new Gson().fromJson(testJSON.get("Query-Token"), String[].class);
        // Extract the expected URLs from the JSON file
        String[] expectedURLs = new Gson().fromJson(testJSON.get("Query-URLs"), String[].class);
        // Execute a search with the given query in the given network via the seed URLs
        LinkedList<String> foundURLs = new LinkedList<>() {

        };
        List<String[]> foundEntries;

        // Place your code here to execute the search
        VectorIndex reverseIndex = new VectorIndex();
        Crawler crawler = new Crawler(reverseIndex);
        crawler.setSeed(seedUrls);
        try {
            crawler.crawl();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        reverseIndex.finish();

        String queryString = "";
        for (String s : query)
            queryString += s;

        foundEntries = (new IndexSearcher(reverseIndex)).searchQueryTfIdf(queryString);

        for (String[] entry : foundEntries) {
            foundURLs.add(entry[0]);
        }

        // Verify that the found URLs are correct, i.e. the same as stated in the JSON
        // file
        // Uncomment the following line once you have implemented the search
        assertTrue(foundURLs.containsAll(Arrays.asList(expectedURLs)));
    }
}