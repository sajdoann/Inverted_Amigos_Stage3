package org.ulpgc.inverted_index.apps;

import org.ulpgc.inverted_index.implementations.GutenbergTokenizer;
import org.ulpgc.inverted_index.implementations.PlainTextDatamartWriter;
import org.ulpgc.inverted_index.ports.InvertedIndex;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class FilePerWordInvertedIndex implements InvertedIndex {
    private final File books;
    private final String datamart;
    private final Set<String> indexed;
    private final File indexedFile;
    private final GutenbergTokenizer tokenizer;

    public FilePerWordInvertedIndex(String books, String datamart, String indexed, GutenbergTokenizer tokenizer) {
        this.books = new File(books);
        this.datamart = datamart;
        this.indexed = this.getIndexed(new File(indexed));
        this.tokenizer = tokenizer;
        this.indexedFile = new File(indexed);
    }

    private Set<String> getIndexed(File indexed){
        try (BufferedReader br = new BufferedReader(new FileReader(indexed))) {
            String linea = br.readLine();
            if (linea != null){
                return Arrays.stream(linea.split(","))
                        .collect(Collectors.toSet());
            }
            else {return new HashSet<>();}
        } catch (IOException e) {
            System.out.println("File not found");
        }
        return new HashSet<>();
    }

    public List<String> listBooks(){
        List<String> books_path = new ArrayList<>();
        File directory = this.books;
        if (directory.isDirectory()){
            File[] files = directory.listFiles();
            assert files != null;
            for (File file: files){
                if (file.isFile()){
                    System.out.println(file);
                    books_path.add(file.getPath().replaceAll("\\\\", "/"));
                }
            }
        }
        return books_path;
    }

    public int isIndexed(String file){
        String id = new File(file).getName().replaceAll("\\D", "");
        if (!id.isEmpty()){
            if (this.indexed.contains(id)){return 0;}
            return Integer.parseInt(id);
        }
        return 1;
    }

    public void indexAll() {
        List<String> books = this.listBooks();
        for (String book: books){
            this.index(book);
        }
    }

    @Override
    public void index(String file) {
        int id = isIndexed(file);
        switch (id) {
            case -1:
                System.out.println("This is not a book");
                break;
            case 0:
                System.out.println("Book Already indexed");
                break;
            default:
                System.out.println("Indexing book " + id);
                Map<String, ResponseList> index = this.tokenizer.tokenize(file, id);
                updateDatamart(index);

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.indexedFile, true))) { // 'true' para agregar al final
                    writer.write(id + ",");
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }


    private void updateDatamart(Map<String, ResponseList> index) {
        PlainTextDatamartWriter writer = new PlainTextDatamartWriter(this.datamart);
        writer.write(index);
    }

    public static void main(String[] args) {
        String books_path = "gutenberg_books";
        String datamart_path = "datamart2";
        String datamart = String.format("%s/%s.txt", datamart_path, "%s");
        String books_indexed = "InvertedIndex/indexed_docs2.txt";
        String stopwords = "InvertedIndex/stopwords.txt";

        // create datamart2 if not already exists
        File directory = new File(datamart_path);
        if (!directory.exists()) {
            if (directory.mkdir()) {
                System.out.println("Directory 'datamart2' created successfully.");
            } else {
                System.out.println("Failed to create directory 'datamart2'.");
            }
        }

        GutenbergTokenizer gutenbergTokenizer = new GutenbergTokenizer(stopwords);
        FilePerWordInvertedIndex filePerWordInvertedIndex = new FilePerWordInvertedIndex(books_path, datamart, books_indexed, gutenbergTokenizer);
        //filePerWordInvertedIndex.index("gutenberg_books/84.txt");
        filePerWordInvertedIndex.indexAll();
    }
}
