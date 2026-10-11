package com.mts.aadati.engine.core.generate;

public enum Cron {

    EVERY_MINUTE("0 * * * * *"),
    HOURLY("0 0 * * * *"),
    DAILY("0 0 0 * * *"),
    WEEKLY("0 0 0 * * MON"),
    MONTHLY("0 0 0 1 * *"),
    YEARLY("0 0 0 1 1 *");

    private final String expression;

    Cron(String expression) {
        this.expression = expression;
    }

    public String expression() {
        return expression;
    }
}
