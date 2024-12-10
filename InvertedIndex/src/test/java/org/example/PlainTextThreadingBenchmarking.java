package org.example;

import org.openjdk.jmh.annotations.*;
import org.ulpgc.inverted_index.apps.FilePerWordInvertedIndex;
import org.ulpgc.inverted_index.implementations.Factory;
import org.ulpgc.inverted_index.implementations.GutenbergTokenizer;
import org.ulpgc.inverted_index.implementations.PlainTextDatamartWriterFactory;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)

@State(Scope.Benchmark)
public class PlainTextThreadingBenchmarking {

    @State(Scope.Thread)
    public static class Operands{
        @Param({"1", "2", "4", "8", "16"}) // Define los tamaños de matriz que quieres probar
        private int numThreads;

    }

    private final String books_path = "C:/Users/Eduardo/Desktop/InvertedIndex3/Inverted_Amigos_Stage3/gutenberg_books";
    private final String datamart_path = "C:/Users/Eduardo/Desktop/InvertedIndex3/Inverted_Amigos_Stage3/datamart2";
    private final String datamart = String.format("%s/%s.txt", datamart_path, "%s");
    private final String books_indexed = "C:/Users/Eduardo/Desktop/InvertedIndex3/Inverted_Amigos_Stage3/InvertedIndex/indexed_docs2.txt";
    private final String stopwords = "C:/Users/Eduardo/Desktop/InvertedIndex3/Inverted_Amigos_Stage3/InvertedIndex/stopwords.txt";
    private final GutenbergTokenizer gutenbergTokenizer = new GutenbergTokenizer(stopwords);
    private final Factory plainTextDatamartWriterFactory = new PlainTextDatamartWriterFactory(datamart);

    // Este método se ejecuta antes de cada benchmark
    @Setup(Level.Invocation)
    public void setup() throws IOException {
        delete_datamart();
        FileWriter writer = new FileWriter(books_indexed, false);  // false para sobrescribir el archivo
        writer.write("");
        writer.close();
    }

    private static void delete_datamart() throws IOException {
        Path dirPath = Paths.get("C:/Users/Eduardo/Desktop/InvertedIndex3/Inverted_Amigos_Stage3/datamart2");

        if (Files.exists(dirPath)) {
            Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        throw exc;
                    }
                }
            });
        }

        // Crear el directorio de nuevo
        Files.createDirectories(dirPath);
    }

    // Método de benchmark que realiza la operación de multiplicación/indexado
    @Benchmark
    public void multiplication(Operands operands) throws IOException {
        new FilePerWordInvertedIndex(books_path, books_indexed, gutenbergTokenizer, plainTextDatamartWriterFactory, operands.numThreads).indexAll();
    }
}

