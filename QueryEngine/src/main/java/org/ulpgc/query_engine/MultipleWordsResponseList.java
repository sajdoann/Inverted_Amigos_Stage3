package org.ulpgc.query_engine;

import java.util.ArrayList;
import java.util.List;

public class MultipleWordsResponseList {
    private final List<SearchResult> results = new ArrayList<>();

    public void addResult(int bookId, String word, List<Integer> positions) {
        results.add(new SearchResult(bookId, word, positions));
    }

    public List<SearchResult> getResults() {
        return results;
    }
}