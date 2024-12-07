package org.ulpgc.query_engine;

public class TextFragment {
    private String line;
    private int position;

    public TextFragment(String line, int position) {
        this.line = line;
        this.position = position;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
