package com.mts.aadati.configs.mail.event;

import org.springframework.lang.NonNull;

public record MailHtmlEvent(
        String to,
        String subject,
        String html
) implements MailEvent{
    @Override
    @NonNull
    public String toString() {
        return "MailHtmlEvent{}";
    }
}