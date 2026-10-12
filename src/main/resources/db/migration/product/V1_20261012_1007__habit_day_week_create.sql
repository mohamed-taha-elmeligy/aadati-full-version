CREATE TABLE habit_day_week (
    day_week_id BIGINT      NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    CONSTRAINT pk_habit_day_week     PRIMARY KEY (day_week_id),
    CONSTRAINT uk_habit_day_week_day UNIQUE (day_of_week),
    CONSTRAINT ck_habit_day_week_id  CHECK (day_week_id BETWEEN 1 AND 7),
    CONSTRAINT ck_habit_day_week_day CHECK (day_of_week IN
        ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))
);
