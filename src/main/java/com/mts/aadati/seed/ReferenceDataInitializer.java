package com.mts.aadati.seed;

import com.mts.aadati.seed.seeders.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReferenceDataInitializer implements ApplicationRunner {

    private final List<Seeder> seeders;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Running reference-data initialization ({} seeders)...", seeders.size());

        for (Seeder seeder : seeders.stream().sorted(Comparator.comparingInt(Seeder::order)).toList()) {
            try {
                seeder.seed();
            } catch (RuntimeException e) {
                log.error("Seeder {} failed", seeder.getClass().getSimpleName(), e);
                throw e;
            }
        }

        log.info("Reference-data initialization complete.");
    }
}
