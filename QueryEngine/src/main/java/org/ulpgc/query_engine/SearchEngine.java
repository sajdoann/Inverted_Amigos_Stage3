package org.ulpgc.query_engine;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ulpgc.inverted_index.implementations.BinaryDatamartReader;
import org.ulpgc.inverted_index.apps.ResponseList;
import org.ulpgc.inverted_index.apps.TrieInvertedIndex;

public class SearchEngine implements SearchEngineInterface {

    private List<Map<String, String>> metadata;
    private static final String PATH_TO_METADATA = "gutenberg_data.txt";
    private static final String PATH_TO_HASHED_INDEX = "InvertedIndex/datamart/bucket_%s.dat";
    private static final int BUCKETS_NUMBER = 8;
    private static final String PATH_TO_DIRECTORY_INDEX = "datamart2";
    private static final String PATH_TO_TRIE_DIRECTORY_INDEX = "indexes/trie_directory";
    private static final String PATH_TO_BOOKS_CONTENT_DIRECTORY = "data/books_content";
    private static final String TRIE_END_OF_WORD_FILENAME = "-.txt";

    private ResponseList searchForBooksWithWord(String word, String indexer) {
        ResponseList list = new ResponseList();
        if(Objects.equals(indexer, "hashed"))
            list = searchInHashedIndex(word);
        else if (Objects.equals(indexer, "directory")) {
            list = searchInDirectoryIndex(word);
        } else if (Objects.equals(indexer, "trie")) {
            list = searchInTrieIndex(word);
        }
        // @TODO maybe add some default value or raise an error
        return list;
    }

    @Override
    public MultipleWordsResponseList searchForBooksWithMultipleWords(String[] words, String indexer) {
        List<ResponseList> accumulateList = new ArrayList<ResponseList>();
        for(String word : words) {
            ResponseList partialList = searchForBooksWithWord(word, indexer);
            accumulateList.add(partialList);
        }
        return compileResultsForManyWords(accumulateList);
    }

    @Override
    public MultipleWordsResponseList searchForMultiplewithCriteria(String indexer, String[] words, String title, String author, String date, String language) {
        MultipleWordsResponseList initialResults = searchForBooksWithMultipleWords(words, indexer);
        if (title != null) {
            initialResults = filterWithMetadata(initialResults, Field.TITLE, title);
        }
        if (author != null) {
            initialResults = filterWithMetadata(initialResults, Field.AUTHOR, author);
        }
        if (date != null) {
            initialResults = filterWithMetadata(initialResults, Field.RELEASE_DATE, date);
        }
        if (language != null) {
            initialResults = filterWithMetadata(initialResults, Field.LANGUAGE, language);
        }
        return initialResults;
    }

    private MultipleWordsResponseList compileResultsForManyWords(List<ResponseList> results) {
        MultipleWordsResponseList compiledResults = new MultipleWordsResponseList();
        int differentWordsNumber = results.size();

        // Handle the case where there is only one ResponseList in the results
        if (differentWordsNumber == 1) {
            ResponseList singleResult = results.get(0);
            for (Map.Entry<Integer, List<Integer>> entry : singleResult.getResults()) {
                // Wrap the existing results in a format compatible with MultipleWordsResponseList
                List<List<Integer>> singlePositionList = new ArrayList<>();
                singlePositionList.add(new ArrayList<>(entry.getValue()));

                Map.Entry<Integer, List<List<Integer>>> compiledEntry = new AbstractMap.SimpleEntry<>(entry.getKey(), singlePositionList);
                compiledResults.addResult(compiledEntry);
            }
            return compiledResults;
        }

        // Get the first list of results as the reference list
        List<Map.Entry<Integer, List<Integer>>> firstResult = results.get(0).getResults();

        // Iterate over the first term's result entries (bookId and positions)
        for (Map.Entry<Integer, List<Integer>> book1 : firstResult) {
            int book1Id = book1.getKey();
            List<List<Integer>> combinedPositions = new ArrayList<>();
            boolean isInAllResults = true;

            // Add positions from the first result to combinedPositions
            combinedPositions.add(new ArrayList<>(book1.getValue()));

            // Check for this bookId in the rest of the results
            for (int i = 1; i < differentWordsNumber; i++) {
                List<Map.Entry<Integer, List<Integer>>> wordResults = results.get(i).getResults();
                boolean foundMatch = false;

                // Search for book1Id in the current word's results
                for (Map.Entry<Integer, List<Integer>> book2 : wordResults) {
                    if (book2.getKey().equals(book1Id)) {
                        combinedPositions.add(new ArrayList<>(book2.getValue()));
                        foundMatch = true;
                        break;
                    }
                }

                // If book1Id is not found in one of the lists, stop processing this bookId
                if (!foundMatch) {
                    isInAllResults = false;
                    break;
                }
            }

            // Only add book1Id if it is present in all ResultLists
            if (isInAllResults) {
                Map.Entry<Integer, List<List<Integer>>> compiledEntry = new AbstractMap.SimpleEntry<>(book1Id, combinedPositions);
                compiledResults.addResult(compiledEntry);
            }
        }

        return compiledResults;
    }

    private ResponseList searchInHashedIndex(String word) {
        int bucket = Math.abs(word.hashCode() % BUCKETS_NUMBER);
        Map<String, ResponseList> index = new BinaryDatamartReader(String.format(PATH_TO_HASHED_INDEX, bucket)).read();
        return index.get(word);
    }

    private ResponseList searchInTrieIndex(String word) {
        ResponseList response = new ResponseList();
        try {
            TrieInvertedIndex invertedIndex = new TrieInvertedIndex();
            String directory = "gutenberg_books";
            invertedIndex.indexBooks(directory);
            Map<String, List<Integer>> results = invertedIndex.searchWord(word);
            if (results != null && !results.isEmpty()) {
                for (Map.Entry<String, List<Integer>> entry : results.entrySet()) {
                    Map.Entry<Integer, List<Integer>> newEntry = new AbstractMap.SimpleEntry<>(Integer.parseInt(entry.getKey()), entry.getValue());
                    response.addResult(newEntry);
                }
            } else {
                System.out.println("No results found for '" + word + "'.");
            }
        } catch (Exception e) {
            System.err.println("Error during indexing: " + e.getMessage());
        }
        return response;
    }

    private ResponseList searchInDirectoryIndex(String word) {
        String pathToFileForWord = PATH_TO_DIRECTORY_INDEX + "/" + word.toLowerCase() + ".txt";
        File fileForWord = new File(System.getProperty("user.dir"), pathToFileForWord);
        return parseFileForWord(fileForWord);
    }

    private ResponseList searchInTrieDirectoryIndex(String word) {
        String pathToFileForWord = String.join("/",
                PATH_TO_TRIE_DIRECTORY_INDEX,
                String.join("/", word.toLowerCase().split("")),
                TRIE_END_OF_WORD_FILENAME
        );
        File fileForWord = new File(System.getProperty("user.dir"), pathToFileForWord);
        return parseFileForWord(fileForWord);
    }

    private ResponseList parseFileForWord(File file) {
        // parses into ResponseList files of format 100: 12, 13, 14 ...
        ResponseList responseList = new ResponseList();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    Integer bookId = Integer.parseInt(parts[0].trim());
                    String positionStr = parts[1].trim();

                    List<Integer> positions = parsePositions(positionStr);

                    Map.Entry<Integer, List<Integer>> entry = new AbstractMap.SimpleEntry<>(bookId, positions);
                    responseList.addResult(entry);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading directory index file: " + e.getMessage());
        }
        return responseList;
    }

    private List<Integer> parsePositions(String positionsStr) {
        List<Integer> positions = new ArrayList<>();
        String[] positionsArray = positionsStr.split(",");
        for (String position : positionsArray) {
            positions.add(Integer.parseInt(position.trim()));
        }
        return positions;
    }

    private MultipleWordsResponseList filterWithMetadata(MultipleWordsResponseList results, Field field, String value) {
        // Load metadata if it hasn't been loaded already
        if (metadata == null || metadata.isEmpty()) {
            File metadataFile = new File(System.getProperty("user.dir"), PATH_TO_METADATA);
            loadMetadataFromFile(metadataFile);
        }

        MultipleWordsResponseList filteredResults = new MultipleWordsResponseList();
        String targetField = field.getValue();

        for (Map.Entry<Integer, List<List<Integer>>> obj : results.getResults()) {

            Integer bookId = obj.getKey();
            List<List<Integer>> positions = obj.getValue(); // Could be List<Integer> or List<List<Integer>>

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

                        // Check if the target field value contains the search string
                        if (fieldValue != null && fieldValue.toLowerCase().contains(value.toLowerCase())) {
                            Map.Entry<Integer, List<List<Integer>>> filteredEntry =
                                    new AbstractMap.SimpleEntry<>(bookId, positions);
                            filteredResults.addResult(filteredEntry);
                        }
                        break; // Stop searching the metadata for the current bookId
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid ID format in metadata: " + bookIdString);
                }
            }
        }
        return filteredResults;
    }

    private void loadMetadataFromFile(File metadataFile) {
        Pattern pattern = Pattern.compile("(\\w+)\\s*:\\s*([^,]+)");
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

    @Override
    public TextFragment getPartOfBookWithWord(Integer bookId, Integer wordId) {
        String fileRelativePath = PATH_TO_BOOKS_CONTENT_DIRECTORY + "/" + bookId + ".txt";
        File filePath = Paths.get(System.getProperty("user.dir"), fileRelativePath).toFile();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            int currPos = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\s+");
                currPos += words.length;

                if (currPos > wordId) {
                    int positionInLine = wordId - (currPos - words.length);
                    String lineContent = String.join(" ", words);
                    return new TextFragment(lineContent, positionInLine);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading book content: " + e.getMessage());
        }

        // If the wordId is not found, return an empty TextFragment or handle error accordingly
        return new TextFragment("Word not found", -1);
    }
}
