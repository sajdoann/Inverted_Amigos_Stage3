package org.ulpgc.query_engine;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class MainTextBenchmark {
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(GetTextFragmentBenchmark.class.getSimpleName())
                .forks(1)
                .output("search_engine_text_benchmark.txt")
                .build();
        new Runner(opt).run();
    }
}
