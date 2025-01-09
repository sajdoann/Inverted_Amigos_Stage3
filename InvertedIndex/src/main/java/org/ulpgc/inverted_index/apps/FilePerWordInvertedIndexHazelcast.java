package org.ulpgc.inverted_index.apps;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import org.ulpgc.inverted_index.implementations.GutenbergTokenizer;

import java.io.File;
import java.util.*;

public class FilePerWordInvertedIndexHazelcast {
    private final File books;
    private final GutenbergTokenizer tokenizer;

    // Hazelcast Maps
    private final IMap<String, Boolean> indexedMap;
    private final MultiMap<String, Integer> wordToBookMap;
    private final MultiMap<String, Integer> wordBookToPositionsMap;

    private final HazelcastInstance hazelcastInstance;

    public FilePerWordInvertedIndexHazelcast(String books, GutenbergTokenizer tokenizer) {
        this.books = new File(books);
        this.tokenizer = tokenizer;

        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig()
                .setEnabled(true)
                .addMember("10.193.36.90")
                .addMember("10.193.132.48");

        this.hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        this.indexedMap = hazelcastInstance.getMap("indexedMap");
        this.wordToBookMap = hazelcastInstance.getMultiMap("wordToBookMap");
        this.wordBookToPositionsMap = hazelcastInstance.getMultiMap("wordBookToPositionsMap");

        if (indexedMap.isEmpty()) {
            System.out.println("Indexed Map is empty, ready to index books.");
        }
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
            return indexedMap.containsKey(id);
        }
        return false;
    }

    public void indexAll() {
        List<String> books = this.listBooks();
        for (String book : books) {
            this.index(book);
        }
    }

    public void index(String file) {
        String id = new File(file).getName().replaceAll("\\D", "");
        if (id.isEmpty()) {
            System.out.println("This is not a valid book");
            return;
        }

        // Adquirir lock para este libro
        FencedLock lock = hazelcastInstance.getCPSubsystem().getLock("indexLock-" + id);
        lock.lock();
        try {
            if (isIndexed(file)) {
                System.out.println("Book already indexed");
                return;
            }

            System.out.println("Indexing book " + id);
            Map<String, ResponseList> index = this.tokenizer.tokenize(file, Integer.parseInt(id));
            updateHazelcast(index, Integer.parseInt(id));

            // Agregar el ID al Map de Hazelcast después de indexar
            indexedMap.put(id, true);
        } finally {
            lock.unlock();
        }
    }

    private void updateHazelcast(Map<String, ResponseList> index, int bookId) {
        for (Map.Entry<String, ResponseList> entry : index.entrySet()) {
            String word = entry.getKey();
            ResponseList responseList = entry.getValue();

            wordToBookMap.put(word, bookId);

            for (int position : responseList.getPositions()) {
                wordBookToPositionsMap.put(word + "|" + bookId, position);
            }
        }
    }

    public static void main(String[] args) {
        String booksDirectory = "gutenberg_books";

        GutenbergTokenizer tokenizer = new GutenbergTokenizer("InvertedIndex/stopwords.txt");

        FilePerWordInvertedIndexHazelcast indexer = new FilePerWordInvertedIndexHazelcast(booksDirectory, tokenizer);

        indexer.indexAll();
    }
}
