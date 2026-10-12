CREATE TABLE habit_completion (
    habit_completion_id UUID        NOT NULL DEFAULT gen_random_uuid(),
    complete            BOOLEAN     NOT NULL,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    habit_calendar_id   UUID        NOT NULL,
    habit_id            UUID        NOT NULL,
    CONSTRAINT pk_habit_completion          PRIMARY KEY (habit_completion_id),
    CONSTRAINT uk_habit_completion_habit_day UNIQUE (habit_id, habit_calendar_id),
    CONSTRAINT fk_habit_completion_calendar FOREIGN KEY (habit_calendar_id) REFERENCES habit_calendar (habit_calendar_id) ON DELETE RESTRICT,
    CONSTRAINT fk_habit_completion_habit    FOREIGN KEY (habit_id) REFERENCES habit (habit_id) ON DELETE CASCADE
);

-- habit_id is covered by uk_habit_completion_habit_day
CREATE INDEX inx_habit_completion_calendar_id ON habit_completion (habit_calendar_id);
