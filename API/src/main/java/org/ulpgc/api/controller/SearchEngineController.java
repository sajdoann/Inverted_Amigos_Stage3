package org.ulpgc.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.ulpgc.query_engine.HazelQueryEngine;
import org.ulpgc.query_engine.MultipleWordsResponseList;
import org.ulpgc.query_engine.TextFragment;

@RestController
public class SearchEngineController implements SearchEngineControllerInterface {

    private final HazelQueryEngine searchEngine;

    public SearchEngineController() {
        this.searchEngine = new HazelQueryEngine();
    }

    @Override
    @GetMapping("/search/{indexer}")
    public MultipleWordsResponseList getSearchResultsMultiple(
            @PathVariable String indexer,
            @RequestParam String word,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String language
    ) {
        String[] words = word.split(" ");
        return searchEngine.searchForMultiplewithCriteria(indexer, words, title, author, date, language);
    }

    @Override
    @GetMapping("/text")
    public TextFragment getTextFragment(
            @RequestParam Integer textId,
            @RequestParam Integer wordPos) {
        return searchEngine.getPartOfBookWithWord(textId, wordPos);
    }
}
