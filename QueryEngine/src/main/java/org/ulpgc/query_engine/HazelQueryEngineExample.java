package org.ulpgc.query_engine;

import com.hazelcast.map.IMap;
import java.util.Scanner;

public class HazelQueryEngineExample {

    public static void main(String[] args) {
        HazelQueryEngine queryEngine = new HazelQueryEngine();

        queryEngine.maps_size();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the interactive search engine. Type ‘exit’ to finish.");

        while (true) {
            System.out.print("Enter words to search for (separated by commas): ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("Leave")) {
                System.out.println("Leaving...");
                break;
            }

            String[] searchWords = input.split(",");

            System.out.println("Searching for words: " + String.join(", ", searchWords));
            MultipleWordsResponseList results = queryEngine.searchForMultiplewithCriteria(searchWords, null, null, null, null, null);

            if (results.getResults().isEmpty()) {
                System.out.println("No results were found for the specified words.");
            } else {
                results.getResults().forEach(result -> {
                    System.out.println("Book ID: " + result.getBookId());
                    System.out.println("Positions: " + result.getPositions());
                });
            }

            System.out.println();
        }
        scanner.close();
    }
}

