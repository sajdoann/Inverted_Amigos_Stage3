package org.ulpgc.query_engine;

import java.util.Scanner;

public class HazelQueryEngineExample {

    public static void main(String[] args) {
        HazelQueryEngine queryEngine = new HazelQueryEngine();

        String datamartDirectory = "datamart2";

        System.out.println("Loading data from " + datamartDirectory + " into Hazelcast...");
        queryEngine.loadData(datamartDirectory);
        System.out.println("Data loading complete.");

        Scanner scanner = new Scanner(System.in);

        System.out.println("Bienvenido al motor de búsqueda interactivo. Escribe 'salir' para terminar.");

        while (true) {
            System.out.print("Introduce palabras para buscar (separadas por comas): ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("salir")) {
                System.out.println("Saliendo...");
                break;
            }

            String[] searchWords = input.split(",");

            System.out.println("Buscando palabras: " + String.join(", ", searchWords));
            MultipleWordsResponseList results = queryEngine.searchForMultiplewithCriteria("", searchWords, null, null, null, null);

            if (results.getResults().isEmpty()) {
                System.out.println("No se encontraron resultados para las palabras especificadas.");
            } else {
                results.getResults().forEach(result -> {
                    System.out.println("Book ID: " + result.getBookId());
                    System.out.println("Word: " + result.getWord());
                    System.out.println("Positions: " + result.getPositions());
                });
            }

            System.out.println(); // Línea en blanco para separar búsquedas
        }
        scanner.close();
    }
}
