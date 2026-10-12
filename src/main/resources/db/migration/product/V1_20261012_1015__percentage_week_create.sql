CREATE TABLE percentage_week (
    percentage_week_id UUID         NOT NULL DEFAULT gen_random_uuid(),
    rate               NUMERIC(5,2) NOT NULL DEFAULT 0,
    grade              VARCHAR(100) NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    user_id            UUID         NOT NULL,
    habit_week_id      UUID         NOT NULL,
    CONSTRAINT pk_percentage_week            PRIMARY KEY (percentage_week_id),
    CONSTRAINT uk_percentage_week_user_week  UNIQUE (user_id, habit_week_id),
    CONSTRAINT fk_percentage_week_user       FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_percentage_week_week       FOREIGN KEY (habit_week_id) REFERENCES habit_week (week_id) ON DELETE RESTRICT,
    CONSTRAINT ck_percentage_week_rate       CHECK (rate BETWEEN 0 AND 100)
);

-- user_id is covered by uk_percentage_week_user_week
CREATE INDEX inx_percentage_week_week_id ON percentage_week (habit_week_id);
