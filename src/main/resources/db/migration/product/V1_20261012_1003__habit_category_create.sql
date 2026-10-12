CREATE TABLE habit_category (
    habit_category_id UUID         NOT NULL DEFAULT gen_random_uuid(),
    name              VARCHAR(50)  NOT NULL,
    description       VARCHAR(800),
    color             VARCHAR(7)   NOT NULL,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_habit_category       PRIMARY KEY (habit_category_id),
    CONSTRAINT uk_habit_category_name  UNIQUE (name),
    CONSTRAINT ck_habit_category_color CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);
