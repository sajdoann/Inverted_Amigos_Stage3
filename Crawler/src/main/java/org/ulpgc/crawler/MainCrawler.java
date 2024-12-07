package org.ulpgc.crawler;

public class MainCrawler {
    public static void main(String[] args) {
        int[] bookCounts = {60};
        double[][] results = new double[bookCounts.length][2];

        for (int i = 0; i < bookCounts.length; i++) {
            int n = bookCounts[i];

            CrawlerThread crawlerThread = new CrawlerThread();
            long startThread = System.nanoTime();
            crawlerThread.fetchBooks(n);
            long endThread = System.nanoTime();
            results[i][0] = (endThread - startThread) / 1_000_000_000.0;

//            CrawlerSerial crawlerSerial = new CrawlerSerial();
            long startSerial = System.nanoTime();
//            crawlerSerial.fetchBooks(n);
            long endSerial = System.nanoTime();
            results[i][1] = (endSerial - startSerial) / 1_000_000_000.0;
        }

        System.out.println("======================================");
        System.out.println("| Libros | CrawlerThread | CrawlerSerial |");
        System.out.println("======================================");
        for (int i = 0; i < bookCounts.length; i++) {
            System.out.printf("|   %3d  |     %.2f s    |     %.2f s     |%n",
                    bookCounts[i], results[i][0], results[i][1]);
        }
        System.out.println("======================================");
    }
}
