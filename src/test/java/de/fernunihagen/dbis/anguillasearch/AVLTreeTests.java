package de.fernunihagen.dbis.anguillasearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.fernunihagen.dbis.anguillasearch.helpers.AVLTree;

import java.util.List;

public class AVLTreeTests {

    @BeforeAll
    static void setUp() {
    }

    @Test
    void testInsert() {
        AVLTree<Integer> tree = new AVLTree<>();
        // The tree is empty so size should be 0.
        assertEquals(0, tree.size());

        // Insert 1024 sorted numbers into the tree.
        for (int i = 0; i < 1024; i++) {
            tree.insert(i);
            // The tree should now contain i+1 nodes.
            assertEquals(i + 1, tree.size());
        }

        // Insert the same numbers in oposite order.
        for (int i = 1023; i >= 0; i--)
            tree.insert(i);

        // Douplicates shouldn't be added so the amount of nodes has to stay the same.
        assertEquals(1024, tree.size());

        // Null may not be inserted.
        assertThrows(NullPointerException.class, () -> {
            tree.insert(null);
        });

        // So the size should still stay the same.
        assertEquals(1024, tree.size());
    }

    @Test
    void testHeight() {
        AVLTree<Integer> tree = new AVLTree<>();
        // The tree is empty so height should be 0.
        assertEquals(0, tree.getHeight());

        // Insert 1024 sorted numbers into the tree.
        for (int i = 0; i < 1024; i++)
            tree.insert(i);

        // Height should be log2(n) or log2(n)+1
        List<Integer> results = List.of(10, 11);
        assertTrue(results.contains(tree.getHeight()));
    }

    @Test
    void TestFindingValues() {
        AVLTree<String> tree = new AVLTree<>();

        // The tree is empty so nothing may be contained.
        assertEquals(false, tree.contains("https://randomnerdtutorials.com/how-to-install-esp8266-board-arduino-ide/"));
        // The tree is empty so null should be returned.
        assertEquals(null, tree.retrieve("https://randomnerdtutorials.com/how-to-install-esp8266-board-arduino-ide/"));

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

        // insert all of the links
        for (String url : testURLs) {
            tree.insert(url);
        }

        // All links are added so they should also be found by contains and retrieve.
        for (String url : testURLs) {
            assertEquals(true, tree.contains(url));
            assertEquals(true, tree.retrieve(url) == url);
        }

        // Null may not be searched for.
        assertThrows(NullPointerException.class, () -> {
            tree.contains(null);
        });
        assertThrows(NullPointerException.class, () -> {
            tree.retrieve(null);
        });

        // The Url is not in the tree so we expect false for contains and null for
        // retrieve.
        assertEquals(false, tree.contains("https://randomnerdtutorials.com/how-to-remove-esp8266-board-arduino-ide/"));
        assertEquals(null, tree.retrieve("https://randomnerdtutorials.com/how-to-remove-esp8266-board-arduino-ide/"));

        System.out.println("------------------Start removal -----------------");
        // remove all of the links
        for (String url : testURLs) {
            tree.delete(url);
            System.out.print(tree.toString(true));
            System.out.println("height:  " + tree.getHeight());
            System.out.println("################################################");
        }
    }

    @Test
    void testDelete() {
        AVLTree<String> tree = new AVLTree<>();
        // The tree is empty so isEmpty() should return true.
        assertEquals(true, tree.isEmpty());

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

        // insert all of the links
        for (String url : testURLs)
            tree.insert(url);

        // The tree has elements now so false should be returned.
        assertEquals(false, tree.isEmpty());

        assertThrows(NullPointerException.class, () -> {
            tree.delete(null);
        });
        // Remove all links in a backwards order.
        for (int i = testURLs.length - 1; i >= 0; i--) {
            tree.delete(testURLs[i]);
            // An element was removed so the tree should have one element less.
            assertEquals(i, tree.size());
            // The value should not be in the tree anymore.
            assertEquals(false, tree.contains(testURLs[i]));
        }

        // All elements were deleted so the tree should now be empty.
        assertEquals(true, tree.isEmpty());

    }
}
