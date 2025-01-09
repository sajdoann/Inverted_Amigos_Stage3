package org.ulpgc.crawler;

public class MainCrawler {
    public static void main(String[] args) {

        // TODO Hazelcast

        CrawlerThread crawlerThread = new CrawlerThread();
        crawlerThread.fetchBooks(50);
        crawlerThread.shutdownExecutor();
        //crawlerThread.fetchBooks(75);
        //crawlerThread.shutdownExecutor();

    }
}
