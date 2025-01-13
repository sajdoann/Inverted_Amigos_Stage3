package org.ulpgc.crawler;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.ulpgc.inverted_index.apps.FilePerWordInvertedIndexHazelcast;
import org.ulpgc.inverted_index.implementations.GutenbergTokenizer;

import java.io.File;
import java.util.concurrent.locks.Lock;

public class MainCrawler {
    public static void main(String[] args) throws InterruptedException {
        // Configuration of Hazelcast
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig()
                .setEnabled(true)
                .addMember("10.26.14.210")
                .addMember("10.26.14.211")
                .addMember("10.26.14.212")
                .addMember("10.26.14.213")
                .addMember("10.26.14.214")
                .addMember("10.26.14.215")
                .addMember("10.26.14.216");

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        IMap<Integer, Boolean> pagesMap = hazelcastInstance.getMap("pagesMap");

        // Initialise the page map if it is empty
        initializePagesMap(hazelcastInstance, pagesMap);

        // Creating instances of the crawler and indexer
        CrawlerThread crawler = new CrawlerThread();
        GutenbergTokenizer tokenizer = new GutenbergTokenizer("InvertedIndex/stopwords.txt");
        FilePerWordInvertedIndexHazelcast indexer = new FilePerWordInvertedIndexHazelcast("gutenberg_books", tokenizer);

        // Process the pages
        while (true) {
            Integer pageToProcess = getNextPage(hazelcastInstance, pagesMap);
            if (pageToProcess == null) {
                System.out.println("There are no pages left to process. Terminating the node...");
                break;
            }

            System.out.println("Node processing page: " + pageToProcess);

            crawler.fetchBooks(pageToProcess);
            crawler.shutdownExecutor();

            // Index downloaded books
            indexer.indexAll();

            System.out.println("Processed page: " + pageToProcess);

            File directory = new File("gutenberg_books");

            crawler.deleteDirectoryContents(directory);
        }

        // Terminating the node
        hazelcastInstance.shutdown();
    }

    private static void initializePagesMap(HazelcastInstance hazelcastInstance, IMap<Integer, Boolean> pagesMap) {
        Lock lock = hazelcastInstance.getCPSubsystem().getLock("initializationLock");
        lock.lock();
        try {
            if (pagesMap.isEmpty()) {
                System.out.println("Initialising map with pages ...");
                for (int i = 0; i <= 2025; i += 25) {
                    pagesMap.put(i, false); // false indicates that the page has not been processed.
                }
            } else {
                System.out.println("Map with the pages already initialised.");
            }
        } finally {
            lock.unlock();
        }
    }

    private static Integer getNextPage(HazelcastInstance hazelcastInstance, IMap<Integer, Boolean> pagesMap) {
        for (Integer page : pagesMap.keySet()) {
            if (!pagesMap.get(page)) { // Check if the page has not been processed
                Lock lock = hazelcastInstance.getCPSubsystem().getLock("pageLock-" + page);
                lock.lock();
                try {
                    if (!pagesMap.get(page)) {
                        pagesMap.put(page, true); // Mark the page as processed
                        return page;
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
        return null; // There are no pages left to process
    }
}

