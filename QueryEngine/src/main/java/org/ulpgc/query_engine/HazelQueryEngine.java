package org.ulpgc.query_engine;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.json.ParseException;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HazelQueryEngine implements SearchEngineInterface {

    private final HazelcastInstance hazelcastInstance;
    private final MultiMap<String, Integer> wordToBookMap; // word -> bookId
    private final MultiMap<String, Integer> wordBookToPositionsMap; // word|bookId -> positions

    private final IMap<Integer, Map<String, String>> metadata; // bookId -> {title="asas", ...}
    private IMap<String, Boolean> indexedMap;

    private static final String PATH_TO_METADATA = "gutenberg_data.txt";

    public HazelQueryEngine() {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig()
                .setEnabled(true)
                .addMember("10.193.36.90")
                .addMember("10.193.132.48");

        this.hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        this.wordToBookMap = hazelcastInstance.getMultiMap("wordToBookMap");
        this.wordBookToPositionsMap = hazelcastInstance.getMultiMap("wordBookToPositionsMap");

        this.indexedMap = hazelcastInstance.getMap("indexedMap");
        this.metadata = hazelcastInstance.getMap("metadata");
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

    private MultipleWordsResponseList searchForBooksWithMultipleWords(String[] words) {
        MultipleWordsResponseList responseList = new MultipleWordsResponseList();
        Map<Integer, Map<String, List<Integer>>> bookWordPositionsMap = new HashMap<>();

        for (String word : words) {
            // Obtener todos los libros que contienen la palabra
            Collection<Integer> bookIds = wordToBookMap.get(word);
            if (bookIds != null) {
                for (Integer bookId : bookIds) {
                    // Construir la clave para wordBookToPositionsMap
                    String key = word + "|" + bookId;

                    Collection<Integer> positions = wordBookToPositionsMap.get(key);
                    if (positions != null) {
                        // Convertir las posiciones en lista
                        List<Integer> positionList = new ArrayList<>(positions);

                        // Obtener o crear el mapa de posiciones por palabra para el libro
                        bookWordPositionsMap.computeIfAbsent(bookId, k -> new HashMap<>())
                                .put(word, positionList);
                    }
                }
            }
        }

        bookWordPositionsMap.forEach((bookId, wordPositionsMap) -> {
            if (wordPositionsMap.keySet().containsAll(Arrays.asList(words))) {
                responseList.addResult(bookId, wordPositionsMap);
            }
        });

        return responseList;
    }

    @Override
    public MultipleWordsResponseList searchForMultiplewithCriteria(String[] words, String title, String author, String from, String to, String language) {
        // Filter criteria are placeholder as there is no direct mapping for title, author, date, or language in the datamart.
        MultipleWordsResponseList responseList = searchForBooksWithMultipleWords(words);
        // implement filtering
        if (title != null) {
            responseList = filterWithMetadata(responseList, Field.TITLE, title);
        }
        if (author != null) {
            responseList = filterWithMetadata(responseList, Field.AUTHOR, author);
        }
        if (from != null) {
            responseList = filterWithMetadata(responseList, Field.FROM, from);
        }
        if (to != null) {
            responseList = filterWithMetadata(responseList, Field.TO, to);
        }
        if (language != null) {
            responseList = filterWithMetadata(responseList, Field.LANGUAGE, language);
        }

        return responseList;
    }

    private MultipleWordsResponseList filterWithMetadata(MultipleWordsResponseList results, Field field, String value) {
        if (metadata == null || metadata.isEmpty()) {
            File metadataFile = new File(System.getProperty("user.dir"), PATH_TO_METADATA);
            loadMetadataFromFile(metadataFile);
        }

        MultipleWordsResponseList filteredResults = new MultipleWordsResponseList();

        for (SearchResultsMW obj : results.getResults()) {

            Integer bookId = obj.getBookId();
            Map<String, List<Integer>> positions = obj.getPositions();

            String fieldValue = metadata.get(bookId).get(field.getValue());
            if (field.getValue().equals("Date"))
                try {
                    if (field == Field.FROM) {
                        int bookYear = extractYear(fieldValue);
                        if (bookYear < Integer.parseInt(value)) continue;
                    } else if (field == Field.TO) {
                        int bookYear = extractYear(fieldValue);
                        if (bookYear > Integer.parseInt(value)) continue;
                    }
                    filteredResults.addResult(bookId, positions);
                } catch (java.text.ParseException e) {
                    System.err.println("Wrong release date format for" + bookId);
                }
            else if (fieldValue != null && fieldValue.toLowerCase().contains(value.toLowerCase())) {
                filteredResults.addResult(bookId, positions);
            }
        }
        return filteredResults;
    }

    private int extractYear(String stringDate) throws ParseException, java.text.ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy");
        Date date = formatter.parse(stringDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    private void loadMetadataFromFile(File metadataFile) {
        Pattern pattern = Pattern.compile("(\\w+)\\s*:\\s*((?:\\w+\\s+\\d{1,2},\\s+\\d{4})|[^,]+)");
        try (BufferedReader reader = new BufferedReader(new FileReader(metadataFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                Map<String, String> bookData = new HashMap<>();
                Matcher matcher = pattern.matcher(line);
                int bookId = -1;

                while (matcher.find()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    if (key.equals("ID")) bookId = Integer.parseInt(value);
                    else bookData.put(key, value);
                }

                metadata.put(bookId, bookData);
            }
        } catch (IOException e) {
            System.err.println("Error reading metadata file: " + e.getMessage());
        }
    }

    @Override
    public TextFragment getPartOfBookWithWord(Integer bookId, Integer wordId) {
        return new TextFragment("Sample text containing word " + wordId + " in book " + bookId);
    }

    public IMap<String, Boolean> getIndexedMap() {
        return this.indexedMap;
    }
}

