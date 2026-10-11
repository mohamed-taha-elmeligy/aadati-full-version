package com.mts.aadati.engine.core.job;

import com.mts.aadati.engine.core.generate.Cron;

import java.util.List;

public interface Job {
    String id();
    List<String> dependsOn();
    List<Cron> cron();
    void execute();
    default void onError(Exception e, int attempt) {}
    default int maxRetries(){
        return 3 ;
    }
}
