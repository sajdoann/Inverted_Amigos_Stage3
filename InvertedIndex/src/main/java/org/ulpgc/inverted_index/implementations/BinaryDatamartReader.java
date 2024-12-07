package org.ulpgc.inverted_index.implementations;

import org.ulpgc.inverted_index.apps.ResponseList;
import org.ulpgc.inverted_index.ports.DatamartReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

public class BinaryDatamartReader implements DatamartReader {

    private final String file;

    public BinaryDatamartReader(String file) {
        this.file = file;
    }

    @Override
    public Map<String, ResponseList> read() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(this.file))) {
            return (Map<String, ResponseList>) ois.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists() {
        return new File(this.file).exists();
    }
}
