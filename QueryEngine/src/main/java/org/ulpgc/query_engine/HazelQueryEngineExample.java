package org.ulpgc.query_engine;

import java.util.List;

public class HazelQueryEngineExample {

    public static void main(String[] args) {
        // Initialize HazelQueryEngine
        HazelQueryEngine queryEngine = new HazelQueryEngine();

        // Specify the directory containing datamart files
        String datamartDirectory = "datamart2";

        // Load data into Hazelcast
        /* todo: temporary solution only first computer should do this,
             make only first computer in the network load data
             */
        System.out.println("Loading data from " + datamartDirectory + " into Hazelcast...");
        queryEngine.loadData(datamartDirectory);
        System.out.println("Data loading complete.");

        // Define search criteria
        String[] searchWords = {"winter"}; // Replace with actual words present in your datamart
        String indexer = ""; // Currently unused
        String title = null; // Placeholder
        String author = null; // Placeholder
        String date = null; // Placeholder
        String language = null; // Placeholder

        // Perform a search
        System.out.println("Performing search...");
        MultipleWordsResponseList searchResults = queryEngine.searchForMultiplewithCriteria(indexer, searchWords, title, author, date, language);

        // Display the search results
        System.out.println("Search results:");
        searchResults.getResults().forEach(result -> {
            System.out.println("Book ID: " + result.getBookId());
            System.out.println("Word: " + result.getWord());
            System.out.println("Positions: " + result.getPositions());
        });

        // Example: Get part of a book containing a specific word
        Integer bookId = 1; // Replace with a valid book ID
        Integer wordId = 42; // Replace with a valid word ID
        TextFragment fragment = queryEngine.getPartOfBookWithWord(bookId, wordId);
        System.out.println("Text fragment: " + fragment.getText());
    }
}
