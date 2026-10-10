package com.mts.aadati.engine.app.calculate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompletionListener {

    private final PercentageCalculationService calculation;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CompletionChanged e) {
        try {
            calculation.recalculateDay(e.userId(), e.date());
            calculation.recalculateWeek(e.userId(), e.date());
        } catch (Exception ex) {
            log.error("Recalculation failed for user {} on {}", e.userId(), e.date(), ex);
        }
    }
}
