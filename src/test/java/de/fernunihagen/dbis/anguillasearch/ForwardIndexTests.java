package de.fernunihagen.dbis.anguillasearch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.fernunihagen.dbis.anguillasearch.helpers.Site;
import de.fernunihagen.dbis.anguillasearch.index.ForwardIndex;

/**
 * Unit tests for the forward index.
 */
public class ForwardIndexTests {

    @Test
    void forwardIndexTests() {

        ForwardIndex forwardIndex = new ForwardIndex();

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
        int i = 0;
        for (String url : testURLs) {
            assertEquals(i, forwardIndex.getNrOfSites());
            forwardIndex.addSite(new Site(url, "" + i, Arrays.asList(url.split("/")), "" + i));
            i++;
        }
        // Now the index should contain only one page for each url.

        // Duplicates should be ignored, therefore the number of sites shouldn´t change.
        for (String url : testURLs) {
            forwardIndex.addSite(new Site(url, null, null, null));
        }
        assertEquals(testURLs.length, forwardIndex.getNrOfSites());

        // Each of the Sites should be findable now and the titles should not have
        // changed.
        i = 0;
        for (String url : testURLs) {
            assertEquals("" + i, forwardIndex.getTitleOf(url));

            Site site = forwardIndex.getSiteWithUrl(url);
            assertEquals(url, site.url);
            assertEquals("" + i, site.title);
            assertEquals(Arrays.asList(url.split("/")), site.headings);
            assertEquals("" + i, site.paragraphs);
            i++;
        }
    }
}
