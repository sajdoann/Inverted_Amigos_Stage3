package org.ulpgc.inverted_index.apps;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.ISemaphore;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.map.IMap;
import org.ulpgc.inverted_index.implementations.GutenbergTokenizer;

import java.io.*;
import java.util.*;

public class FilePerWordInvertedIndexHazelcast {
    private final File books;
    private final GutenbergTokenizer tokenizer;

    // Hazelcast Maps
    private final IMap<String, Boolean> indexedMap;  // Map de libros indexados en Hazelcast
    private final MultiMap<String, Integer> wordToBookMap; // word -> bookId
    private final MultiMap<String, Integer> wordBookToPositionsMap; // word|bookId -> positions

    private final ISemaphore semaphoreWTB;
    private final ISemaphore semaphoreBTP;

    public FilePerWordInvertedIndexHazelcast(String books, GutenbergTokenizer tokenizer, String[] args) {
        this.books = new File(books);
        this.tokenizer = tokenizer;

        /*Config config = new Config();
        config.getNetworkConfig().getInterfaces()
                .setEnabled(true)
                .addInterface("192.168.191.*"); // Ajustar IP para entorno de laboratorio
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);*/
        Config config = new Config();
        config.getNetworkConfig().getJoin().getTcpIpConfig()
                .setEnabled(true)
                .addMember("10.26.14.225")
                .addMember("10.26.14.226")
                .addMember("10.26.14.227")
                //.addMember("10.26.14.228")
                .addMember("10.26.14.239")
                .addMember("10.26.14.240")
                .addMember("10.26.14.241");
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().setPublicAddress(args[0]+":5701");

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        this.semaphoreWTB = hazelcastInstance.getCPSubsystem().getSemaphore("wordToBook");
        this.semaphoreBTP = hazelcastInstance.getCPSubsystem().getSemaphore("bookToPosition");

        semaphoreWTB.init(1);
        semaphoreBTP.init(1);

        this.indexedMap = hazelcastInstance.getMap("indexedMap");
        this.wordToBookMap = hazelcastInstance.getMultiMap("wordToBookMap");
        this.wordBookToPositionsMap = hazelcastInstance.getMultiMap("wordBookToPositionsMap");

        if (indexedMap.isEmpty()) {
            System.out.println("Indexed Map is empty, ready to index books.");
        }
    }

    public FilePerWordInvertedIndexHazelcast(File books, GutenbergTokenizer tokenizer, IMap<String, Boolean> indexedMap, MultiMap<String, Integer> wordToBookMap, MultiMap<String, Integer> wordBookToPositionsMap, ISemaphore semaphore, ISemaphore semaphoreWTB, ISemaphore semaphoreWTP) {
        this.books = books;
        this.tokenizer = tokenizer;
        this.indexedMap = indexedMap;
        this.wordToBookMap = wordToBookMap;
        this.wordBookToPositionsMap = wordBookToPositionsMap;
        this.semaphoreWTB = semaphoreWTB;
        this.semaphoreBTP = semaphoreWTP;
    }

    public List<String> listBooks() {
        List<String> booksPath = new ArrayList<>();
        File directory = this.books;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            assert files != null;
            for (File file : files) {
                if (file.isFile()) {
                    booksPath.add(file.getPath().replaceAll("\\\\", "/"));
                }
            }
        }
        return booksPath;
    }

    public boolean isIndexed(String file) {
        String id = new File(file).getName().replaceAll("\\D", "");
        if (!id.isEmpty()) {
            return indexedMap.containsKey(id);  // Consultamos el Map de Hazelcast
        }
        return false;
    }

    public void indexAll() throws InterruptedException {
        List<String> books = this.listBooks();
        for (String book : books) {
            this.index(book);
        }
    }

    public void index(String file) throws InterruptedException {
        String id = new File(file).getName().replaceAll("\\D", "");
        if (id.isEmpty()) {
            System.out.println("This is not a valid book");
            return;
        }

        if (isIndexed(file)) {
            System.out.println("Book already indexed");
            return;
        }

        System.out.println("Indexing book " + id);
        Map<String, ResponseList> index = this.tokenizer.tokenize(file, Integer.parseInt(id));
        updateHazelcast(index, Integer.parseInt(id));

        // Agregar el ID al Map de Hazelcast después de indexar
        indexedMap.put(id, true);
    }

    private void updateHazelcast(Map<String, ResponseList> index, int bookId) throws InterruptedException {
        // Crear los mapas temporales para todas las actualizaciones
        Map<String, Collection<Integer>> wordToBookMapTemp = new HashMap<>();
        Map<String, Collection<Integer>> wordBookToPositionsMapTemp = new HashMap<>();

        // Recolectar todas las entradas para wordToBookMap y wordBookToPositionsMap
        for (Map.Entry<String, ResponseList> entry : index.entrySet()) {
            String word = entry.getKey();
            ResponseList responseList = entry.getValue();

            // Crear una clave compuesta como word + "|" + bookId
            String key = word + "|" + bookId;

            // Agregar la clave (combinación de word + bookId) a wordToBookMap
            Collection<Integer> books = wordToBookMapTemp.computeIfAbsent(word, k -> new ArrayList<>());
            books.add(bookId);  // Agregar la clave a la lista de libros para esa palabra

            // Obtener las posiciones desde responseList
            Collection<Integer> responsePositions = responseList.getPositions();

            // Agregar las posiciones para wordBookToPositionsMap (debe ser una colección de posiciones)
            Collection<Integer> positions = wordBookToPositionsMapTemp.computeIfAbsent(key, k -> new HashSet<>());

            // Asegurarnos de que las posiciones se agregan correctamente
            if (responsePositions != null && !responsePositions.isEmpty()) {
                positions.addAll(responsePositions);  // Agregar las posiciones de la lista
            }

            // Verificar el estado de la colección de posiciones antes de subirlo a Hazelcast
        }


        // Adquirir semáforo para el acceso a wordToBookMap
        semaphoreWTB.acquire();
        try {
            // Hacer el cast adecuado para el tipo y agregar todas las entradas a la vez en wordToBookMap
            Map<? extends String, ? extends Collection<? extends Integer>> castedWordToBookMap = wordToBookMapTemp;
            wordToBookMap.putAllAsync((Map<? extends String, Collection<? extends Integer>>) castedWordToBookMap);
        } finally {
            semaphoreWTB.release();
        }

        // Adquirir semáforo para el acceso a wordBookToPositionsMap
        semaphoreBTP.acquire();
        try {
            // Hacer el cast adecuado para el tipo y agregar todas las entradas a la vez en wordBookToPositionsMap
            Map<? extends String, ? extends Collection<? extends Integer>> castedWordBookToPositionsMap = wordBookToPositionsMapTemp;
            wordBookToPositionsMap.putAllAsync((Map<? extends String, Collection<? extends Integer>>) castedWordBookToPositionsMap);
        } finally {
            semaphoreBTP.release();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        String booksDirectory = "gutenberg_books";

        GutenbergTokenizer tokenizer = new GutenbergTokenizer("stopwords.txt");

        FilePerWordInvertedIndexHazelcast indexer = new FilePerWordInvertedIndexHazelcast(booksDirectory, tokenizer, args);

        System.out.println("waking up");

        indexer.indexAll();
    }
}