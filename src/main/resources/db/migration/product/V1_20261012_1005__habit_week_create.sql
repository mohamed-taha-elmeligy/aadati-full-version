CREATE TABLE habit_week (
    week_id     UUID        NOT NULL DEFAULT gen_random_uuid(),
    week_number INTEGER     NOT NULL,
    start_week  DATE        NOT NULL,
    end_week    DATE        NOT NULL,
    year_number INTEGER     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_habit_week            PRIMARY KEY (week_id),
    CONSTRAINT uk_habit_week_start      UNIQUE (start_week),
    CONSTRAINT uk_habit_week_end        UNIQUE (end_week),
    CONSTRAINT uk_habit_week_year_week  UNIQUE (year_number, week_number),
    CONSTRAINT ck_habit_week_number     CHECK (week_number BETWEEN 1 AND 53),
    CONSTRAINT ck_habit_week_range      CHECK (end_week > start_week)
);
