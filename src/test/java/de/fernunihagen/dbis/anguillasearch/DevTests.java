package de.fernunihagen.dbis.anguillasearch;

import org.junit.jupiter.api.Test;

/**
 * Unit tests written by the developer.
 */
class DevTests {
    @Test
    void runUnitTests() {
        // Test the AVLTree.
        AVLTreeTests avlTreeTests = new AVLTreeTests();
        avlTreeTests.testInsert();
        avlTreeTests.testHeight();
        avlTreeTests.TestFindingValues();
        avlTreeTests.testDelete();

        UniqQueueTests uniqQueueTests = new UniqQueueTests();
        uniqQueueTests.testQueue();
        uniqQueueTests.testQueueDouplicates();
        uniqQueueTests.testPop();
    }
}