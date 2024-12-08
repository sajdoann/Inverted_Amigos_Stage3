package org.ulpgc.inverted_index.implementations;

import org.ulpgc.inverted_index.ports.DatamartWriter;

public abstract class Factory {
    public abstract DatamartWriter createDatamartWriter();
}
