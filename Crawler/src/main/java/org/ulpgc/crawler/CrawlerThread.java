package org.ulpgc.crawler;

import java.io.*;
import java.util.concurrent.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlerThread implements ICrawler {
    private final String baseUrl = "https://www.gutenberg.org";
    private final String outputDir = "gutenberg_books";
    private final ExecutorService executor; // Pool de hilos

    public CrawlerThread() {
        this.executor = Executors.newFixedThreadPool(10); // Crea un pool con 10 hilos
        createOutputDirectory(outputDir);
    }

    private void createOutputDirectory(String dir) {
        File directory = new File(dir);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    private void downloadBookContent(String downloadLink, String bookFileName) {
        try {
            Document bookContent = Jsoup.connect(downloadLink).get();
            String textContent = bookContent.body().wholeText();

            Pattern pattern = Pattern.compile("\\*\\*\\*.*?\\*\\*\\*");
            Matcher matcher = pattern.matcher(textContent);
            if (matcher.find()) {
                textContent = textContent.substring(matcher.end()).trim();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(bookFileName, StandardCharsets.UTF_8))) {
                writer.write(textContent);
            }
        } catch (IOException e) {
            System.err.println("Error downloading book content from " + downloadLink + ": " + e.getMessage());
        }
    }

    private void saveMetadata(String id, String title, String author, String releaseDate, String language) {
        synchronized (this) { // Sincronización para evitar conflictos al escribir en el archivo
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("gutenberg_data.txt", true))) {
                writer.write("ID: " + id + ", Title: " + title + ", Author: " + author +
                        ", Release Date: " + releaseDate + ", Language: " + language + "\n");
            } catch (IOException e) {
                System.err.println("Error saving metadata for book " + id + ": " + e.getMessage());
            }
        }
    }

    private void downloadBook(String bookLink) {
        try {
            Document bookPage = Jsoup.connect(baseUrl + bookLink).get();
            String fullTitle = bookPage.selectFirst("h1").text();

            String title = extractMetadata(fullTitle, "Title");
            String author = extractMetadata(fullTitle, "Author");

            Element downloadElement = bookPage.selectFirst("a:contains(Plain Text UTF-8)");

            String releaseDate = bookPage.select("td[itemprop=datePublished]").text().trim();
            if (releaseDate.isEmpty()) {
                releaseDate = "Unknown";
            }

            String language = bookPage.select("td").stream()
                    .filter(td -> td.text().matches("English|Spanish|French|German|Italian|Dutch|Chinese|Russian|Japanese"))
                    .findFirst()
                    .map(Element::text)
                    .orElse("Unknown");

            if (downloadElement != null) {
                String downloadLink = downloadElement.attr("href");
                if (!downloadLink.startsWith("http")) {
                    downloadLink = baseUrl + downloadLink;
                }

                String id = bookLink.split("/")[2];
                String bookFileName = outputDir + "/" + id + ".txt";

                downloadBookContent(downloadLink, bookFileName);

                saveMetadata(id, title, author, releaseDate, language);

                System.out.println("Downloaded and saved book: " + title);
            } else {
                System.err.println("Plain Text UTF-8 format not available for " + bookLink);
            }
        } catch (IOException e) {
            System.err.println("Failed to download book at " + bookLink + ": " + e.getMessage());
        }
    }

    private String extractMetadata(String text, String label) {
        String patternString;
        if (label.equals("Title")) {
            patternString = "(.*)\\s+by\\s+(.*)";
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } else if (label.equals("Author")) {
            patternString = "(.*)\\s+by\\s+(.*)";
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(2).trim();
            }
        }
        return "Unknown";
    }

    @Override
    public void fetchBooks(int n) {
        try {
            Document searchPage = Jsoup.connect(baseUrl + "/ebooks/search/?sort_order=downloads").get();
            Elements bookLinks = searchPage.select("li.booklink a");

            int count = 0;
            for (Element link : bookLinks) {
                if (count >= n) break;

                // Ejecutar cada descarga en un hilo del pool
                String bookLink = link.attr("href");
                executor.submit(() -> downloadBook(bookLink));

                count++;
            }
        } catch (IOException e) {
            System.err.println("Error fetching book list: " + e.getMessage());
        } finally {
            shutdownExecutor();
        }
    }

    private void shutdownExecutor() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
