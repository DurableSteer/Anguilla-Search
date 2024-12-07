package de.fernunihagen.dbis.anguillasearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import de.fernunihagen.dbis.anguillasearch.index.VectorIndex;

/**
 * Unit tests for cosine similarity.
 */
class CosineTests {
    VectorIndex vi = new VectorIndex();

    private ArrayList<Double> ArrToArrList(double[] arr) {
        ArrayList<Double> arrList = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            arrList.add(0.0);
            arrList.set(i, arr[i]);
        }
        return arrList;
    }

    @Test
    void equalVectors() {
        // Create two vector with random positive double values;
        double[] vectorA = { 0.1, 0.2, 0.3, 0.4, 0.5 };
        double[] vectorB = { 0.1, 0.2, 0.3, 0.4, 0.5 };

        ArrayList<Double> A = ArrToArrList(vectorA);
        ArrayList<Double> B = ArrToArrList(vectorB);

        // The cosine similarity of two equal vectors should be 1.0
        // Replace the cosineSimilarity method with your implementation
        assertEquals(1.0, vi.calcCosineSimilarity(A, B));
    }

    @Test
    void orthogonalVectors() {
        // Create two orthogonal vectors
        double[] vectorA = { 1.0, 0.0, 0.0 };
        double[] vectorB = { 0.0, 1.0, 0.0 };

        ArrayList<Double> A = ArrToArrList(vectorA);
        ArrayList<Double> B = ArrToArrList(vectorB);
        // The cosine similarity of two orthogonal vectors should be 0.0
        // Replace the cosineSimilarity method with your implementation
        assertEquals(0.0, vi.calcCosineSimilarity(A, B));

    }

    @Test
    void randomVectors() {
        // Create two random vectors
        double[] vectorA = { 0.1, 0.2, 0.3, 0.4, 0.5 };
        double[] vectorB = { 0.5, 0.4, 0.3, 0.2, 0.1 };

        ArrayList<Double> A = ArrToArrList(vectorA);
        ArrayList<Double> B = ArrToArrList(vectorB);
        // The cosine similarity of two random positive vectors should be between 0.0
        // and 1.0
        // Replace the cosineSimilarity method with your implementation
        assertTrue(vi.calcCosineSimilarity(A, B) > 0.0);
        assertTrue(vi.calcCosineSimilarity(A, B) < 1.0);

    }

    @Test
    void specificResults() {
        // Create two vectors with specific values
        double[] vectorA = { 0.1, 0.2, 0.3, 0.4, 0.5 };
        double[] vectorB = { 0.5, 0.4, 0.3, 0.2, 0.1 };

        ArrayList<Double> A = ArrToArrList(vectorA);
        ArrayList<Double> B = ArrToArrList(vectorB);
        // The cosine similarity of these vectors should be 0.7
        // Replace the cosineSimilarity method with your implementation
        assertTrue(Math.abs(vi.calcCosineSimilarity(A, B) - 0.6364) < 0.0001);
    }
}