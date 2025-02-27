package org.ulpgc.query_engine;

public interface SearchEngineInterface {
    enum Field {
        ID("ID"),
        TITLE("Title"),
        AUTHOR("Author"),
        FROM("Date"),
        TO("Date"),
        LANGUAGE("Language");

        private final String value;

        Field(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public MultipleWordsResponseList searchForMultiplewithCriteria(String[] words, String title, String author, String from, String to, String language);
    public TextFragment getPartOfBookWithWord(Integer bookId, Integer wordId);
}