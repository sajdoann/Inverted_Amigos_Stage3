package org.ulpgc.inverted_index.apps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResponseList implements Serializable {
    private List<Map.Entry<Integer, List<Integer>>> results = new ArrayList<>();

    public ResponseList() {}

    public ResponseList(List<Map.Entry<Integer, List<Integer>>> results) {
        this.results = results;
    }

    public List<Map.Entry<Integer, List<Integer>>> getResults() {
        return results;
    }

    public void setResults(List<Map.Entry<Integer, List<Integer>>> results) {
        this.results = results;
    }

    public void addResult(Map.Entry<Integer, List<Integer>> result) {
        this.results.add(result);
    }

    public List<Integer> getPositions() {
        List<Integer> positions = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : results) {
            positions.addAll(entry.getValue());
        }
        return positions;
    }
}
