package org.ulpgc.inverted_index.apps;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
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

    public FilePerWordInvertedIndexHazelcast(String books, GutenbergTokenizer tokenizer) {
        this.books = new File(books);
        this.tokenizer = tokenizer;

        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig()
                .setEnabled(true)
                .addMember("10.193.36.90")
                .addMember("10.193.132.48");

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        this.indexedMap = hazelcastInstance.getMap("indexedMap");
        this.wordToBookMap = hazelcastInstance.getMultiMap("wordToBookMap");
        this.wordBookToPositionsMap = hazelcastInstance.getMultiMap("wordBookToPositionsMap");

        if (indexedMap.isEmpty()) {
            System.out.println("Indexed Map is empty, ready to index books.");
        }
    }

    public FilePerWordInvertedIndexHazelcast(File books, GutenbergTokenizer tokenizer, IMap<String, Boolean> indexedMap, MultiMap<String, Integer> wordToBookMap, MultiMap<String, Integer> wordBookToPositionsMap) {
        this.books = books;
        this.tokenizer = tokenizer;
        this.indexedMap = indexedMap;
        this.wordToBookMap = wordToBookMap;
        this.wordBookToPositionsMap = wordBookToPositionsMap;
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