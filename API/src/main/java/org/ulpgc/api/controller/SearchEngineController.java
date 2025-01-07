package org.ulpgc.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.ulpgc.query_engine.HazelQueryEngine;
import org.ulpgc.query_engine.MultipleWordsResponseList;
import org.ulpgc.query_engine.TextFragment;

import java.util.Arrays;

@RestController
public class SearchEngineController implements SearchEngineControllerInterface {

    private final HazelQueryEngine searchEngine;

    public SearchEngineController() {
        this.searchEngine = new HazelQueryEngine();
        this.searchEngine.loadData("datamart2");
        System.out.println("Loaded data into the memory");
    }

    @Override
    @GetMapping("/text")
    public TextFragment getTextFragment(
            @RequestParam Integer textId,
            @RequestParam Integer wordPos) {
        return searchEngine.getPartOfBookWithWord(textId, wordPos);
    }

    @GetMapping("/documents/{word}")
    public MultipleWordsResponseList getDocumentsWords(
            @PathVariable String word,
            @RequestParam(required=false) String from,
            @RequestParam(required=false) String to) {
        String[] words = word.split("\\+");
        System.out.println(Arrays.toString(words));
        return searchEngine.searchForMultiplewithCriteria(words, null, null, from, to, null);
    }
}
