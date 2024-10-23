package de.fernunihagen.dbis.anguillasearch.crawler;

import java.util.ArrayList;

//Prototype !!! 
// Document me!
// TestMe!

public class UniqQueue {
    private ArrayList<String> prevQueuedUrls;
    private ArrayList<String> urls;
    
    public UniqQueue(){
        prevQueuedUrls = new ArrayList<>();
        urls = new ArrayList<>();
    }

    public void queue(String url){
        if(prevQueuedUrls.contains(url)) return;
        prevQueuedUrls.add(url);
        urls.add(url);
    }

    public String pop(){
        return urls.isEmpty() ? "" : urls.get(urls.size());
    }

}
