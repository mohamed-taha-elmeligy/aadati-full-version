package com.mts.aadati.configs.caching;

import lombok.Getter;

import java.time.Duration;

@Getter
public enum CacheTtl {
    DAY             (CacheNames.HABIT_DAY_WEEK,              Ttl.REFERENCE, Ttl.REFERENCE, Ttl.STABLE),
    WEEK            (CacheNames.HABIT_WEEK,             Ttl.REFERENCE, Ttl.REFERENCE, Ttl.STABLE),
    ROLE            (CacheNames.ROLE,             Ttl.REFERENCE, Ttl.REFERENCE, Ttl.STABLE),
    PRIORITY_LEVEL  (CacheNames.PRIORITY_LEVEL,   Ttl.REFERENCE, Ttl.REFERENCE, Ttl.STABLE),

    CATEGORY        (CacheNames.HABIT_CATEGORY,         Ttl.STABLE,    Ttl.STABLE,    Ttl.LISTS),

    USER            (CacheNames.USER,             Ttl.STANDARD,  Ttl.LISTS,     Ttl.PAGES),
    HABIT           (CacheNames.HABIT,            Ttl.STANDARD,  Ttl.LISTS,     Ttl.PAGES),
    HABIT_TASK      (CacheNames.HABIT_TASK,       Ttl.STANDARD,  Ttl.LISTS,     Ttl.PAGES),
    CALENDAR        (CacheNames.HABIT_CALENDAR,         Ttl.STANDARD,  Ttl.LISTS,     Ttl.PAGES),

    PERCENTAGE_DAY  (CacheNames.PERCENTAGE_DAY,   Ttl.COMPUTED,  Ttl.COMPUTED,  Ttl.COMPUTED),
    PERCENTAGE_WEEK (CacheNames.PERCENTAGE_WEEK,  Ttl.COMPUTED,  Ttl.COMPUTED,  Ttl.COMPUTED),
    ;

    private final String baseName;
    private final Duration itemTtl;
    private final Duration listTtl;
    private final Duration pageTtl;

    CacheTtl(String baseName, Duration itemTtl, Duration listTtl, Duration pageTtl) {
        this.baseName = baseName;
        this.itemTtl = itemTtl;
        this.listTtl = listTtl;
        this.pageTtl = pageTtl;
    }

    private static final class Ttl {
        static final Duration REFERENCE = Duration.ofHours(12);
        static final Duration STABLE    = Duration.ofHours(1);
        static final Duration STANDARD  = Duration.ofMinutes(30);
        static final Duration LISTS     = Duration.ofMinutes(15);
        static final Duration PAGES     = Duration.ofMinutes(10);
        static final Duration COMPUTED  = Duration.ofMinutes(10);
    }
}
