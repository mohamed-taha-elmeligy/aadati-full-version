CREATE TABLE habit_task (
    habit_task_id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    title                  VARCHAR(50)  NOT NULL,
    description            VARCHAR(800),
    recurrence_type        VARCHAR(20)  NOT NULL,
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    start_date             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    user_id                UUID         NOT NULL,
    task_priority_level_id UUID         NOT NULL,
    habit_category_id      UUID         NOT NULL,
    CONSTRAINT pk_habit_task            PRIMARY KEY (habit_task_id),
    CONSTRAINT uk_habit_task_user_title UNIQUE (user_id, title),
    CONSTRAINT fk_habit_task_user       FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_habit_task_priority   FOREIGN KEY (task_priority_level_id) REFERENCES task_priority_level (task_priority_level_id) ON DELETE RESTRICT,
    CONSTRAINT fk_habit_task_category   FOREIGN KEY (habit_category_id) REFERENCES habit_category (habit_category_id) ON DELETE RESTRICT,
    CONSTRAINT ck_habit_task_recurrence CHECK (recurrence_type IN
        ('NONE','ONCE','DAILY','WEEKLY','MONTHLY','YEARLY'))
);

CREATE INDEX inx_habit_task_user_start_date ON habit_task (user_id, start_date);
CREATE INDEX inx_habit_task_priority_id     ON habit_task (task_priority_level_id);
CREATE INDEX inx_habit_task_category_id     ON habit_task (habit_category_id);
