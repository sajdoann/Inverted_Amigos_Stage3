package org.ulpgc.query_engine;

public class MainQueryEngine {
    public static void main(String[] args) {
        System.out.println("This is the Query Engine");
        SearchEngine searchEngine = new SearchEngine();
        String[] words = new String[]{"winter"};
        MultipleWordsResponseList response = searchEngine.searchForBooksWithMultipleWords(words, "hashed");
        System.out.println(response.getResults());
    }
}