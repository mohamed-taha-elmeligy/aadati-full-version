package com.mts.aadati.engine.app.generate.triggers;

import com.mts.aadati.engine.app.generate.triggers.bridges.CompletionChanged;
import com.mts.aadati.engine.app.generate.triggers.bridges.HabitCreated;
import com.mts.aadati.engine.app.generate.triggers.bridges.HabitTaskCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProvisioningTrigger {
    private final CompletionProvisioner provisioner;
    private final PercentageCalculationService calculation;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void on(HabitCreated e) {
        safely(() ->
                provisioner.ensureForHabit(e.habitId(), LocalDate.now())
        );
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void on(HabitTaskCreated e) {
        safely(() ->
                provisioner.ensureForTask(e.habitTaskId(), LocalDate.now())
        );
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void on(CompletionChanged e) {
        safely(()->{
            calculation.recalculateDay(e.userId(), e.date());
            calculation.recalculateWeek(e.userId(), e.date());
        });
    }

    private void safely(Runnable r) {
        try { r.run(); }
        catch (Exception ex) {
            log.error("Provisioning trigger failed", ex);
        }
    }
}
