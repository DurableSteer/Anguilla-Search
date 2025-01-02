package de.fernunihagen.dbis.anguillasearch.helpers;

public final class HelperFunctions {
    private HelperFunctions() {
    }

    /**
     * Find and return the bigger of the two numbers.
     * If a==b the a will be returned.
     * 
     * @param a The first number to be compared.
     * @param b The second number to be compared.
     * @return a if a >= b else b.
     */
    public static double max(double a, double b) {
        return a >= b ? a : b;
    }
}
