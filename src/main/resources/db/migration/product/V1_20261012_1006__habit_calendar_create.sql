CREATE TABLE habit_calendar (
    habit_calendar_id UUID        NOT NULL DEFAULT gen_random_uuid(),
    day_of_week       VARCHAR(10) NOT NULL,
    date              DATE        NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    week_id           UUID        NOT NULL,
    CONSTRAINT pk_habit_calendar          PRIMARY KEY (habit_calendar_id),
    CONSTRAINT uk_habit_calendar_date     UNIQUE (date),
    CONSTRAINT uk_habit_calendar_week_day UNIQUE (week_id, day_of_week),
    CONSTRAINT fk_habit_calendar_week     FOREIGN KEY (week_id) REFERENCES habit_week (week_id) ON DELETE RESTRICT,
    CONSTRAINT ck_habit_calendar_day      CHECK (day_of_week IN
        ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))
);
-- No separate index on week_id: uk_habit_calendar_week_day already starts with it.
