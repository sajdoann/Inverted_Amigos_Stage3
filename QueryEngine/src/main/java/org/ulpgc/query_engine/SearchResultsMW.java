package org.ulpgc.query_engine;

import java.util.List;
import java.util.Map;

public class SearchResultsMW {
    private final int bookId;
    private final Map<String, List<Integer>> words_positions;

    public SearchResultsMW(int bookId, Map<String, List<Integer>> words_positions) {
        this.bookId = bookId;
        this.words_positions = words_positions;
    }

    public int getBookId() {
        return bookId;
    }

    public Map<String, List<Integer>> getPositions() {
        return words_positions;
    }
}