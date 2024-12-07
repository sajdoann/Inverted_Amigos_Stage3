package org.ulpgc.query_engine;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class GetTextFragmentBenchmark {
    @Param({"1000", "2000", "3000", "4000", "5000"})
    private int word_id;
    private int text_id;

    private SearchEngine searchEngine;

    @Setup(Level.Trial)
    public void setup() {
        text_id = 100;
        searchEngine = new SearchEngine();
    }

    @Benchmark
    public void testGetPartOfBookWithWord() {
        searchEngine.getPartOfBookWithWord(text_id, word_id);
    }
}
