package org.ulpgc.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.ulpgc.query_engine.MultipleWordsResponseList;
import org.ulpgc.query_engine.TextFragment;

@RestController
public interface SearchEngineControllerInterface {
    @GetMapping("/text")
    TextFragment getTextFragment(
            @RequestParam Integer textId,
            @RequestParam Integer wordPos);
}
