CREATE TABLE task_completion (
    task_completion_id UUID        NOT NULL DEFAULT RANDOM_UUID(),
    complete           BOOLEAN     NOT NULL,
    completed_at       TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    habit_calendar_id  UUID        NOT NULL,
    habit_task_id      UUID        NOT NULL,
    CONSTRAINT pk_task_completion           PRIMARY KEY (task_completion_id),
    CONSTRAINT uk_task_completion_task_day  UNIQUE (habit_task_id, habit_calendar_id),
    CONSTRAINT fk_task_completion_calendar  FOREIGN KEY (habit_calendar_id) REFERENCES habit_calendar (habit_calendar_id) ON DELETE RESTRICT,
    CONSTRAINT fk_task_completion_task      FOREIGN KEY (habit_task_id) REFERENCES habit_task (habit_task_id) ON DELETE CASCADE
);

-- habit_task_id is covered by uk_task_completion_task_day
CREATE INDEX inx_task_completion_calendar_id ON task_completion (habit_calendar_id);
