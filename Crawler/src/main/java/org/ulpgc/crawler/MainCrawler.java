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
        // Configuración de Hazelcast
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig()
                .setEnabled(true)
                .addMember("10.26.14.225")
                .addMember("10.26.14.226")
                .addMember("10.26.14.227")
                .addMember("10.26.14.228")
                .addMember("10.26.14.229")
                .addMember("10.26.14.230")
                .addMember("10.26.14.231")
                .addMember("10.26.14.232")
                .addMember("10.26.14.233")
                .addMember("10.26.14.234")
                .addMember("10.26.14.235")
                .addMember("10.26.14.236")
                .addMember("10.26.14.237")
                .addMember("10.26.14.238")
                .addMember("10.26.14.239")
                .addMember("10.26.14.240")
                .addMember("10.26.14.241")
                .addMember("10.26.14.242")
                .addMember("10.26.14.243")
                .addMember("10.26.14.244")
                .addMember("10.26.14.245")
                .addMember("10.26.14.246")
                .addMember("10.26.14.247")
                .addMember("10.26.14.248")
                .addMember("10.26.14.249")
                .addMember("10.26.14.250")
                .addMember("10.26.14.251")
                .addMember("10.26.14.252")
                .addMember("10.26.14.253")
                .addMember("10.26.14.254");

        config.getNetworkConfig().setPublicAddress(args[0]+":5701");

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        IMap<Integer, Boolean> pagesMap = hazelcastInstance.getMap("pagesMap");
        IMap<Integer, Map<String, String>> metadata = hazelcastInstance.getMap("metadata");

        // Inicializar el mapa de páginas si está vacío
        initializePagesMap(hazelcastInstance, pagesMap);

        // Crear instancias del crawler y del indexador
        CrawlerThread crawler = new CrawlerThread();
        GutenbergTokenizer tokenizer = new GutenbergTokenizer("stopwords.txt");
        FilePerWordInvertedIndexHazelcast indexer = new FilePerWordInvertedIndexHazelcast("gutenberg_books", tokenizer, args);

        // Procesar las páginas
        while (true) {
            Integer pageToProcess = getNextPage(hazelcastInstance, pagesMap);
            if (pageToProcess == null) {
                System.out.println("No quedan páginas por procesar. Finalizando nodo...");
                break;
            }

            System.out.println("Nodo procesando página: " + pageToProcess);

            crawler.fetchBooks(pageToProcess);
            crawler.shutdownExecutor();

            // Indexar los libros descargados
            indexer.indexAll();

            System.out.println("Página procesada: " + pageToProcess);

            File directory = new File("gutenberg_books");

            crawler.deleteDirectoryContents(directory);
        }

        File metadataFile = new File("gutenberg_data.txt");

        crawler.loadMetadataFromFile(metadataFile, metadata);

        ApiApplication.main(args);

        // Finalizar el nodo
//        hazelcastInstance.shutdown();
    }

    private static void initializePagesMap(HazelcastInstance hazelcastInstance, IMap<Integer, Boolean> pagesMap) {
        Lock lock = hazelcastInstance.getCPSubsystem().getLock("initializationLock");
        lock.lock();
        try {
            if (pagesMap.isEmpty()) {
                System.out.println("Inicializando mapa de páginas...");
                for (int i = 0; i <= 250; i += 25) {
                    pagesMap.put(i, false); // false indica que la página no ha sido procesada
                }
            } else {
                System.out.println("Mapa de páginas ya inicializado.");
            }
        } finally {
            lock.unlock();
        }
    }

    private static Integer getNextPage(HazelcastInstance hazelcastInstance, IMap<Integer, Boolean> pagesMap) {
        for (Integer page : pagesMap.keySet()) {
            if (!pagesMap.get(page)) { // Verificar si la página no ha sido procesada
                Lock lock = hazelcastInstance.getCPSubsystem().getLock("pageLock-" + page);
                lock.lock();
                try {
                    if (!pagesMap.get(page)) {
                        pagesMap.put(page, true); // Marcar la página como procesada
                        return page;
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
        return null; // No quedan páginas por procesar
    }
}

