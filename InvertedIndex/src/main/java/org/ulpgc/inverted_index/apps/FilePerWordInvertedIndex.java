package org.ulpgc.inverted_index.apps;

import org.ulpgc.inverted_index.implementations.Factory;
import org.ulpgc.inverted_index.implementations.GutenbergTokenizer;
import org.ulpgc.inverted_index.implementations.PlainTextDatamartWriter;
import org.ulpgc.inverted_index.implementations.PlainTextDatamartWriterFactory;
import org.ulpgc.inverted_index.ports.DatamartWriter;
import org.ulpgc.inverted_index.ports.InvertedIndex;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FilePerWordInvertedIndex implements InvertedIndex {
    private final File books;
    private final Set<String> indexed;
    private final File indexedFile;
    private final GutenbergTokenizer tokenizer;

    private final Factory datamartWriterFactory;

    private final int numberOfThreads;

    public FilePerWordInvertedIndex(String books, String indexed, GutenbergTokenizer tokenizer, Factory datamartWriterFactory, int numberOfThreads) {
        this.books = new File(books);
        this.indexed = this.getIndexed(new File(indexed));
        this.tokenizer = tokenizer;
        this.indexedFile = new File(indexed);
        this.datamartWriterFactory = datamartWriterFactory;
        this.numberOfThreads = numberOfThreads;
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
        int n = 0;
        List<String> books = this.listBooks();
        for (String book: books){
            n++;
            System.out.println("Indexing book " + n + "/1935");
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
                System.out.printf("Book Already %s indexed%n", id);
                break;
            default:
                System.out.println("Indexing book " + id);
                Map<String, ResponseList> index = this.tokenizer.tokenize(file, id);

                ExecutorService executorService = Executors.newFixedThreadPool(this.numberOfThreads);
                List<Map<String, ResponseList>> partitions = partitionMap(index, this.numberOfThreads);

                for (Map<String, ResponseList> partition : partitions) {
                    executorService.submit(() -> updateDatamart(partition, this.datamartWriterFactory.createDatamartWriter()));
                }

                executorService.shutdown();
                try {
                    executorService.awaitTermination(1, TimeUnit.HOURS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.indexedFile, true))) { // 'true' para agregar al final
                    writer.write(id + ",");
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private List<Map<String, ResponseList>> partitionMap(Map<String, ResponseList> map, int numPartitions) {
        List<Map<String, ResponseList>> partitions = new ArrayList<>();
        int partitionSize = (int) Math.ceil((double) map.size() / numPartitions);
        Map<String, ResponseList> currentPartition = new HashMap<>();

        int count = 0;
        for (Map.Entry<String, ResponseList> entry : map.entrySet()) {
            currentPartition.put(entry.getKey(), entry.getValue());
            count++;
            if (count == partitionSize) {
                partitions.add(currentPartition);
                currentPartition = new HashMap<>();
                count = 0;
            }
        }

        if (!currentPartition.isEmpty()) {
            partitions.add(currentPartition);
        }

        return partitions;
    }


    private void updateDatamart(Map<String, ResponseList> index, DatamartWriter datamartWriter) {
        datamartWriter.write(index);
    }

    public static void main(String[] args) {
        String books_path = "gutenberg_books";
        String datamart_path = "datamart2";
        String datamart = String.format("%s/%s.txt", datamart_path, "%s");
        String books_indexed = "InvertedIndex/indexed_docs2.txt";
        String stopwords = "InvertedIndex/stopwords.txt";
        int numberOfThreads = 2;

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
        Factory plainTextDatamartWriterFactory = new PlainTextDatamartWriterFactory(datamart);
        FilePerWordInvertedIndex filePerWordInvertedIndex = new FilePerWordInvertedIndex(books_path, books_indexed, gutenbergTokenizer, plainTextDatamartWriterFactory, numberOfThreads);
        //filePerWordInvertedIndex.index("gutenberg_books/84.txt");
        filePerWordInvertedIndex.indexAll();
    }
}
