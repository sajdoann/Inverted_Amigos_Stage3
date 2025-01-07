package org.ulpgc.query_engine;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;

import java.util.*;
import java.util.stream.Collectors;

public class HazelQueryEngine implements SearchEngineInterface {

    private final HazelcastInstance hazelcastInstance;
    private final MultiMap<String, Integer> wordToBookMap; // word -> bookId
    private final MultiMap<String, Integer> wordBookToPositionsMap; // word|bookId -> positions

    private IMap<String, Boolean> indexedMap;

    public HazelQueryEngine() {
        Config config = new Config();
        config.getNetworkConfig().getInterfaces()
                .setEnabled(true)
                .addInterface("192.168.191.*"); // Ajustar IP para entorno de laboratorio
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);

        this.hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        this.wordToBookMap = hazelcastInstance.getMultiMap("wordToBookMap");
        this.wordBookToPositionsMap = hazelcastInstance.getMultiMap("wordBookToPositionsMap");

        this.indexedMap = hazelcastInstance.getMap("indexedMap");
    }

    public void maps_size() {
        System.out.println("Hazelcast MultiMap wordToBookMap size: " + wordToBookMap.size());
        System.out.println("Hazelcast MultiMap wordBookToPositionsMap size: " + wordBookToPositionsMap.size());
    }

    public Collection<Integer> searchBooksByWord(String word) {
        return wordToBookMap.get(word);
    }

    public Collection<Integer> searchWordPositionsInBook(String word, int bookId) {
        return wordBookToPositionsMap.get(word + "|" + bookId);
    }

    @Override
    public MultipleWordsResponseList searchForBooksWithMultipleWords(String[] words, String indexer) {
        return searchForMultiplewithCriteria(indexer, words, null, null, null, null);
    }

    /* Search using Hazelcast */
    @Override
    public MultipleWordsResponseList searchForMultiplewithCriteria(
            String indexer, String[] words, String title, String author, String date, String language) {
        // No hay filtros por title, author, date, o language en el MultiMap
        MultipleWordsResponseList responseList = new MultipleWordsResponseList();

        for (String word : words) {
            // Buscar en el MultiMap wordToBookMap
            if (wordToBookMap.containsKey(word)) {
                Collection<Integer> bookIds = wordToBookMap.get(word);

                for (Integer bookId : bookIds) {
                    String key = word + "|" + bookId;
                    Collection<Integer> positions = wordBookToPositionsMap.get(key);

                    if (positions != null && !positions.isEmpty()) {
                        List<Integer> positionList = new ArrayList<>(positions);
                        responseList.addResult(bookId, word, positionList);
                    }
                }
            }
        }
        return responseList;
    }

    /* Get a part of a book that contains the specified word */
    @Override
    public TextFragment getPartOfBookWithWord(Integer bookId, Integer wordId) {
        // Placeholder: Esto requeriría acceder al contenido del libro por ID.
        return new TextFragment("Sample text containing word " + wordId + " in book " + bookId);
    }

    public IMap<String, Boolean> getIndexedMap() {
        return this.indexedMap;
    }
}

