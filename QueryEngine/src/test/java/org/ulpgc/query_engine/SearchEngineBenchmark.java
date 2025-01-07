package org.ulpgc.query_engine;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class SearchEngineBenchmark {
    @Param({"hashed", "trie", "directory"})
    private String indexer;

    private HazelQueryEngine searchEngine;
    private String[] words1, words2;
    private String author, language;

    @Setup(Level.Trial)
    public void setup() {
        searchEngine = new HazelQueryEngine();
        words1 = new String[1];
        words1[0] = "love";
        words2 = new String[2];
        words2[0] = "summer";
        words2[1] = "winter";
        author = "William";
        language = "English";
    }

    @Benchmark
    public void testOneWordSearch() {
        searchEngine.searchForMultiplewithCriteria(indexer, words1, null, null, null, null);
    }

    @Benchmark
    public void testTwoWordSearch() {
        searchEngine.searchForMultiplewithCriteria(indexer, words2, null, null, null, null);
    }

    @Benchmark
    public void testOneWordWithFilter() {
        searchEngine.searchForMultiplewithCriteria(indexer, words1, null, author, null, language);
    }

    @Benchmark
    public void testTwoWordWithFilter() {
        searchEngine.searchForMultiplewithCriteria(indexer, words2, null, author, language, null);
    }
}
