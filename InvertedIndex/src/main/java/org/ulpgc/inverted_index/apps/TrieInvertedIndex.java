package org.ulpgc.inverted_index.apps;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.ulpgc.inverted_index.implementations.DocumentReader;
import org.ulpgc.inverted_index.ports.InvertedIndex;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class TrieInvertedIndex implements InvertedIndex {
    private Trie trie;
    private Map<String, String> bookMetadata;
    private final String indexedBooksFile = "indexed_books.txt";
    private Set<String> indexedBookIds;

    // Initialize the trie, metadata map, and loads previously indexed books
    public TrieInvertedIndex() {
        this.trie = new Trie();
        this.bookMetadata = new HashMap<>();
        this.indexedBookIds = new HashSet<>();
        loadIndexedBooks();
    }


    private void loadIndexedBooks() {
        File file = new File(indexedBooksFile);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creating indexed books file: " + e.getMessage());
            }
        } else {
            try {
                String line = Files.readString(file.toPath()).trim();
                if (!line.isEmpty()) {
                    String[] ids = line.split(",");
                    indexedBookIds.addAll(Arrays.asList(ids));
                }
            } catch (IOException e) {
                System.err.println("Error loading indexed books: " + e.getMessage());
            }
        }
    }

    // Save only the book ID to indexed_books.txt
    private void saveIndexedBook(String bookId) {
        try {
            File file = new File(indexedBooksFile);

            // Check if file exists and read the contents to verify if the bookId is already present
            boolean bookAlreadyIndexed = false;
            if (file.exists()) {
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    String[] ids = line.split(",");
                    for (String id : ids) {
                        if (id.trim().equals(bookId)) {
                            bookAlreadyIndexed = true;
                            break;
                        }
                    }
                    if (bookAlreadyIndexed) {
                        break;
                    }
                }
            }

            // If the book is not already indexed, add it
            if (!bookAlreadyIndexed) {
                // Check if file exists and is non-empty for appending a comma separator
                boolean fileExistsAndNotEmpty = file.exists() && file.length() > 0;

                // Open the file in append mode
                try (FileWriter fw = new FileWriter(file, true)) {
                    // Add a comma if there are already entries in the file
                    if (fileExistsAndNotEmpty) {
                        fw.write(",");
                    }
                    // Write the new book ID
                    fw.write(bookId);
                    fw.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving indexed book: " + e.getMessage());
        }
    }


    public void indexBooks(String directory) throws IOException {
        List<String> documents = DocumentReader.readDocumentsFromDirectory(directory, bookMetadata);
        for (String document : documents) {
            String bookId = bookMetadata.get(document).replace(".txt", "");  // Remove .txt extension if present

            String[] words = preprocessText(document);
            for (int position = 0; position < words.length; position++) {
                trie.insert(words[position], bookId, position);
            }
            saveIndexedBook(bookId);  // Save only the book ID (no .txt extension)
        }
        saveTrieAsMessagePack("trie_index_by_prefix");
    }


    @Override
    public void index(String directory) {
        File dir = new File(directory);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt")); // Filter for .txt files

        if (files == null) {
            System.out.println("No files found in the directory.");
            return;
        }

        // Loop through the files to find the first unindexed book
        for (File file : files) {
            String filePath = file.getAbsolutePath();
            String bookId = file.getName().replace(".txt", ""); // Extract the book ID

            if (!isBookIndexed(bookId)) {
                try {
                    String content = Files.readString(file.toPath());
                    String[] words = preprocessText(content);
                    for (int position = 0; position < words.length; position++) {
                        trie.insert(words[position], bookId, position);
                    }
                    bookMetadata.put(bookId, bookId);  // Mark the book as indexed in metadata
                    saveIndexedBook(bookId);  // Save the book ID as indexed
                    saveTrieAsMessagePack("trie_index_by_prefix");  // Save the trie structure
                    System.out.println("Indexed book: " + bookId);
                    return; // Exit after indexing the first unindexed book
                } catch (IOException e) {
                    System.err.println("Error indexing book: " + e.getMessage());
                }
            }
        }
        System.out.println("All books in the directory are already indexed.");
    }


    // Checks if a book has already been indexed by looking up its file path in bookMetadata
    public boolean isBookIndexed(String file) {
        return indexedBookIds.contains(file);
    }

    // Method to search for a word in the inverted index
    public Map<String, List<Integer>> searchWord(String word) {
        return trie.search(word);
    }

    // Preprocesses text by converting to lowercase and splitting into words
    private String[] preprocessText(String text) {
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(word -> !word.isEmpty())
                .toArray(String[]::new);
    }

    // Saves the trie as MessagePack files in the specified output directory
    private void saveTrieAsMessagePack(String outputDirectory) throws IOException {
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        for (Character prefix : trie.getRoot().children.keySet()) {
            File file = new File(dir, prefix + ".msgpack");
            try (FileOutputStream fos = new FileOutputStream(file);
                 MessagePacker packer = MessagePack.newDefaultPacker(fos)) {
                packer.packMapHeader(1);
                packer.packString(String.valueOf(prefix));
                trie.getRoot().children.get(prefix).toMessagePack(packer);
            }
        }
    }

    // Main method to run the indexing and searching for tests
    public static void main(String[] args) {
        try {
            TrieInvertedIndex invertedIndex = new TrieInvertedIndex();
            String directory = "gutenberg_books";
            invertedIndex.indexBooks(directory);
            System.out.println("Indexing completed.");

            String searchWord = "alice";
            Map<String, List<Integer>> results = invertedIndex.searchWord(searchWord);
            if (results != null && !results.isEmpty()) {
                System.out.println("Search results for '" + searchWord + "':");
                for (Map.Entry<String, List<Integer>> entry : results.entrySet()) {
                    System.out.println("Book ID: " + entry.getKey() + ", Positions: " + entry.getValue());
                }
            } else {
                System.out.println("No results found for '" + searchWord + "'.");
            }
        } catch (IOException e) {
            System.err.println("Error during indexing: " + e.getMessage());
        }
    }
}
