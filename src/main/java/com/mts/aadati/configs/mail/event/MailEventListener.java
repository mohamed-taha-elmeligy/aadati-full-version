package com.mts.aadati.configs.mail.event;

import com.mts.aadati.configs.mail.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class MailEventListener {

    private final MailService mailService;

    public MailEventListener(MailService mailService) {
        this.mailService = mailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMailSimpleEvent(MailSimpleEvent event) {
        try {
            mailService.sendSimpleMail(event.to(), event.subject(), event.body());
        } catch (Exception e) {
            log.error("Failed to send simple mail", e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMailHtmlEvent(MailHtmlEvent event) {
        try {
            mailService.sendHtmlMail(event.to(), event.subject(), event.html());
        } catch (Exception e) {
            log.error("Failed to send HTML mail", e);
        }
    }
}