package org.ulpgc.query_engine;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class HazelQueryEngine implements SearchEngineInterface {

    private final HazelcastInstance hazelcastInstance;
    private final IMap<String, Object> map;
    private Config config;
    private List<Map<String, String>> metadata;
    private static final String PATH_TO_METADATA = "gutenberg_data.txt";

    public HazelQueryEngine() {
        this.config = new Config();
        config.getNetworkConfig().getInterfaces()
                        .setEnabled(true)
                        .addInterface("192.168.1.*"); // check your IP add yours! todo: make this work for lab computers
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        this.hazelcastInstance = Hazelcast.newHazelcastInstance(config);
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

    private MultipleWordsResponseList searchForBooksWithMultipleWords(String[] words) {
        MultipleWordsResponseList responseList = new MultipleWordsResponseList();
        Map<Integer, Map<String, List<Integer>>> bookWordPositionsMap = new HashMap<>();

        for (String word : words) {
            Object value = map.get(word);
            if (value instanceof Map<?, ?> occurrencesMap) {
                occurrencesMap.forEach((bookId, positions) -> {
                    // Convert int[] to List<Integer>
                    List<Integer> positionList = Arrays.stream((int[]) positions)
                            .boxed()
                            .collect(Collectors.toList());

                    // Get or create the word-positions map for the book
                    bookWordPositionsMap.computeIfAbsent((Integer) bookId, k -> new HashMap<>())
                            .put(word, positionList);
                });
            }
        }

        // Add results to the response list
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
        // Load metadata if it hasn't been loaded already
        if (metadata == null || metadata.isEmpty()) {
            File metadataFile = new File(System.getProperty("user.dir"), PATH_TO_METADATA);
            loadMetadataFromFile(metadataFile);
        }

        MultipleWordsResponseList filteredResults = new MultipleWordsResponseList();
        String targetField = field.getValue();

        for (SearchResultsMW obj : results.getResults()) {

            Integer bookId = obj.getBookId();
            Map<String, List<Integer>> positions = obj.getPositions(); // Could be List<Integer> or List<List<Integer>>

            // Find corresponding metadata entry for the current bookId
            for (Map<String, String> book : metadata) {
                String bookIdString = book.get("ID");

                if (bookIdString == null || bookIdString.isEmpty()) {
                    System.err.println("Metadata entry with missing or empty ID.");
                    continue; // Skip to the next book in metadata if ID is missing
                }

                try {
                    Integer metadataBookId = Integer.parseInt(bookIdString);

                    // Check if book ID in metadata matches the result book ID
                    if (metadataBookId.equals(bookId)) {
                        String fieldValue = book.get(targetField);
                        if (targetField.equals("Date")) {
                            int bookYear = extractYear(fieldValue);
                            if (field == Field.FROM) {
                                if (bookYear < Integer.parseInt(value)) break;
                            }
                            if (field == Field.TO) {
                                if (bookYear > Integer.parseInt(value)) break;
                            }
                            filteredResults.addResult(bookId, positions);
                        }
                        // Check if the target field value contains the search string
                        else if (fieldValue != null && fieldValue.toLowerCase().contains(value.toLowerCase())) {
                            filteredResults.addResult(bookId, positions);
                        }
                        break; // Stop searching the metadata for the current bookId
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid ID format in metadata: " + bookIdString);
                } catch (ParseException e) {
                    System.err.println("Invalid date format in metadata for: " + bookIdString);
                }
            }
        }
        return filteredResults;
    }

    private int extractYear(String stringDate) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy");
        Date date = formatter.parse(stringDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    private void loadMetadataFromFile(File metadataFile) {
        Pattern pattern = Pattern.compile("(\\w+)\\s*:\\s*((?:\\w+\\s+\\d{1,2},\\s+\\d{4})|[^,]+)");
        metadata = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(metadataFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                Map<String, String> bookData = new HashMap<>();
                Matcher matcher = pattern.matcher(line);

                while (matcher.find()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    bookData.put(key, value);
                }

                metadata.add(bookData);
            }
        } catch (IOException e) {
            System.err.println("Error reading metadata file: " + e.getMessage());
        }
    }

    /* Get a part of a book that contains the specified word */
    @Override
    public TextFragment getPartOfBookWithWord(Integer bookId, Integer wordId) {
        // Placeholder: This would require accessing book content by ID.
        return new TextFragment("Sample text containing word " + wordId + " in book " + bookId);
    }
}
