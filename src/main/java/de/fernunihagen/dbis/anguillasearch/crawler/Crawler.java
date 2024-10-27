package de.fernunihagen.dbis.anguillasearch.crawler;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.ConnectException;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;

import de.fernunihagen.dbis.anguillasearch.helpers.AVLTree;
import de.fernunihagen.dbis.anguillasearch.helpers.Site;

// Clean me!
// Finish me!
/**
 * The Crawler class represents the webcrawler.
 * It contains methods to access its current state and methods to load a seed
 * and start crawling.
 * 
 * @author Nico Beyer
 */
public class Crawler {

    private UniqQueue queue;
    private LinkedList<Site> parsedSites;
    private AVLTree<Node> networkMap;
    private mxGraph networkGraph;
    private int nrOfLinksFound;
    private int nrOfSitesCrawled;

    public class SeedNotSetException extends Exception {
        public SeedNotSetException() {
            super("crawler: Can't start crawling, seed not set!");
        }
    }

    /**
     * A container class for use with the networkMap tree. Each Node contains a url
     * and it's corresponding vertex object on the networkGraph.
     */
    private class Node implements Comparable<Node> {
        public final String url;
        public final Object vertex;

        /**
         * Create a new Node object.
         * 
         * @param url    The found url.
         * @param vertex The urls corresponding vertex on the networkGraph.
         */
        public Node(String url, Object vertex) {
            this.url = url;
            this.vertex = vertex;
        }

        /**
         * Define a comparison function between nodes to establish a total order.
         */
        @Override
        public int compareTo(Node other) {
            return this.url.compareTo(other.url);
        }
    }

    /**
     * Instantiate a new empty webcrawler.
     */
    public Crawler() {
        queue = new UniqQueue();
        reset();
    }

    /**
     * Set a new seed for the crawler to start with. The seed needs to be set before
     * starting to crawl.
     * Calling this method will reset the crawlers current queue to only the
     * provided seed.
     * 
     * @param seedUrls A String[] of URLs, each a startpoint for crawling.
     */
    public void setSeed(String[] seedUrls) {
        queue = new UniqQueue();
        for (String url : seedUrls)
            queue.queue(url);
    }

    /**
     * Check if a String is a valid http(s) URL for crawling e.g. a reference to a
     * different page on the same host or a reference to a different host
     * alltogether.
     * 
     * @param url The URL to be checked.
     * @return True if url is a reference to a different page on the same host or on
     *         a different host and an http or https service. False otherwise.
     */
    private boolean isValid(String url) {
        return (url.startsWith("/") && !url.equals("/")) || url.startsWith("http");
    }

    /**
     * Reset the found links and crawled sites counters to zero.
     */
    private void reset() {
        nrOfLinksFound = 0;
        nrOfSitesCrawled = 0;
        parsedSites = new LinkedList<>();
        mxGraphModel model = new mxGraphModel();
        networkGraph = new mxGraph(model);
        networkMap = new AVLTree<>();
    }

    /**
     * Get title, headers and textcontent as well as all valid links from the given
     * webpage. Store text to parsedSites and queue up the links.
     * 
     * @param url  URL of the webpage.
     * @param page The webpage to be parsed.
     */
    private void storeTextContent(String url, Document page) {
        // 1. get the title
        String title = page.title();

        // 2. get the headers
        LinkedList<String> foundHeaders = new LinkedList<>();
        Elements headers = page.select("h1, h2, h3, h4, h5, h6");
        for (Element header : headers) {
            foundHeaders.addLast(header.text());
        }

        // 3. get the text content
        String text = page.body().text();

        // 4. format title/text into object and save to Structure
        parsedSites.addLast(new Site(url, title, foundHeaders, text));
    }

    /**
     * Find all links on a page and add them to the queue.
     * 
     * @param page The page to be searched.
     */
    private void queueUrls(Document page) {
        // get all links on the page
        Elements links = page.select("a[href]");

        for (Element link : links) {
            // check links for other types and absolutify
            if (isValid(link.attr("href"))) {
                // add valid link to the UniqQueue
                queue.queue(link.absUrl("href"));
                nrOfLinksFound++;
            }
        }
    }

    /**
     * Check if a url is already in the networkGraph or create a new Vertex to work
     * with if not.
     * Add the new Vertex to the graph if a new one is created.
     * 
     * @param url    The url of the found site.
     * @param parent The parent node in the networkGraph that this Vertex belongs
     *               to.
     * @return The Vertex object for the found site.
     */
    private Object insertVertexIfNotAlreadyAdded(String url, Object parent) {
        Node target = networkMap.retrieve(new Node(url, null));
        Object targetVertex = null;
        if (target == null) {
            // create new Vertex
            targetVertex = networkGraph.insertVertex(parent, null, url, 0, 0, 240, 30);
            // add new Vertex to the map.

            networkMap.insert(new Node(url, targetVertex));
        } else
            targetVertex = target.vertex;

        return targetVertex;

    }

    /**
     * Finds all the urls that lead to webpages on the given page and add them as
     * vertices/edges to the networkGraph.
     * 
     * @param rootUrl The Url of the page to be mapped.
     * @param page    The DOM Tree of the page to be mapped.
     */
    private void mapUrls(String rootUrl, Document page) {
        // get all links on the page
        Object parent = networkGraph.getDefaultParent();
        Elements links = page.select("a[href]");
        networkGraph.getModel().beginUpdate();

        Object currentVertex = insertVertexIfNotAlreadyAdded(rootUrl, parent);

        for (Element link : links) {
            // check links for other types and absolutify
            if (isValid(link.attr("href"))) {
                // add valid link to the UniqQueue
                String url = link.absUrl("href");

                Object targetVertex = insertVertexIfNotAlreadyAdded(url, parent);
                // connect parent -> target
                networkGraph.insertEdge(parent, null, "", currentVertex, targetVertex);

                queue.queue(url);
                nrOfLinksFound++;
            }
        }
        networkGraph.getModel().endUpdate();
    }

    /**
     * Start the crawler until siteLimit pages have been visited and parsed. The
     * parsed pages url, title, headers and textcontent will be saved to the
     * structure.
     * The seed needs to be set before calling this method!
     * Calling this method will delete all saved information of the last run.
     * 
     * @param siteLimit The non zero based number of sites to be visited.
     * @throws SeedNotSetException            Will be thrown if the seed is not set
     *                                        before calling crawl().
     * @throws java.net.MalformedURLException Will be thrown if a non http(s) link
     *                                        is in the seed or queue. Likely the
     *                                        seed.
     * @throws java.io.IOException            Will be thrown if a general error
     *                                        prevents the crawler from fetching a
     *                                        site.
     */
    public void crawl(int siteLimit, boolean map) throws SeedNotSetException, java.io.IOException {
        reset();
        if (siteLimit < 1)
            return; // nothing to do

        // 1. check if seed is set
        if (queue.isEmpty())
            throw new SeedNotSetException();

        // 2. while siteLimit isn't reached
        int sitesVisited = 0;
        String url = "";

        while ((sitesVisited < siteLimit) && !queue.isEmpty()) {

            try {
                // 2.1 fetch webpage from url
                url = queue.pop();
                Document page = Jsoup.connect(url).get();

                if (map)
                    mapUrls(url, page);
                else {
                    storeTextContent(url, page);
                    queueUrls(page);
                }
                sitesVisited++;

            } catch (ConnectException | HttpStatusException e) {
                // A page couldn't be loaded; continue with the next link in the queue.

            } catch (java.net.MalformedURLException e) {
                // A given URL was malformed
                throw new java.net.MalformedURLException("crawler: " + e.getMessage());
            } catch (java.io.IOException e) {
                // An error occurred!
                throw new java.io.IOException("crawler: " + e.getMessage());
            }
        }
        nrOfSitesCrawled = sitesVisited;
    }

    /**
     * Start the crawler until 1024 pages have been visited and parsed. The parsed
     * pages title, headers and textcontent will be saved to the index structure.
     * The seed needs to be set before calling this method!
     * 
     * @throws SeedNotSetException            Will be thrown if the seed is not set
     *                                        before calling crawl().
     * @throws java.net.MalformedURLException Will be thrown if a non http(s) link
     *                                        is in the seed or queue. Likely the
     *                                        seed.
     * @throws java.io.IOException            Will be thrown if a general error
     *                                        prevents the crawler from fetching a
     *                                        site.
     */
    public void crawl() throws SeedNotSetException, java.io.IOException {
        crawl(1024, false);
    }

    /**
     * Use the crawler to map out the first 16 pages of a network. The result is a
     * graph saved in figures/net-graph.png.
     * The seed needs to be set first before calling map().
     * 
     * @throws SeedNotSetException Will be thrown if the seed is not set before
     *                             calling this method.
     * @throws java.io.IOException Will be thrown in case of a general error in the
     *                             crawl() method.
     */
    public void map() throws SeedNotSetException, java.io.IOException {
        crawl(16, true);

        // Draw a directed graph of the mapped out network.
        mxCircleLayout layout = new mxCircleLayout(networkGraph);
        networkGraph.getModel().beginUpdate();
        layout.execute(networkGraph.getDefaultParent());
        networkGraph.getModel().endUpdate();

        mxRectangle bounds = networkGraph.getGraphBounds();

        BufferedImage graph = mxCellRenderer.createBufferedImage(networkGraph,
                null, 1, Color.WHITE, true,
                bounds);

        File image = new File("figures/net-graph.png");
        ImageIO.write(graph, "png", image);
    }

    /**
     * Get the text content of the parsed Websites. Calling crawl() or map() will
     * delete the
     * current list.
     * 
     * @return
     */
    public List<Site> getParsedWebsites() {
        return parsedSites;
    }

    /**
     * Get the number of Links found in the last crawl. Calling crawl() or map()
     * will reset
     * this value.
     * 
     * @return The zero based number of links found in the last crawl().
     */
    public int getNrOfLinksFound() {
        return nrOfLinksFound;
    }

    /**
     * Get the number of pages visited and parsed in the last crawl. Calling crawl()
     * or map()
     * will reset this value.
     * 
     * @return The zero based number of pages visited in the last crawl().
     */
    public int getNrOfSitesCrawled() {
        return nrOfSitesCrawled;
    }
}
