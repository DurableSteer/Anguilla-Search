package de.fernunihagen.dbis.anguillasearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class of the AnguillaSearch project.
 */
public final class AnguillaSearch {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnguillaSearch.class);
    
    private AnguillaSearch() {
    }

    /**
     * Main method.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {

        // Print start message to logger
        LOGGER.info("Starting AnguillaSearch...");

        /*
         * Set the java.awt.headless property to true to prevent awt from opening windows.
         * If the property is not set to true, the program will throw an exception when trying to 
         * generate the graph visualizations in a headless environment.
         */
        System.setProperty("java.awt.headless", "true");
        LOGGER.info("Java awt GraphicsEnvironment headless: {}", java.awt.GraphicsEnvironment.isHeadless());

    }
}
