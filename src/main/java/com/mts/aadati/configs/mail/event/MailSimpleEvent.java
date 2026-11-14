package com.mts.aadati.configs.mail.event;

import org.springframework.lang.NonNull;

public record MailSimpleEvent(
        String to,
        String subject,
        String body
) implements MailEvent {
    @Override
    @NonNull
    public String toString() {
        return "MailSimpleEvent{}";
    }
}