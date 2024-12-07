package org.ulpgc.query_engine;

import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class MainBenchmark {
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SearchEngineBenchmark.class.getSimpleName())
                .forks(1)
                .output("search_engine_benchmark.txt")
                .build();
        new Runner(opt).run();
    }
}
