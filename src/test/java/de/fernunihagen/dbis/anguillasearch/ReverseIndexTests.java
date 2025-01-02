package de.fernunihagen.dbis.anguillasearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
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

    @Test
    void reverseIndexTests() {
        VectorIndex reverseIndex = new VectorIndex();

        String[] testURLs = {
                "https://randomnerdtutorials.com/how-to-install-esp8266-board-arduino-ide/",
                "https://wiki.archlinux.org/title/Arduino",
                "https://jsoup.org/cookbook/input/load-document-from-url",
                "http://127.0.0.1:8080/?folder=/home/vscode/workspace",
                "https://www.wikipedia.de/",
                "https://www.wikipedia.org/",
                "https://es.wikipedia.org/wiki/Wikipedia:Portada",
                "https://ru.wikipedia.org/wiki/%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F_%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0",
                "https://hu.wikipedia.org/wiki/Kezd%C5%91lap",
                "https://hu.wikipedia.org/wiki/Hegeszt%C3%A9s_(f%C3%A9mek)"
        };
        String[] testHeadings = {
                "Welcome to [Your Website Name], your trusted destination for [industry or service] solutions.",
                "Our mission is to deliver innovative and personalized [products/services] that exceed expectations.",
                "Explore our wide range of [specific services] designed to meet your needs and budget.",
                "Discover our collection of [product category], crafted with care and precision to ensure quality.",
                "Have questions? Get in touch with us today—we’re here to help!",
                "See why our customers love us! Read real reviews from satisfied clients.",
                "Looking for answers? Check out our frequently asked questions for quick solutions.",
                "Stay updated with the latest trends, tips, and insights from our experts on the [specific industry/topic].",
                "Ready to get started? Sign up now and join the [Your Website Name] community.",
                "Enjoy hassle-free shopping with fast, secure checkout and free shipping on orders over $50.",
                "© 2025 [Your Company Name]. All rights reserved. Privacy Policy | Terms of Service."
        };

        int i = 0;
        for (String url : testURLs) {
            assertEquals(i, reverseIndex.getNrOfSites());
            reverseIndex.addSite(new Site(url, "" + i, Arrays.asList(testHeadings[i]), "" + i));
            i++;
        }
        // Now all sites should be contained in the index.
        assertEquals(testURLs.length, reverseIndex.getNrOfSites());

        // Adding Sites with the same URL should not change the index.
        for (String url : testURLs) {
            reverseIndex.addSite(new Site(url, null, null, null));
            i++;
        }
        assertEquals(testURLs.length, reverseIndex.getNrOfSites());

        // The following list of tokens should now be contained and findable:
        String[] testToken = {
                "expert", "quick", "trend", "precision", "sign", "craft", "secure", "shipping", "solution", "review",
                "exceed", "tip", "join", "welcome", "shopping", "order", "budget", "discover", "read", "ensure", "$",
                "need", "satisfied", "touch", "check", "real", "collection", "community", "look", "personalize",
                "quality", "innovative", "0", "insight", "1", "2", "mission", "3", "4", "5", "6", "7", "8", "name", "9",
                "topic", "50.9", "care", "love", "trust", ".0", ".1", ".2", ".3", "frequently", ".5", ".6",
                "destination", "enjoy", "update", ".7", "range", "deliver", ".8", "industry", "expectation", "see",
                "hassle", "ready", "today", "get", "design", "client", "free", "checkout", "latest", "product",
                "website", "question", "wide", "explore", "start", "specific", "stay", "help", "fast", "answer", "’re",
                "meet", "service", "ask", "category", "customer"
        };

        for (String token : testToken) {
            assertEquals(true, reverseIndex.containsToken(token), token);
        }
    }
}