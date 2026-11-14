package com.mts.aadati.engine.app.generate.triggers;

import com.mts.aadati.engine.app.generate.triggers.bridges.CompletionChanged;
import com.mts.aadati.engine.app.generate.triggers.bridges.HabitCreated;
import com.mts.aadati.engine.app.generate.triggers.bridges.HabitPointChanged;
import com.mts.aadati.engine.app.generate.triggers.bridges.HabitTaskCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDate;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProvisioningTrigger {
    private final CompletionProvisioner provisioner;
    private final PercentageCalculationService calculation;
    private final Clock clock;


    @Async("engineEventExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
    public void on(HabitCreated e) {
        safely("HabitCreated habitId=" + e.habitId(), () ->
                provisioner.ensureForHabit(e.habitId(), LocalDate.now(clock))
        );
    }

    @Async("engineEventExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
    public void on(HabitTaskCreated e) {
        safely("HabitTaskCreated habitTaskId=" + e.habitTaskId(), () ->
                provisioner.ensureForTask(e.habitTaskId(), LocalDate.now(clock))
        );
    }

    @Async("engineEventExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
    public void on(CompletionChanged e) {
        safely("CompletionChanged userId=" + e.userId() + " date=" + e.date(), () -> {
            calculation.recalculateDay(e.userId(), e.date());
            calculation.recalculateWeek(e.userId(), e.date());
        });
    }

    @Async("engineEventExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
    public void on(HabitPointChanged e) {
        safely("HabitPointChanged " + e, () ->
                provisioner.onHabitPointChanged(e)
        );
    }

    private void safely(String what, Runnable r) {
        try {
            r.run();
        } catch (Exception ex) {
            log.error("Provisioning trigger failed: {}", what, ex);
        }
    }
}