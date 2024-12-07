package org.ulpgc.inverted_index.implementations;

import org.ulpgc.inverted_index.apps.ResponseList;
import org.ulpgc.inverted_index.ports.DatamartReader;
import org.ulpgc.inverted_index.ports.DatamartWriter;

import java.util.Map;

public class BinaryFileUpdaterWorkers extends Thread implements UpdaterWorkers{

    private final DatamartReader reader;
    private final DatamartWriter writer;
    private final Map<String, ResponseList> index;

    public BinaryFileUpdaterWorkers(DatamartReader reader, DatamartWriter writer, Map<String, ResponseList> index) {
        this.reader = reader;
        this.writer = writer;
        this.index = index;
    }

    @Override
    public void run() {
        if (!reader.exists()){
            writer.write(this.index);
        }
        else{
            Map<String, ResponseList> savedIndex = reader.read();
            Map<String, ResponseList> updatedIndex = updateIndex(savedIndex, this.index);
            writer.write(updatedIndex);
        }
    }

    private Map<String, ResponseList> updateIndex(Map<String, ResponseList> savedIndex, Map<String, ResponseList> index) {
        index.forEach((word, newResponseList) -> {
            savedIndex.merge(word, newResponseList, (savedResponseList, newIndex) -> {
                newIndex.getResults().forEach(savedResponseList::addResult);
                return savedResponseList;
            });
        });

        return savedIndex;
    }
}
