package org.ulpgc.query_engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MultipleWordsResponseList {
    private final List<SearchResultsMW> results = new ArrayList<>();

    public List<SearchResultsMW> getResults() {
        return results;
    }

    public void addResult(Integer bookId, Map<String, List<Integer>> wordPositionsMap) {
        results.add(new SearchResultsMW(bookId, wordPositionsMap));
    }
}