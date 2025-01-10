package org.ulpgc.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
//import org.ulpgc.crawler.MainCrawler;
//import org.ulpgc.inverted_index.apps.FilePerWordInvertedIndexHazelcast;
//import org.ulpgc.inverted_index.implementations.GutenbergTokenizer;
import org.ulpgc.query_engine.HazelQueryEngine;
import org.ulpgc.query_engine.MultipleWordsResponseList;
import org.ulpgc.query_engine.TextFragment;

import java.util.Arrays;

@RestController
public class SearchEngineController implements SearchEngineControllerInterface {

    private final HazelQueryEngine searchEngine;

    public SearchEngineController() throws InterruptedException {
        this.searchEngine = new HazelQueryEngine();

        //String[] args2 = new String[] {"aa", "bb"};
        //MainCrawler.main(args2);
        //System.out.println("Crawled some books");

        //String booksDirectory = "gutenberg_books";

        //GutenbergTokenizer tokenizer = new GutenbergTokenizer("InvertedIndex/stopwords.txt");

        //FilePerWordInvertedIndexHazelcast indexer = new FilePerWordInvertedIndexHazelcast(booksDirectory, tokenizer);

        //indexer.indexAll();
        //System.out.println("Loaded data into the memory");
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
            @RequestParam(required = false) String author,
            @RequestParam(required=false) String from,
            @RequestParam(required=false) String to,
            @RequestParam(required=false) String language,
            @RequestParam(required=false) String title) {
        String[] words = word.split("\\+");
        System.out.println(Arrays.toString(words));
        return searchEngine.searchForMultiplewithCriteria(words, title, author, from, to, language);
    }
}