package org.ulpgc.inverted_index.implementations;
import org.ulpgc.inverted_index.apps.ResponseList;
import org.ulpgc.inverted_index.ports.Tokenizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


public class GutenbergTokenizer implements Tokenizer {

    private final String stopwords_file;

    public GutenbergTokenizer(String stopwords_file) {
        this.stopwords_file = stopwords_file;
    }

    @Override
    public Map<String, ResponseList> tokenize(String book, int bookID) {
        try {
            Set<String> stopwords = readStopwords(this.stopwords_file);
            return processText(book, stopwords, bookID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> readStopwords(String stopwordsFile) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(stopwordsFile);

        if (inputStream == null) {
            throw new IOException("El archivo de stopwords no fue encontrado: " + stopwordsFile);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines()
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }
    }

    private Map<String, ResponseList> processText(String book, Set<String> stopwords, int bookID) throws IOException {
        Map<String, ResponseList> wordsWithPositions = new HashMap<>();
        int actualPosition = 0;

        List<String> lines = Files.lines(Paths.get(book))
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        for (String line : lines) {
            String[] words = line.replaceAll("[\\[\\]\\\\,.;:º\\dª&`’´\\-_()¡\"!?¿{}=+<>|^“‘/$™%—•*”]", " ").split("\\s+");

            for (String word : words) {
                if (!word.isEmpty()) {
                    if (!stopwords.contains(word)) {
                        ResponseList responseList = wordsWithPositions.computeIfAbsent(word, k -> new ResponseList());

                        Optional<Map.Entry<Integer, List<Integer>>> entryOpt = responseList.getResults().stream()
                                .filter(entry -> entry.getKey() == bookID)
                                .findFirst();

                        if (entryOpt.isPresent()) {
                            entryOpt.get().getValue().add(actualPosition);
                        } else {
                            List<Integer> positions = new ArrayList<>();
                            positions.add(actualPosition);
                            responseList.addResult(new AbstractMap.SimpleEntry<>(bookID, positions));
                        }
                    }
                    actualPosition++;
                }
            }
        }

        return wordsWithPositions;
    }
}
