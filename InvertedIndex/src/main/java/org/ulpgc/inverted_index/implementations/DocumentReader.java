package org.ulpgc.inverted_index.implementations;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocumentReader {
    public static List<String> readDocumentsFromDirectory(String directory, Map<String, String> bookMetadata) throws IOException {
        List<String> documents = new ArrayList<>();
        File dir = new File(directory);

        // Filter for .txt files in the specified directory
        for (File file : dir.listFiles((d, name) -> name.endsWith(".txt"))) {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(" ");
                }
            }
            documents.add(content.toString());
            String bookId = file.getName().split("_")[0];
            bookMetadata.put(content.toString(), bookId);
        }
        return documents;
    }
}
