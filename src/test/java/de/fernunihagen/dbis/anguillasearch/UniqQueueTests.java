package de.fernunihagen.dbis.anguillasearch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.fernunihagen.dbis.anguillasearch.crawler.UniqQueue;

class UniqQueueTests {

    @BeforeAll
    static void setUp() throws IOException {

    }

    @Test
    void testQueue() {
        UniqQueue queue = new UniqQueue();
        assertEquals(0, queue.size());
        assertEquals(true, queue.isEmpty());
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
        int counter = 0;
        // queue all of the links
        for (String url : testURLs) {
            queue.queue(url);
            counter++;
            // One element was added so size should increase by 1.
            assertEquals(counter, queue.size());
        }

        // There are elements in the queue so it isn't empty.
        assertEquals(false, queue.isEmpty());
    }

    @Test
    void testPop() {
        UniqQueue queue = new UniqQueue();
        assertEquals(0, queue.size());
        assertEquals(true, queue.isEmpty());
        List<String> testURLs = List.of(
                "https://randomnerdtutorials.com/how-to-install-esp8266-board-arduino-ide/",
                "https://wiki.archlinux.org/title/Arduino",
                "https://jsoup.org/cookbook/input/load-document-from-url",
                "http://127.0.0.1:8080/?folder=/home/vscode/workspace",
                "https://www.wikipedia.de/",
                "https://www.wikipedia.org/",
                "https://es.wikipedia.org/wiki/Wikipedia:Portada",
                "https://ru.wikipedia.org/wiki/%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F_%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0",
                "https://hu.wikipedia.org/wiki/Kezd%C5%91lap",
                "https://hu.wikipedia.org/wiki/Hegeszt%C3%A9s_(f%C3%A9mek)");

        // queue all of the links
        for (String url : testURLs)
            queue.queue(url);

        for (int i = testURLs.size() - 1; i >= 0; i--) {
            // Remove all elements and see if they are unchanged.
            assertEquals(true, testURLs.contains(queue.pop()));
            // An element has been removed so the size should decrease by 1.
            assertEquals(i, queue.size());
        }

        // All elements have been removed, so the queue should be empty.
        assertEquals(true, queue.isEmpty());
    }

    @Test
    void testQueueDouplicates() {
        UniqQueue queue = new UniqQueue();

        // contains 3 copies
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
                "https://hu.wikipedia.org/wiki/Hegeszt%C3%A9s_(f%C3%A9mek)",
                "https://jsoup.org/cookbook/input/load-document-from-url",
                "https://hu.wikipedia.org/wiki/Kezd%C5%91lap",
                "https://randomnerdtutorials.com/how-to-install-esp8266-board-arduino-ide/"
        };
        // queue all of the links
        for (String url : testURLs)
            queue.queue(url);

        // Three links are douplicates and should not have been added.
        assertEquals(testURLs.length - 3, queue.size());
    }
}
