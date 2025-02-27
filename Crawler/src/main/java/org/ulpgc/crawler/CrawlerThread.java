package org.ulpgc.crawler;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

import com.hazelcast.map.IMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlerThread implements ICrawler {
    private final String baseUrl = "https://www.gutenberg.org";
    private ExecutorService executor;

    public CrawlerThread() {
        this.executor = create_Executors();
        createOutputDirectory();
    }

    private void createOutputDirectory() {
        File directory = new File("gutenberg_books");
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public void deleteDirectoryContents(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryContents(file);
                }
                file.delete();
            }
        }
    }

    private ExecutorService create_Executors(){
        return Executors.newFixedThreadPool(10);
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
        synchronized (this) {
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
            String fullTitle = Objects.requireNonNull(bookPage.selectFirst("h1")).text();

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
                String outputDir = "gutenberg_books";
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

    public void loadMetadataFromFile(File metadataFile, IMap metadata) {
        Pattern pattern = Pattern.compile("(\\w+)\\s*:\\s*((?:\\w+\\s+\\d{1,2},\\s+\\d{4})|[^,]+)");
        try (BufferedReader reader = new BufferedReader(new FileReader(metadataFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                Map<String, String> bookData = new HashMap<>();
                Matcher matcher = pattern.matcher(line);
                int bookId = -1;

                while (matcher.find()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    if (key.equals("ID")) bookId = Integer.parseInt(value);
                    else bookData.put(key, value);
                }

                metadata.put(bookId, bookData);
            }
        } catch (IOException e) {
            System.err.println("Error reading metadata file: " + e.getMessage());
        }
    }

    @Override
    public void fetchBooks(int page) {
        createOutputDirectory();
        this.executor = create_Executors();
        String nextPage = baseUrl + "/ebooks/search/?sort_order=title&start_index=" + page;
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        try {
            System.out.println("Fetching from: " + nextPage);
            Document searchPage = Jsoup.connect(nextPage).timeout(10000).get();

            Elements bookLinks = searchPage.select("li.booklink a[href^='/ebooks/']");
            System.out.println("Books found on page: " + bookLinks.size());

            if (bookLinks.isEmpty()) {
                System.err.println("No books found on page, stopping...");
            }

            for (Element link : bookLinks) {
                String bookLink = link.attr("href");
                completionService.submit(() -> {
                    downloadBook(bookLink);
                    return null;
                });
            }

            for (int i = 0; i < bookLinks.size(); i++) {
                try {
                    completionService.take();
                } catch (InterruptedException e) {
                    System.err.println("Task interrupted while waiting: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            System.err.println("Error fetching book list: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

    }

    public void shutdownExecutor() {
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

    public static void main(String[] args) {
        CrawlerThread crawlerThread = new CrawlerThread();
        crawlerThread.fetchBooks(25);
        crawlerThread.shutdownExecutor();
    }
}