package org.ulpgc.query_engine;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class HazelQueryEngine implements SearchEngineInterface {

    private final HazelcastInstance hazelcastInstance;
    private final IMap<String, Object> map;

    public HazelQueryEngine() {
        this.hazelcastInstance = Hazelcast.newHazelcastInstance();
        this.map = hazelcastInstance.getMap("datamart-map");
    }

    /* Load the data from datamart2 into Hazelcast */
    public void loadData(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory: " + directoryPath);
        }

        for (File file : directory.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".txt")) {
                String word = file.getName().replace(".txt", "");
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    Map<Integer, int[]> occurrences = new HashMap<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            int bookId = Integer.parseInt(parts[0].trim());
                            String[] positionsStr = parts[1].trim().split(",");
                            int[] positions = Arrays.stream(positionsStr)
                                    .map(String::trim)
                                    .mapToInt(Integer::parseInt)
                                    .toArray();
                            occurrences.put(bookId, positions);
                        }
                    }
                    map.put(word, occurrences);
                } catch (IOException e) {
                    System.err.println("Error reading file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public MultipleWordsResponseList searchForBooksWithMultipleWords(String[] words, String indexer) {
        return null;
    }

    /* Search using Hazelcast */
    @Override
    public MultipleWordsResponseList searchForMultiplewithCriteria(String indexer, String[] words, String title, String author, String date, String language) {
        // Filter criteria are placeholder as there is no direct mapping for title, author, date, or language in the datamart.
        MultipleWordsResponseList responseList = new MultipleWordsResponseList();
        for (String word : words) {
            Object value = map.get(word);
            if (value instanceof Map<?, ?> occurrencesMap) {
                occurrencesMap.forEach((bookId, positions) -> {
                    // Convert int[] to List<Integer>
                    List<Integer> positionList = Arrays.stream((int[]) positions)
                            .boxed()
                            .collect(Collectors.toList());
                    responseList.addResult((Integer) bookId, word, positionList);
                });
            }
        }
        return responseList;
    }

    /* Get a part of a book that contains the specified word */
    @Override
    public TextFragment getPartOfBookWithWord(Integer bookId, Integer wordId) {
        // Placeholder: This would require accessing book content by ID.
        return new TextFragment("Sample text containing word " + wordId + " in book " + bookId);
    }
}
