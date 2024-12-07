package org.ulpgc.inverted_index.implementations;

import org.ulpgc.inverted_index.apps.ResponseList;
import org.ulpgc.inverted_index.ports.DatamartWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class BinaryDatamartWriter implements DatamartWriter {

    private final String file;

    public BinaryDatamartWriter(String file) {
        this.file = file;
    }

    @Override
    public void write(Map<String, ResponseList> index) {
        File archivo = new File(this.file);
        if (!archivo.exists()) {
            try {
                Files.createFile(Paths.get(this.file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(archivo))) {
            oos.writeObject(index);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
