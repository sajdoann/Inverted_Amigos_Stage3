package org.ulpgc.inverted_index.implementations;

import org.ulpgc.inverted_index.apps.ResponseList;
import org.ulpgc.inverted_index.ports.DatamartWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlainTextDatamartWriter implements DatamartWriter {

    private final String datamart;

    public PlainTextDatamartWriter(String datamart) {
        this.datamart = datamart;
    }

    @Override
    public void write(Map<String, ResponseList> index) {
        index.forEach((word, responseList) -> {
            List<Map.Entry<Integer, List<Integer>>> results = responseList.getResults();

            for (Map.Entry<Integer, List<Integer>> entry : results) {
                Integer key = entry.getKey();
                String value = entry.getValue().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));

                this.writeOnFile(word, String.valueOf(key), value);
            }
        });

    }
    private void writeOnFile(String word, String bookID, String appearances){
        if (word.equals("aux")){word="aux_";}
        File file = new File(String.format(this.datamart, word));
        if (!file.exists()){
            try {
                boolean newFile = file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(bookID + ":" + appearances + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
