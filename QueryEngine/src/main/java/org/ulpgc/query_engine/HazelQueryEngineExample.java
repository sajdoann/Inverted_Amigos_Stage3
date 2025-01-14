package org.ulpgc.query_engine;

import com.hazelcast.map.IMap;
import java.util.Scanner;

public class HazelQueryEngineExample {

    public static void main(String[] args) {
        HazelQueryEngine queryEngine = new HazelQueryEngine(args);

        queryEngine.maps_size();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Interactive Query Engine. Write exit for exiting the program");

        while (true) {
            System.out.print("Search for words separated by comas");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting...");
                break;
            }

            String[] searchWords = input.split(",");

            System.out.println("Searching words: " + String.join(", ", searchWords));
            MultipleWordsResponseList results = queryEngine.searchForMultiplewithCriteria(searchWords, null, null, null, null, null);

            if (results.getResults().isEmpty()) {
                System.out.println("No results found for the specified words");
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
