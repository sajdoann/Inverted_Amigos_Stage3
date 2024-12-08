package org.ulpgc.inverted_index.implementations;

import org.ulpgc.inverted_index.ports.DatamartWriter;

public class PlainTextDatamartWriterFactory extends Factory {
    private final String datamart;

    public PlainTextDatamartWriterFactory(String datamart) {
        this.datamart = datamart;
    }

    @Override
    public DatamartWriter createDatamartWriter() {
        return new PlainTextDatamartWriter(this.datamart);
    }
}
