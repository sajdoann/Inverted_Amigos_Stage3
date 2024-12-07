package org.ulpgc.inverted_index.ports;

import org.ulpgc.inverted_index.apps.ResponseList;

import java.util.Map;

public interface Tokenizer {
    public Map<String, ResponseList> tokenize(String book, int bookID);
}