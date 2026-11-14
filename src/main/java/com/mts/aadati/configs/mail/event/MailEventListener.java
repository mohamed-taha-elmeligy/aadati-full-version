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
    public void onMailEvent(MailEvent event) {
        try {
            switch (event) {
                case MailSimpleEvent e -> mailService.sendSimpleMail(e.to(), e.subject(), e.body());
                case MailHtmlEvent e -> mailService.sendHtmlMail(e.to(), e.subject(), e.html());
            }
        } catch (Exception ex) {
            log.error("Failed to send mail: {}", event.getClass().getSimpleName(), ex);
        }
    }
}