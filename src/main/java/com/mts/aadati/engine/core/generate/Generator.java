package com.mts.aadati.engine.core.generate;

import com.mts.aadati.engine.core.job.Job;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface  Generator<T> extends Job {
    Optional<LocalDate> before();
    List<T> generate(LocalDate start);
    void after(List<T> generated);

    @Override
    default void execute() {
        before().ifPresent(start -> after(generate(start)));
    }
}