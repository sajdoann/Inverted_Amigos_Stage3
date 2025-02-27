package org.ulpgc.crawler;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.ulpgc.inverted_index.apps.FilePerWordInvertedIndexHazelcast;
import org.ulpgc.inverted_index.implementations.GutenbergTokenizer;
import org.ulpgc.api.ApiApplication;

import java.io.File;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class MainCrawler {
    public static void main(String[] args) throws InterruptedException {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig()
                .setEnabled(true)
                .addMember("10.26.14.200")
                .addMember("10.26.14.201")
                .addMember("10.26.14.202")
                .addMember("10.26.14.203")
                .addMember("10.26.14.204")
                .addMember("10.26.14.205")
                .addMember("10.26.14.206")
                .addMember("10.26.14.207")
                .addMember("10.26.14.208")
                .addMember("10.26.14.209")
                .addMember("10.26.14.210")
                .addMember("10.26.14.211")
                .addMember("10.26.14.212")
                .addMember("10.26.14.213")
                .addMember("10.26.14.214")
                .addMember("10.26.14.215")
                .addMember("10.26.14.216")
                .addMember("10.26.14.217")
                .addMember("10.26.14.218")
                .addMember("10.26.14.219")
                .addMember("10.26.14.220")
                .addMember("10.26.14.221")
                .addMember("10.26.14.222")
                .addMember("10.26.14.223");

        config.getNetworkConfig().setPublicAddress(args[0]+":5701");

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        IMap<Integer, Boolean> pagesMap = hazelcastInstance.getMap("pagesMap");
        IMap<Integer, Map<String, String>> metadata = hazelcastInstance.getMap("metadata");

        initializePagesMap(hazelcastInstance, pagesMap);

        CrawlerThread crawler = new CrawlerThread();
        GutenbergTokenizer tokenizer = new GutenbergTokenizer("stopwords.txt");
        FilePerWordInvertedIndexHazelcast indexer = new FilePerWordInvertedIndexHazelcast("gutenberg_books", tokenizer, args);

        while (true) {
            Integer pageToProcess = getNextPage(hazelcastInstance, pagesMap);
            if (pageToProcess == null) {
                System.out.println("No quedan páginas por procesar. Finalizando nodo...");
                break;
            }

            System.out.println("Nodo procesando página: " + pageToProcess);

            crawler.fetchBooks(pageToProcess);
            crawler.shutdownExecutor();

            indexer.indexAll();

            System.out.println("Página procesada: " + pageToProcess);

            File directory = new File("gutenberg_books");

            crawler.deleteDirectoryContents(directory);
        }

        File metadataFile = new File("gutenberg_data.txt");

        crawler.loadMetadataFromFile(metadataFile, metadata);

        ApiApplication.main(args);

//        hazelcastInstance.shutdown();
    }

    private static void initializePagesMap(HazelcastInstance hazelcastInstance, IMap<Integer, Boolean> pagesMap) {
        Lock lock = hazelcastInstance.getCPSubsystem().getLock("initializationLock");
        lock.lock();
        try {
            if (pagesMap.isEmpty()) {
                System.out.println("Creating Pages Map");
                for (int i = 0; i <= 2100; i += 25) {
                    pagesMap.put(i, false);
                }
            } else {
                System.out.println("Map already initialized");
            }
        } finally {
            lock.unlock();
        }
    }

    private static Integer getNextPage(HazelcastInstance hazelcastInstance, IMap<Integer, Boolean> pagesMap) {
        for (Integer page : pagesMap.keySet()) {
            if (!pagesMap.get(page)) {
                Lock lock = hazelcastInstance.getCPSubsystem().getLock("pageLock-" + page);
                lock.lock();
                try {
                    if (!pagesMap.get(page)) {
                        pagesMap.put(page, true);
                        return page;
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
        return null;
    }
}
