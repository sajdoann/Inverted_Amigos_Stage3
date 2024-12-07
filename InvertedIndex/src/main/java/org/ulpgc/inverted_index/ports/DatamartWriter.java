package org.ulpgc.inverted_index.ports;

import org.ulpgc.inverted_index.apps.ResponseList;

import java.util.Map;

public interface DatamartWriter {
    void write(Map<String, ResponseList> index);
}
