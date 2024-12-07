package org.ulpgc.query_engine;
import java.util.List;

public class SearchResult {
    private final int bookId;
    private final String word;
    private final List<Integer> positions;

    public SearchResult(int bookId, String word, List<Integer> positions) {
        this.bookId = bookId;
        this.word = word;
        this.positions = positions;
    }

    public int getBookId() {
        return bookId;
    }

    public String getWord() {
        return word;
    }

    public List<Integer> getPositions() {
        return positions;
    }
}
