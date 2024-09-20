package de.fernunihagen.dbis.anguillasearch;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * Unit tests for the reverse index.
 */
class ReverseIndexTests {

    static List<JsonObject> testPages;
    static JsonObject correctReverseIdex;


    @BeforeAll
    static void setUp() throws IOException {

        testPages = Utils.parseAllJSONFiles(java.util.Optional.of("src/test/resources/tf-idf/pages"));
        correctReverseIdex = Utils.parseJSONFile("src/test/resources/tf-idf/index.json");

        // Add your code here to create your reverse index

    }

        


    @Test
    void reverseIdexTFIDF() {

        for (Entry<String, JsonElement> entry : correctReverseIdex.entrySet()) {
            // The token of the reverse index
            String token = entry.getKey();
            JsonObject pagesMap= entry.getValue().getAsJsonObject();
            for (Entry<String, JsonElement> pageEntry : pagesMap.entrySet()) {

                // The URL of the page
                String url = pageEntry.getKey();
                // The TF-IDF value of the token in the page
                Double tfidf = pageEntry.getValue().getAsDouble();


                // Add your code here to compare the TF-IDF values of your reverse index with the correct values


                // Check if the reverse index contains the token
                //assertTrue(     .containsKey(token) );

                // Get the map of pages for the token
        
                // Check if the URL exists for that token
                //assertTrue(    .containsKey(url) );

                // Get the TF-IDF value for the URL from your reverse index
                Double indexTfidf;
                // Check if the TF-IDF value is correct
                //assertTrue(Math.abs(tfidf - indexTfidf) < 0.0001);

                // Remove the following line after adding your code
                assertTrue(false);

            }
        }
    }
}