package de.fernunihagen.dbis.anguillasearch;

import org.junit.jupiter.api.Test;

/**
 * Unit tests written by the developer.
 */
class DevTests {
    @Test
    void runUnitTests() {
        try {
            // Test the AVLTree.
            AVLTreeTests avlTreeTests = new AVLTreeTests();
            AVLTreeTests.setUp();
            avlTreeTests.testInsert();
            avlTreeTests.testHeight();
            avlTreeTests.TestFindingValues();
            avlTreeTests.testDelete();

            UniqQueueTests uniqQueueTests = new UniqQueueTests();
            UniqQueueTests.setUp();
            uniqQueueTests.testQueue();
            uniqQueueTests.testQueueDouplicates();
            uniqQueueTests.testPop();

            ReverseIndexTests reverseIndexTests = new ReverseIndexTests();
            ReverseIndexTests.setUp();
            reverseIndexTests.reverseIdexTFIDF();

            SearchTests searchTests = new SearchTests();
            searchTests.findCorrectURLs();

            CrawlerTests crawlerTests = new CrawlerTests();
            CrawlerTests.setUp();
            crawlerTests.crawlAllWebsitesInProvidedNetwork();
            crawlerTests.findCorrectNumberOfLinks();

            CosineTests cosineTests = new CosineTests();
            cosineTests.equalVectors();
            cosineTests.orthogonalVectors();
            cosineTests.randomVectors();
            cosineTests.specificResults();

            PageRankTests pageRankTests = new PageRankTests();
            PageRankTests.setUp();
            pageRankTests.seedPageRank();
            pageRankTests.sumOfPageRank();
            pageRankTests.analyseNetwork();
            pageRankTests.correctPageRankScores();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}