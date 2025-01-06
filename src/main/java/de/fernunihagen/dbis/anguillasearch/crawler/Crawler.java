package de.fernunihagen.dbis.anguillasearch.crawler;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
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
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;

import de.fernunihagen.dbis.anguillasearch.helpers.AVLTree;
import de.fernunihagen.dbis.anguillasearch.helpers.Site;
import de.fernunihagen.dbis.anguillasearch.index.ForwardIndex;
import de.fernunihagen.dbis.anguillasearch.index.IndexSearcher;
import de.fernunihagen.dbis.anguillasearch.index.VectorIndex;
import de.fernunihagen.dbis.anguillasearch.pagerank.PageRankIndex;
import static de.fernunihagen.dbis.anguillasearch.crawler.CrawlerConfig.*;

/**
 * This class implements a simple webcrawler.
 * A Crawler object may be used to index a network of websites that link to each
 * other or create maps of a network.
 * 
 * The found data will be saved to a VectorIndex a ForwardIndex and a
 * PageRankIndex if given.
 * .
 * Furthermore the Crawler can map out a portion of a given network and display
 * the results in a simple graph.
 * 
 * @author Nico Beyer
 */
public class Crawler {

    private UniqQueue queue;
    private VectorIndex vectorIndex;
    private AVLTree<Node> networkMap;
    private mxGraph networkGraph;
    private PageRankIndex pageRankIndex;
    private ForwardIndex forwardIndex;
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

        /**
         * Just added for compliance with the Comparable contract.
         */
        @Override
        public boolean equals(Object other) {
            return this.url.equals(other);
        }

        /**
         * Just added for compliance with the Comparable contract.
         */
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    /**
     * Instantiate a new empty webcrawler that will not save crawled websites.
     */
    public Crawler() {
        queue = new UniqQueue();
        reset();
    }

    /**
     * Instantiate a new empty webcrawler that will save the crawled websites to the
     * given vector index.
     */
    public Crawler(ForwardIndex forwardIndex) {
        this();
        this.forwardIndex = forwardIndex;
    }

    /**
     * Instantiate a new empty webcrawler that will save the crawled websites to the
     * given vector index.
     */
    public Crawler(VectorIndex reverseIndex) {
        this();
        this.vectorIndex = reverseIndex;
    }

    /**
     * Instantiate a new empty webcrawler that will save the crawled websites to the
     * given page rank index.
     */
    public Crawler(PageRankIndex pageRankIndex) {
        this();
        this.pageRankIndex = pageRankIndex;
    }

    /**
     * Instantiate a new empty webcrawler that will save the crawled websites to the
     * given indices.
     */
    public Crawler(ForwardIndex forwardIndex, VectorIndex vectorIndex, PageRankIndex pageRankIndex) {
        this();
        this.forwardIndex = forwardIndex;
        this.vectorIndex = vectorIndex;
        this.pageRankIndex = pageRankIndex;
    }

    /**
     * Set a new seed for the crawler to start with. The seed needs to be set before
     * starting to crawl.
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
     * Reset the temporary data structures and counters for a new crawl.
     */
    private void reset() {
        nrOfLinksFound = 0;
        nrOfSitesCrawled = 0;
        mxGraphModel model = new mxGraphModel();
        networkGraph = new mxGraph(model);
        networkMap = new AVLTree<>();
    }

    /**
     * Get title, headers and textcontent as well as all valid links from the given
     * webpage. Store text to forwardIndex and vectorIndex if given and queue up the
     * links.
     * 
     * @param url  URL of the webpage.
     * @param page The webpage to be parsed.
     */
    private void storeTextContent(String url, Document page) {
        // If no indices are to be created there is nothing to do.
        if (forwardIndex == null && vectorIndex == null)
            return;

        // 1. Get the title.
        String title = page.title();

        // 2. Get the headers.
        LinkedList<String> foundHeaders = new LinkedList<>();
        Elements headers = page.select("h1, h2, h3, h4, h5, h6");
        for (Element header : headers) {
            foundHeaders.addLast(header.text());
        }

        // 3. Get the text content
        String text = page.body().text();

        // 4. Format title/text into object and save to forward and reverse index.
        if (forwardIndex != null)
            forwardIndex.addSite(new Site(url, title, foundHeaders, text));
        if (vectorIndex != null)
            vectorIndex.addSite(new Site(url, title, foundHeaders, text));

    }

    /**
     * Find all links on a page and add them to the queue.
     * 
     * @param source The URL of the page containing the links to be queued.
     * @param page   The page to be searched.
     */
    private void queueUrls(String source, Document page) {
        // Get all links on the page.
        Elements links = page.select("a[href]");

        LinkedList<String> linkList = new LinkedList<>();
        for (Element link : links) {
            // Check links for other types and absolutify
            if (isValid(link.attr("href"))) {
                // Add valid link to the UniqQueue and linkList.
                String url = link.absUrl("href");
                queue.queue(url);
                linkList.add(url);
                nrOfLinksFound++;
            }
        }
        if (pageRankIndex != null)
            pageRankIndex.addLinks(source, linkList);
    }

    /**
     * Check if a URL is already in the networkGraph or create a new Vertex to work
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
            // Create new Vertex
            targetVertex = networkGraph.insertVertex(parent, null, url, 0, 0, NETWORKGRAPH_VERTEX_WIDTH,
                    NETWORKGRAPH_VERTEX_HEIGHT);

            // Add new Vertex to the map.
            networkMap.insert(new Node(url, targetVertex));
        } else
            targetVertex = target.vertex;

        return targetVertex;

    }

    /**
     * Finds all the URLs that lead to webpages on the given page and add them as
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
            // Check links for other types and absolutify.
            if (isValid(link.attr("href"))) {
                // Add the valid link to the UniqQueue.
                String url = link.absUrl("href");

                Object targetVertex = insertVertexIfNotAlreadyAdded(url, parent);
                // connect parent -> target
                networkGraph.insertEdge(parent, null, "", currentVertex, targetVertex,
                        "");

                queue.queue(url);
                nrOfLinksFound++;
            }
        }
        networkGraph.getModel().endUpdate();
    }

    /**
     * Crawl starting from the seed until siteLimit pages have been visited and
     * parsed.
     * If any indices are present the relevant information will be saved to each
     * index.
     * 
     * The seed needs to be set before calling this method!
     * 
     * Calling this method will delete all saved information of the last run.
     * 
     * @param siteLimit The non zero based number of sites to be visited.
     * @param map       If set true the crawler will add all found links and sites
     *                  to the networkgraph.
     * @throws SeedNotSetException            Will be thrown if the seed is not set
     *                                        before calling crawl().
     * @throws java.net.MalformedURLException Will be thrown if a non http(s) link
     *                                        is in the seed or queue. Likely the
     *                                        seed.
     * @throws java.io.IOException            Will be thrown if a general error
     *                                        prevents the crawler from fetching a
     *                                        site.
     */
    private void crawl(int siteLimit, boolean map) throws SeedNotSetException, java.io.IOException {
        reset();
        if (siteLimit < 1)
            return; // nothing to do

        // 1. Check if seed is set.
        if (queue.isEmpty())
            throw new SeedNotSetException();

        // 2. While siteLimit isn't reached.
        int sitesVisited = 0;
        String url = "";
        while ((sitesVisited < siteLimit) && !queue.isEmpty()) {

            try {
                // 2.1 Fetch the webpage from the next URL.
                url = queue.pop();
                Document page = Jsoup.connect(url).get();

                if (map)
                    mapUrls(url, page);
                else {
                    storeTextContent(url, page);
                    queueUrls(url, page);
                }
                sitesVisited++;

            } catch (ConnectException | HttpStatusException e) {
                // A page couldn't be loaded; continue with the next link in the queue.

            } catch (java.net.MalformedURLException e) {
                // A given URL was malformed.
                throw new java.net.MalformedURLException("crawler: " + e.getMessage());
            } catch (java.io.IOException e) {
                // An error occurred!
                throw new java.io.IOException("crawler: " + e.getMessage());
            }
        }
        nrOfSitesCrawled = sitesVisited;
    }

    /**
     * Start the crawler until the standard number of pages(1024) have been visited
     * and parsed. If any indices are present the relevant information will be saved
     * to each index.
     * 
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
        crawl(CRAWLER_STD_SITELIMIT, false);
    }

    /**
     * Start the crawler until siteLimit pages have been visited and parsed.
     * If any indices are present the relevant information will be saved to each
     * index.
     * 
     * The seed needs to be set before calling this method!
     * 
     * @param siteLimit The maximum number of pages to be visited.
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
    public void crawl(int siteLimit) throws SeedNotSetException, java.io.IOException {
        crawl(siteLimit, false);
    }

    /**
     * Apply the search scores of a list of search results to a list of Nodes adding
     * them to the nodes value.
     * 
     * If withTop3 is true the top 3 search results nodes background color will be
     * changed to a highlighting color.
     * 
     * @param entryList The list of search results in the format: [url,
     *                  searchScore].
     * @param nodes     The list of nodes to apply the searchscores/highlighting to.
     * @param withTop3  If true the top 3 search results will be highlighted.
     */
    private void setNodeValuesOf(List<String[]> entryList, List<Node> nodes, boolean withTop3) {
        // Get the top 3 search results.
        LinkedList<String> top3 = new LinkedList<>();
        for (int j = 0; j < 3; j++) {
            top3.add(entryList.get(j)[0]);
        }

        // Add the TFIDFScore or combined cosine-pagerank to each node on the
        // searchpath.
        for (String[] entry : entryList) {
            String url = entry[0];

            String score = String.format(NETWORKGRAPH_SCORE_FORMAT, entry[1]);
            for (Node node : nodes) {
                mxCell vertex = (mxCell) node.vertex;

                // Add the TFIDFScore or combined cosine-pagerank to the nodes value.
                if (vertex.getValue().equals(url)) {
                    vertex.setValue(vertex.getValue() + "\n" + score);

                    if (withTop3 && top3.contains(entry[0])) {
                        vertex.setStyle("fillColor=" + NETWORKGRAPH_HIGHLIGHT_COLOR);
                    }
                }

            }
        }
    }

    /**
     * Use the crawler to map out the first 16 pages of a network. If a search query
     * is given the network will be searched and the found TFIDF search scores are
     * added to each relevant node.
     * 
     * If withPageRank is set true the page rank which each node hands down to their
     * child nodes is going to be displayed at the edges.
     * 
     * If withTop3 is set true the combined cosine-pagerank score is written to the
     * nodes and the top 3 results are marked.
     * 
     * The result is a graph saved in figures/net-graph if no options are given,
     * figures/${query} if a query is given,
     * figures/page-rank.png if withPageRank is true,
     * figures/top3 if a query and withTop3 is true.
     * 
     * 
     * The seed needs to be set first before calling map().
     * 
     * @param query        The search query to be displayed.
     * @param withPageRank If true the page rank handed down will be displayed at
     *                     the graphs edges.
     * @param withTop3     If true the cosine-pagerank score will be used and the
     *                     top 3 results will be marked in the graph.
     * @throws SeedNotSetException Will be thrown if the seed is not set before
     *                             calling this method.
     * @throws java.io.IOException Will be thrown in case of a general error in the
     *                             crawl() method.
     */
    private void map(String query, boolean withPageRank, boolean withTop3)
            throws SeedNotSetException, java.io.IOException {
        // Get the search results.
        String[] seed = new String[queue.size()];
        int i = 0;
        for (String url : queue.toList()) {
            seed[i] = url;
            i++;
        }

        setSeed(seed);
        crawl();
        VectorIndex fullIndex = this.vectorIndex;
        PageRankIndex fullPageRankIndex = this.pageRankIndex;
        fullPageRankIndex.calcPageRanks();
        IndexSearcher indexSearcher = new IndexSearcher(fullIndex);

        // Get the map data.
        setSeed(seed);
        crawl(CRAWLER_MAP_SITELIMIT, true);

        networkGraph.getModel().beginUpdate();
        String fileName = "net-graph.png";

        List<Node> nodes = networkMap.getValuesInorder();
        if (query != null) {
            List<String[]> queryResult;
            if (withTop3) {
                fileName = "top3.png";
                queryResult = indexSearcher.searchQueryCosinePageRank(query, fullPageRankIndex);
            } else {
                fileName = query + ".png";
                queryResult = indexSearcher.searchQueryTfIdf(query);
            }
            setNodeValuesOf(queryResult, nodes, withTop3);
        }
        if (withPageRank) {
            // Add the pageRank to each edge.
            for (Node node : nodes) {
                mxCell vertex = (mxCell) node.vertex;
                int edgeCount = vertex.getEdgeCount();
                for (int j = 0; j < edgeCount; j++)
                    vertex.getEdgeAt(j)
                            .setValue(String.format(NETWORKGRAPH_SCORE_FORMAT,
                                    fullPageRankIndex.getPageRankOf(node.url) / edgeCount));
            }

            fileName = "page-rank.png";
        }

        // Draw a directed graph of the mapped out network.
        mxCircleLayout layout = new mxCircleLayout(networkGraph);

        layout.execute(networkGraph.getDefaultParent());
        networkGraph.getModel().endUpdate();

        mxRectangle bounds = networkGraph.getGraphBounds();

        BufferedImage graph = mxCellRenderer.createBufferedImage(networkGraph,
                null, 1, Color.WHITE, true,
                bounds);

        // Draw the title.
        Graphics2D g2d = graph.createGraphics();
        g2d.setPaint(Color.RED);
        g2d.setFont(new Font(NETWORKGRAPH_TITLE_FONT_NAME, Font.BOLD, NETWORKGRAPH_TITLE_FONT_SIZE));
        g2d.drawString(fileName, NETWORKGRAPH_TITLE_FONT_POSX, NETWORKGRAPH_TITLE_FONT_POSY);
        g2d.dispose();
        File image = new File("figures/" + fileName);
        ImageIO.write(graph, "png", image);
    }

    /**
     * Use the crawler to map out the first standard map sitelimit(16) pages of a
     * network. The result is a graph saved in figures/net-graph.png.
     * The seed needs to be set first before calling map().
     * 
     * @throws SeedNotSetException Will be thrown if the seed is not set before
     *                             calling this method.
     * @throws java.io.IOException Will be thrown in case of a general error in the
     *                             crawl() method.
     */
    public void map() throws SeedNotSetException, java.io.IOException {
        map(null, false, false);
    }

    /**
     * Use the crawler to map out the first standard map sitelimit(16) pages of a
     * network and include the TfIdf search score for the given query in each node.
     * The result is a graph saved in figures/${query}.png.
     * The seed needs to be set first before calling mapQuery().
     * 
     * @param query The search query to be displayed.
     * @throws SeedNotSetException Will be thrown if the seed is not set before
     *                             calling this method.
     * @throws java.io.IOException Will be thrown in case of a general error in the
     *                             crawl() method.
     */
    public void mapQuery(String query) throws SeedNotSetException, java.io.IOException {
        map(query, false, false);
    }

    /**
     * Use the crawler to map out the first standard map sitelimit(16) pages of a
     * network and include the combined cosine similarity page rank search score
     * for the given query in each node. The result is a graph
     * saved in figures/top3.png.
     * The seed needs to be set first before calling mapQuery().
     * 
     * @param query The search query to be displayed.
     * @throws SeedNotSetException Will be thrown if the seed is not set before
     *                             calling this method.
     * @throws java.io.IOException Will be thrown in case of a general error in the
     *                             crawl() method.
     */
    public void mapTop3(String query) throws SeedNotSetException, java.io.IOException {
        map(query, false, true);
    }

    /**
     * Use the crawler to map out the first standard map sitelimit(16) pages of a
     * network and include the page rank handed down to each nodes children written
     * at their edges. The result is a graph
     * saved in figures/page-rank.png.
     * The seed needs to be set first before calling mapQuery().
     * 
     * @throws SeedNotSetException Will be thrown if the seed is not set before
     *                             calling this method.
     * @throws java.io.IOException Will be thrown in case of a general error in the
     *                             crawl() method.
     */
    public void mapPageRank() throws SeedNotSetException, java.io.IOException {
        map(null, true, false);
    }

    /**
     * Get the forward index object containing the results of a crawl if present.
     *
     * @return The forward index containing the crawled websites or null if no
     *         forward index has been added.
     */
    public ForwardIndex getForwardIndex() {
        if (forwardIndex != null)
            return this.forwardIndex;
        return null;
    }

    /**
     * Get the reverse index(VectorIndex) object containing the results of a crawl
     * if present.
     * 
     * @return The VectorIndex object or null if no vector index has been added.
     */
    public VectorIndex getVectorIndex() {
        if (vectorIndex != null)
            return this.vectorIndex;
        return null;
    }

    /**
     * Get the PageRankIndex object filled during crawling if present.
     * pageRankIndex.calcPageRanks() should be called before getting any page ranks
     * from the object.
     * 
     * @return The PageRankIndex object or null if no PageRankIndex has been added
     *         to the crawler.
     */
    public PageRankIndex getPageRankIndex() {
        if (pageRankIndex != null)
            return this.pageRankIndex;
        return null;
    }

    /**
     * Get the number of Links found in the last crawl. Calling crawl() or map()
     * will reset this value.
     * 
     * @return The zero based number of links found in the last crawl().
     */
    public int getNrOfLinksFound() {
        return nrOfLinksFound;
    }

    /**
     * Get the number of pages visited and parsed in the last crawl.
     * Calling crawl() or map() will reset this value.
     * 
     * @return The zero based number of pages visited in the last crawl().
     */
    public int getNrOfSitesCrawled() {
        return nrOfSitesCrawled;
    }
}
