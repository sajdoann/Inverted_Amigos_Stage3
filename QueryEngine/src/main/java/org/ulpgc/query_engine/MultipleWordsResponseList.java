package org.ulpgc.query_engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MultipleWordsResponseList implements Serializable {
    private List<Map.Entry<Integer, List<List<Integer>>>> results = new ArrayList<>();

    public MultipleWordsResponseList() {}

    public MultipleWordsResponseList(List<Map.Entry<Integer, List<List<Integer>>>> results) {
        this.results = results;
    }

    public List<Map.Entry<Integer, List<List<Integer>>>> getResults() {
        return results;
    }

    public void setResults(List<Map.Entry<Integer, List<List<Integer>>>> results) {
        this.results = results;
    }

    public void addResult(Map.Entry<Integer, List<List<Integer>>> result) {
        this.results.add(result);
    }
}
