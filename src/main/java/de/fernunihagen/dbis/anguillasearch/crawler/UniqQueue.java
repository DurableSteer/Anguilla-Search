package de.fernunihagen.dbis.anguillasearch.crawler;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import de.fernunihagen.dbis.anguillasearch.helpers.AVLTree;

/**
 * An efficient implementation of a unique FIFO queue that only accepts elements
 * which haven't been queued before.
 * An AVL tree is used to store added values for douplicate checking so queue()
 * works in O(log(n)).
 * The actual queue is a linked list hence all other operations are O(1).
 * 
 * @author Nico Beyer
 */
public class UniqQueue {

    private AVLTree<String> prevQueuedUrls;
    private LinkedList<String> urls;

    /**
     * Instantiate a new empty UniqQueue.
     */
    public UniqQueue() {
        prevQueuedUrls = new AVLTree<>();
        urls = new LinkedList<>();
    }

    /**
     * Add a URL to the back of the queue.
     * 
     * @param url The URL to be added.
     */
    public void queue(String url) {
        if (prevQueuedUrls.contains(url))
            return;
        prevQueuedUrls.insert(url);
        urls.addFirst(url);
    }

    /**
     * Remove the next(FIFO) URL from the queue and return it.
     * 
     * @return The next URL.
     * @throws NoSuchElementException If pop() is called on an empty queue.
     */
    public String pop() throws NoSuchElementException {
        return urls.removeLast();
    }

    /**
     * Finds the number of currently queued elements.
     * 
     * @return The number of currently queued URLs.
     */
    public int size() {
        return urls.size();
    }

    /**
     * Get a List of all currently queued urls.
     * 
     * @return A List of all currently queued urls.
     */
    public List<String> toList() {
        return this.urls;
    }

    /**
     * Checks if there are currently queued URLs.
     * 
     * @return True if the queue is empty. False otherwise.
     */
    public boolean isEmpty() {
        return urls.isEmpty();
    }
}
