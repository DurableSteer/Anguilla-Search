package de.fernunihagen.dbis.anguillasearch.crawler;

// Document me!
// TestMe!
// Prototype !!!

public class Crawler {
    private UniqQueue queue;

    public Crawler(){
        queue = new UniqQueue();
    }

    public void setSeed(String[] seedUrls){
        for(String url : seedUrls)
            queue.queue(url);
    }

    public void crawl(int siteLimit){
        // 1. check if seed is set

        // 2. while siteLimit isn't reached 

            // 2.1 fetch site from url

            // 2.2 grab title
            // 2.3 grab text
            // 2.4 format title/text into object and save to Structure

            // 2.5 grab links
                //2.5.1 add links to queue

    }
    public void crawl(){
        crawl(1024);
    }
    
}
