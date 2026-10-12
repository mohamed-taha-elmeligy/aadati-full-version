CREATE TABLE habit (
    habit_id          UUID             NOT NULL DEFAULT gen_random_uuid(),
    title             VARCHAR(50)      NOT NULL,
    point             DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    type              BOOLEAN          NOT NULL DEFAULT TRUE,
    description       VARCHAR(1000),
    is_active         BOOLEAN          NOT NULL DEFAULT TRUE,
    updated_at        TIMESTAMPTZ      NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ      NOT NULL DEFAULT now(),
    user_id           UUID             NOT NULL,
    habit_category_id UUID             NOT NULL,
    CONSTRAINT pk_habit               PRIMARY KEY (habit_id),
    CONSTRAINT uk_habit_user_title    UNIQUE (user_id, title),
    CONSTRAINT fk_habit_user          FOREIGN KEY (user_id)           REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_habit_category      FOREIGN KEY (habit_category_id) REFERENCES habit_category (habit_category_id) ON DELETE RESTRICT,
    CONSTRAINT ck_habit_point         CHECK (point > 0)
);

-- user_id is covered by uk_habit_user_title
CREATE INDEX inx_habit_category_id ON habit (habit_category_id);
