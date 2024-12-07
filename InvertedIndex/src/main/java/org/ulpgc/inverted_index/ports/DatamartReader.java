package org.ulpgc.inverted_index.ports;

import org.ulpgc.inverted_index.apps.ResponseList;

import java.util.Map;

public interface DatamartReader {
    Map<String, ResponseList> read();
    boolean exists();
}
