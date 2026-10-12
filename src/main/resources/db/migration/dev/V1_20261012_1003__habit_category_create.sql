CREATE TABLE habit_category (
    habit_category_id UUID         NOT NULL DEFAULT RANDOM_UUID(),
    name              VARCHAR(50)  NOT NULL,
    description       VARCHAR(800),
    color             VARCHAR(7)   NOT NULL,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at        TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_habit_category       PRIMARY KEY (habit_category_id),
    CONSTRAINT uk_habit_category_name  UNIQUE (name),
    CONSTRAINT ck_habit_category_color CHECK (REGEXP_LIKE(color, '^#[0-9A-Fa-f]{6}$'))
);
