CREATE TABLE percentage_day (
    percentage_day_id UUID         NOT NULL DEFAULT RANDOM_UUID(),
    rate              NUMERIC(5,2) NOT NULL DEFAULT 0,
    updated_at        TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id           UUID         NOT NULL,
    habit_calendar_id UUID         NOT NULL,
    CONSTRAINT pk_percentage_day               PRIMARY KEY (percentage_day_id),
    CONSTRAINT uk_percentage_day_user_calendar UNIQUE (user_id, habit_calendar_id),
    CONSTRAINT fk_percentage_day_user          FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_percentage_day_calendar      FOREIGN KEY (habit_calendar_id) REFERENCES habit_calendar (habit_calendar_id) ON DELETE RESTRICT,
    CONSTRAINT ck_percentage_day_rate          CHECK (rate BETWEEN 0 AND 100)
);

-- user_id is covered by uk_percentage_day_user_calendar
CREATE INDEX inx_percentage_day_calendar_id ON percentage_day (habit_calendar_id);
