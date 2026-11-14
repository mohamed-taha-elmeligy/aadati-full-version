package com.mts.aadati.configs.mail.event;

public sealed interface MailEvent permits MailSimpleEvent, MailHtmlEvent {
    String to();
    String subject();
}