package de.fernunihagen.dbis.anguillasearch;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.fernunihagen.dbis.anguillasearch.helpers.Site;
import de.fernunihagen.dbis.anguillasearch.index.VectorIndex;

/**
 * Unit tests for the reverse index.
 */
class ReverseIndexTests {

    static List<JsonObject> testPages;
    static JsonObject correctReverseIdex;
    static VectorIndex testIndex;

    @BeforeAll
    static void setUp() throws IOException {

        testPages = Utils.parseAllJSONFiles(java.util.Optional.of("src/test/resources/tf-idf/pages"));
        correctReverseIdex = Utils.parseJSONFile("src/test/resources/tf-idf/index.json");

        // Add your code here to create your reverse index

        testIndex = new VectorIndex();
        // Transform the testpages into my format and add to index.
        for (JsonObject testPage : testPages) {
            LinkedList<String> headings = new LinkedList<>();
            headings.add(testPage.get("headings").toString());
            String url = testPage.get("url").toString();
            // Remove quotes that the json conversion adds.
            url = url.substring(1, url.length() - 1);

            Site site = new Site(url, testPage.get("title").toString(), headings,
                    testPage.get("paragraphs").toString());
            testIndex.addSite(site);
        }
        testIndex.finish();
    }

    @Test
    void reverseIdexTFIDF() {

        for (Entry<String, JsonElement> entry : correctReverseIdex.entrySet()) {
            // The token of the reverse index
            String token = entry.getKey();
            JsonObject pagesMap = entry.getValue().getAsJsonObject();
            for (Entry<String, JsonElement> pageEntry : pagesMap.entrySet()) {

                // The URL of the page
                String url = pageEntry.getKey();
                // The TF-IDF value of the token in the page
                Double tfidf = pageEntry.getValue().getAsDouble();

                // Add your code here to compare the TF-IDF values of your reverse index with
                // the correct values

                // Check if the reverse index contains the token
                assertTrue(testIndex.containsToken(token));

                // Check if the URL exists for that token
                assertTrue(testIndex.docHasToken(token, url));

                // Get the TF-IDF value for the URL from your reverse index
                Double indexTfidf = testIndex.getTfIdfOf(token, url);

                // Check if the TF-IDF value is correct
                assertTrue(Math.abs(tfidf - indexTfidf) < 0.0001);

            }
        }
    }
}